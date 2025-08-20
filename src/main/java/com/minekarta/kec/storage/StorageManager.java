package com.minekarta.kec.storage;

import com.minekarta.kec.KartaEmeraldCurrencyPlugin;
import com.minekarta.kec.storage.provider.FileStorageProvider;
import com.minekarta.kec.storage.provider.MySqlStorageProvider;
import com.minekarta.kec.storage.provider.StorageProvider;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.ConfigurationSection;

import java.io.File;
import java.util.concurrent.TimeUnit;

public class StorageManager {

    private final KartaEmeraldCurrencyPlugin plugin;
    private StorageProvider activeProvider;
    private HikariDataSource dataSource;

    public enum StorageType {
        MYSQL,
        YAML // Changed from FILE to be more specific, as per implementation
    }

    public StorageManager(KartaEmeraldCurrencyPlugin plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        // Before initializing, check for deprecated H2 files
        checkForH2Files();

        String storageTypeStr = plugin.getConfig().getString("storage.type", "YAML").toUpperCase();
        StorageType storageType;
        try {
            storageType = StorageType.valueOf(storageTypeStr);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid storage type '" + storageTypeStr + "' in config.yml. Defaulting to YAML.");
            storageType = StorageType.YAML;
        }

        plugin.getLogger().info("Initializing storage provider: " + storageType);

        if (this.activeProvider != null) {
            shutdown(); // Shutdown existing provider before creating a new one
        }

        switch (storageType) {
            case MYSQL:
                this.dataSource = createHikariDataSource();
                this.activeProvider = new MySqlStorageProvider(dataSource, plugin.getLogger());
                break;
            case YAML:
                this.activeProvider = new FileStorageProvider(plugin);
                break;
            default:
                throw new IllegalStateException("Unsupported storage type: " + storageType);
        }

        this.activeProvider.initialize();
        plugin.getLogger().info("Storage provider initialized successfully.");
    }

    public void shutdown() {
        if (activeProvider != null) {
            activeProvider.shutdown();
            plugin.getLogger().info("Storage provider shut down.");
        }
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLogger().info("Database connection pool closed.");
        }
    }

    public void reload() {
        plugin.getLogger().info("Reloading storage provider...");
        shutdown();
        initialize();
    }

    public StorageProvider getProvider() {
        return activeProvider;
    }

    private HikariDataSource createHikariDataSource() {
        ConfigurationSection mysqlConfig = plugin.getConfig().getConfigurationSection("storage.mysql");
        if (mysqlConfig == null) {
            throw new IllegalStateException("MySQL storage is selected, but 'storage.mysql' configuration is missing in config.yml.");
        }

        HikariConfig config = new HikariConfig();
        config.setPoolName("KartaEmerald-MySQL-Pool");
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setJdbcUrl(String.format("jdbc:mysql://%s:%s/%s",
                mysqlConfig.getString("host", "localhost"),
                mysqlConfig.getString("port", "3306"),
                mysqlConfig.getString("database")));
        config.setUsername(mysqlConfig.getString("username"));
        config.setPassword(mysqlConfig.getString("password"));

        config.setMaximumPoolSize(mysqlConfig.getInt("pool.maximum-pool-size", 10));
        config.setMinimumIdle(mysqlConfig.getInt("pool.minimum-idle", 2));
        config.setConnectionTimeout(mysqlConfig.getLong("pool.connection-timeout-ms", 10000));

        // MySQL specific properties from the old DatabaseManager
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
        if (mysqlConfig.getBoolean("use-ssl", false)) {
            config.addDataSourceProperty("useSSL", "true");
        }

        return new HikariDataSource(config);
    }

    private void checkForH2Files() {
        // Look for kec-data.db.mv.db or other h2 files in the plugin's data folder
        File dataFolder = plugin.getDataFolder();
        File h2File = new File(dataFolder, "kec-data.db.mv.db");
        if (h2File.exists()) {
            plugin.getLogger().warning("[KartaEmeraldCurrency] H2 storage is no longer supported. Please migrate to MySQL or YAML storage.");
        }
    }
}
