package com.minekarta.kec.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Path;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * Manages the database connection pool using HikariCP.
 * It can be configured to connect to either SQLite or MySQL based on the provided configuration.
 */
public class DatabaseManager {

    private final HikariDataSource dataSource;

    /**
     * Enum for the supported storage types.
     */
    public enum StorageType {
        /**
         * Use H2 for data storage.
         */
        H2,
        /**
         * Use MySQL for data storage.
         */
        MYSQL
    }

    /**
     * Constructs a new DatabaseManager.
     *
     * @param type The type of storage to use.
     * @param dbProps The database properties.
     * @param dataFolderPath The path to the plugin's data folder.
     */
    public DatabaseManager(@NotNull StorageType type, @NotNull Properties dbProps, @NotNull Path dataFolderPath) {
        HikariConfig config = new HikariConfig();

        switch (type) {
            case H2:
                String dbFileName = dbProps.getProperty("h2.file", "kec-data.db");
                File dbFile = dataFolderPath.resolve(dbFileName).toFile();
                if (!dbFile.getParentFile().exists()) {
                    dbFile.getParentFile().mkdirs();
                }
                config.setPoolName("KartaEmerald-H2-Pool");
                config.setDriverClassName("com.minekarta.kec.libs.h2.Driver");
                // AUTO_SERVER=TRUE allows multiple connections within the same JVM, preventing file lock issues.
                config.setJdbcUrl("jdbc:h2:" + dbFile.getAbsolutePath() + ";AUTO_SERVER=TRUE");

                // Sensible pool settings for H2
                config.setMaximumPoolSize(2);
                config.setConnectionTimeout(TimeUnit.SECONDS.toMillis(10));
                break;

            case MYSQL:
                config.setDriverClassName("com.mysql.cj.jdbc.Driver");
                config.setJdbcUrl(String.format("jdbc:mysql://%s:%s/%s",
                        dbProps.getProperty("mysql.host"),
                        dbProps.getProperty("mysql.port"),
                        dbProps.getProperty("mysql.database")));
                config.setUsername(dbProps.getProperty("mysql.username"));
                config.setPassword(dbProps.getProperty("mysql.password"));

                // Pool settings
                config.setMaximumPoolSize(Integer.parseInt(dbProps.getProperty("mysql.pool.maximumPoolSize", "10")));
                config.setMinimumIdle(Integer.parseInt(dbProps.getProperty("mysql.pool.minimumIdle", "2")));
                config.setConnectionTimeout(Long.parseLong(dbProps.getProperty("mysql.pool.connectionTimeoutMs", "10000")));

                // MySQL specific properties
                config.addDataSourceProperty("cachePrepStmts", "true");
                config.addDataSourceProperty("prepStmtCacheSize", "250");
                config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
                config.addDataSourceProperty("useServerPrepStmts", "true");
                config.addDataSourceProperty("useLocalSessionState", "true");
                config.addDataSourceProperty("rewriteBatchedStatements", "true");
                config.addDataSourceProperty("cacheResultSetMetadata", "true");
                config.addDataSourceProperty("cacheServerConfiguration", "true");
                config.addDataSourceProperty("elideSetAutoCommits", "true");
                config.addDataSourceProperty("maintainTimeStats", "false");
                if (Boolean.parseBoolean(dbProps.getProperty("mysql.use-ssl", "false"))) {
                    config.addDataSourceProperty("useSSL", "true");
                }
                break;
            default:
                throw new IllegalArgumentException("Unsupported storage type: " + type);
        }

        this.dataSource = new HikariDataSource(config);
    }

    /**
     * Gets the configured HikariCP data source.
     *
     * @return The HikariDataSource instance.
     */
    public HikariDataSource getDataSource() {
        return dataSource;
    }

    /**
     * Closes the connection pool and releases all resources.
     */
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
