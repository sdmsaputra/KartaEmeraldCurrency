package com.minekarta.kec.storage;

import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * A MySQL implementation of the {@link Storage} interface.
 */
public class MySqlStorage implements Storage {

    private final DatabaseManager dbManager;
    private final Executor asyncExecutor;

    private static final String CREATE_ACCOUNTS_TABLE = """
            CREATE TABLE IF NOT EXISTS kec_accounts (
                uuid CHAR(36) NOT NULL,
                balance BIGINT NOT NULL DEFAULT 0,
                updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                PRIMARY KEY (uuid)
            ) ENGINE=InnoDB;""";

    private static final String GET_BALANCE = "SELECT balance FROM kec_accounts WHERE uuid = ?;";
    private static final String UPSERT_BALANCE = """
            INSERT INTO kec_accounts (uuid, balance) VALUES (?, ?)
            ON DUPLICATE KEY UPDATE balance = VALUES(balance);
            """;
    private static final String ACCOUNT_EXISTS = "SELECT 1 FROM kec_accounts WHERE uuid = ?;";
    private static final String ADD_BALANCE = """
            INSERT INTO kec_accounts (uuid, balance) VALUES (?, ?)
            ON DUPLICATE KEY UPDATE balance = balance + VALUES(balance);
            """;
    private static final String REMOVE_BALANCE = """
            UPDATE kec_accounts SET balance = balance - ? WHERE uuid = ?;
            """; // A simple update is better here for MySQL

    /**
     * Constructs a new MySqlStorage.
     *
     * @param dbManager The database manager.
     * @param asyncExecutor The executor for async tasks.
     */
    public MySqlStorage(DatabaseManager dbManager, Executor asyncExecutor) {
        this.dbManager = dbManager;
        this.asyncExecutor = asyncExecutor;
    }

    private <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, asyncExecutor);
    }

    private CompletableFuture<Void> runAsync(Runnable runnable) {
        return CompletableFuture.runAsync(runnable, asyncExecutor);
    }

    @Override
    public void initialize() {
        // This must run synchronously on startup to ensure tables are ready.
        try (Connection conn = dbManager.getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(CREATE_ACCOUNTS_TABLE)) {
            ps.execute();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize MySQL database", e);
        }
    }

    @Override
    public void close() {
        dbManager.close();
    }

    @Override
    public CompletableFuture<Long> getBalance(@NotNull UUID uuid) {
        return supplyAsync(() -> {
            try (Connection conn = dbManager.getDataSource().getConnection();
                 PreparedStatement ps = conn.prepareStatement(GET_BALANCE)) {
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    return rs.getLong("balance");
                }
                return 0L;
            } catch (SQLException e) {
                throw new RuntimeException("Failed to get balance for " + uuid, e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> setBalance(@NotNull UUID uuid, long balance) {
        return runAsync(() -> {
            try (Connection conn = dbManager.getDataSource().getConnection();
                 PreparedStatement ps = conn.prepareStatement(UPSERT_BALANCE)) {
                ps.setString(1, uuid.toString());
                ps.setLong(2, balance);
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to set balance for " + uuid, e);
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> hasAccount(@NotNull UUID uuid) {
        return supplyAsync(() -> {
            try (Connection conn = dbManager.getDataSource().getConnection();
                 PreparedStatement ps = conn.prepareStatement(ACCOUNT_EXISTS)) {
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();
                return rs.next();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to check account for " + uuid, e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> createAccount(@NotNull UUID uuid, long startingBalance) {
        return runAsync(() -> {
             try (Connection conn = dbManager.getDataSource().getConnection();
                  PreparedStatement ps = conn.prepareStatement("INSERT IGNORE INTO kec_accounts (uuid, balance) VALUES (?, ?)")) {
                 ps.setString(1, uuid.toString());
                 ps.setLong(2, startingBalance);
                 ps.executeUpdate();
             } catch (SQLException e) {
                 throw new RuntimeException("Failed to create account for " + uuid, e);
             }
        });
    }

    @Override
    public CompletableFuture<Long> addBalance(@NotNull UUID uuid, long amount) {
         return supplyAsync(() -> {
            try (Connection conn = dbManager.getDataSource().getConnection();
                 PreparedStatement ps = conn.prepareStatement(ADD_BALANCE)) {
                ps.setString(1, uuid.toString());
                ps.setLong(2, amount);
                ps.executeUpdate();
                return getBalance(uuid).join();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to add balance for " + uuid, e);
            }
        });
    }

    @Override
    public CompletableFuture<Long> removeBalance(@NotNull UUID uuid, long amount) {
        return supplyAsync(() -> {
            try (Connection conn = dbManager.getDataSource().getConnection();
                 PreparedStatement ps = conn.prepareStatement(REMOVE_BALANCE)) {
                ps.setLong(1, amount);
                ps.setString(2, uuid.toString());
                ps.executeUpdate();
                return getBalance(uuid).join();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to remove balance for " + uuid, e);
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> performTransfer(@NotNull UUID from, @NotNull UUID to, long amount, long fee) {
        return supplyAsync(() -> {
            long totalDeduction = amount + fee;
            try (Connection conn = dbManager.getDataSource().getConnection()) {
                conn.setAutoCommit(false);

                // Deduct from sender, but only if they have enough funds
                int updatedRows;
                try (PreparedStatement deductPs = conn.prepareStatement("UPDATE kec_accounts SET balance = balance - ? WHERE uuid = ? AND balance >= ?")) {
                    deductPs.setLong(1, totalDeduction);
                    deductPs.setString(2, from.toString());
                    deductPs.setLong(3, totalDeduction);
                    updatedRows = deductPs.executeUpdate();
                }

                if (updatedRows == 0) {
                    conn.rollback();
                    return false; // Insufficient funds or user does not exist
                }

                // Add to receiver
                try (PreparedStatement addPs = conn.prepareStatement(ADD_BALANCE)) {
                    addPs.setString(1, to.toString());
                    addPs.setLong(2, amount);
                    addPs.executeUpdate();
                }

                conn.commit();
                return true;

            } catch (SQLException e) {
                 try (Connection conn = dbManager.getDataSource().getConnection()) {
                    if (conn != null) conn.rollback();
                } catch (SQLException ex) {
                    e.addSuppressed(ex);
                }
                throw new RuntimeException("Failed to perform transfer", e);
            }
        });
    }
}
