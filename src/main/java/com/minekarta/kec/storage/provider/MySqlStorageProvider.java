package com.minekarta.kec.storage.provider;

import com.zaxxer.hikari.HikariDataSource;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MySqlStorageProvider implements StorageProvider {

    private final HikariDataSource dataSource;
    private final Logger logger;

    private static final String CREATE_TABLE = """
            CREATE TABLE IF NOT EXISTS kec_accounts (
                uuid CHAR(36) NOT NULL,
                balance BIGINT NOT NULL DEFAULT 0,
                PRIMARY KEY (uuid)
            ) ENGINE=InnoDB;""";
    private static final String GET_PLAYER = "SELECT balance FROM kec_accounts WHERE uuid = ?;";
    private static final String SAVE_PLAYER = "INSERT INTO kec_accounts (uuid, balance) VALUES (?, ?) ON DUPLICATE KEY UPDATE balance = VALUES(balance);";
    private static final String DELETE_PLAYER = "DELETE FROM kec_accounts WHERE uuid = ?;";
    private static final String GET_ALL_PLAYERS = "SELECT uuid, balance FROM kec_accounts;";

    public MySqlStorageProvider(HikariDataSource dataSource, Logger logger) {
        this.dataSource = dataSource;
        this.logger = logger;
    }

    @Override
    public void initialize() {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(CREATE_TABLE)) {
            ps.execute();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize MySQL database tables", e);
        }
    }

    @Override
    public void shutdown() {
        // The connection pool is managed by the StorageManager, so nothing to do here.
    }

    @Override
    public Optional<PlayerData> getPlayerData(@NotNull UUID uuid) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(GET_PLAYER)) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return Optional.of(new PlayerData(rs.getLong("balance")));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to get player data for " + uuid, e);
        }
        return Optional.empty();
    }

    @Override
    public void savePlayerData(@NotNull UUID uuid, @NotNull PlayerData data) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(SAVE_PLAYER)) {
            ps.setString(1, uuid.toString());
            ps.setLong(2, data.getBalance());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to save player data for " + uuid, e);
        }
    }

    @Override
    public void deletePlayerData(@NotNull UUID uuid) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(DELETE_PLAYER)) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to delete player data for " + uuid, e);
        }
    }

    @Override
    public Map<UUID, PlayerData> getAllPlayerData() {
        Map<UUID, PlayerData> allData = new HashMap<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(GET_ALL_PLAYERS)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                long balance = rs.getLong("balance");
                allData.put(uuid, new PlayerData(balance));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to get all player data", e);
        }
        return allData;
    }
}
