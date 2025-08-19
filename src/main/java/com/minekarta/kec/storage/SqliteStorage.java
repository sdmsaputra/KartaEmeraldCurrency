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
 * A SQLite implementation of the {@link Storage} interface.
 */
public class SqliteStorage implements Storage {

    private final DatabaseManager dbManager;
    private final Executor asyncExecutor;

    private static final String CREATE_ACCOUNTS_TABLE = """
            CREATE TABLE IF NOT EXISTS kec_accounts (
                uuid CHAR(36) PRIMARY KEY,
                balance BIGINT NOT NULL DEFAULT 0,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            );""";

    private static final String GET_BALANCE = "SELECT balance FROM kec_accounts WHERE uuid = ?;";
    private static final String UPSERT_BALANCE = """
            INSERT INTO kec_accounts (uuid, balance) VALUES (?, ?)
            ON CONFLICT(uuid) DO UPDATE SET balance = excluded.balance, updated_at = CURRENT_TIMESTAMP;
            """;
    private static final String ACCOUNT_EXISTS = "SELECT 1 FROM kec_accounts WHERE uuid = ?;";
    private static final String ADD_BALANCE = """
            INSERT INTO kec_accounts (uuid, balance) VALUES (?, ?)
            ON CONFLICT(uuid) DO UPDATE SET balance = balance + excluded.balance, updated_at = CURRENT_TIMESTAMP;
            """;
    private static final String REMOVE_BALANCE = """
            INSERT INTO kec_accounts (uuid, balance) VALUES (?, 0)
            ON CONFLICT(uuid) DO UPDATE SET balance = balance - ?, updated_at = CURRENT_TIMESTAMP;
            """;


    /**
     * Constructs a new SqliteStorage.
     *
     * @param dbManager The database manager.
     * @param asyncExecutor The executor for async tasks.
     */
    public SqliteStorage(DatabaseManager dbManager, Executor asyncExecutor) {
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
    public CompletableFuture<Void> initialize() {
        return runAsync(() -> {
            try (Connection conn = dbManager.getDataSource().getConnection();
                 PreparedStatement ps = conn.prepareStatement(CREATE_ACCOUNTS_TABLE)) {
                ps.execute();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to initialize SQLite database", e);
            }
        });
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
        return setBalance(uuid, startingBalance); // UPSERT handles creation
    }

    @Override
    public CompletableFuture<Long> addBalance(@NotNull UUID uuid, long amount) {
        return supplyAsync(() -> {
            try (Connection conn = dbManager.getDataSource().getConnection();
                 PreparedStatement ps = conn.prepareStatement(ADD_BALANCE)) {
                ps.setString(1, uuid.toString());
                ps.setLong(2, amount);
                ps.executeUpdate();
                // We need to return the new balance. A second query is needed.
                return getBalance(uuid).join(); // .join() is safe inside CompletableFuture
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
                ps.setString(1, uuid.toString());
                ps.setLong(2, amount);
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

                // Check sender's balance
                try (PreparedStatement checkPs = conn.prepareStatement("SELECT balance FROM kec_accounts WHERE uuid = ?")) {
                    checkPs.setString(1, from.toString());
                    ResultSet rs = checkPs.executeQuery();
                    if (!rs.next() || rs.getLong("balance") < totalDeduction) {
                        conn.rollback();
                        return false;
                    }
                }

                // Deduct from sender
                try (PreparedStatement deductPs = conn.prepareStatement("UPDATE kec_accounts SET balance = balance - ? WHERE uuid = ?")) {
                    deductPs.setLong(1, totalDeduction);
                    deductPs.setString(2, from.toString());
                    deductPs.executeUpdate();
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
                // In case of error, the whole transaction should be rolled back.
                // try-with-resources on Connection should handle this if autocommit is false, but explicit rollback is safer.
                throw new RuntimeException("Failed to perform transfer", e);
            }
        });
    }
}
