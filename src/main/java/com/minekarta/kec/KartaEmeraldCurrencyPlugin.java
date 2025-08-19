package com.minekarta.kec;

import com.minekarta.kec.api.KartaEmeraldService;
import com.minekarta.kec.command.EmeraldAdminCommand;
import com.minekarta.kec.command.EmeraldCommand;
import com.minekarta.kec.gui.ChatInputManager;
import com.minekarta.kec.service.KartaEmeraldServiceImpl;
import com.minekarta.kec.storage.DatabaseManager;
import com.minekarta.kec.util.MessageUtil;
import com.minekarta.kec.storage.MySqlStorage;
import com.minekarta.kec.storage.H2Storage;
import com.minekarta.kec.storage.Storage;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.Executor;

/**
 * The main plugin class for KartaEmeraldCurrency.
 * Handles loading, enabling, and disabling of the plugin's features.
 */
public class KartaEmeraldCurrencyPlugin extends JavaPlugin {

    /**
     * Constructs a new KartaEmeraldCurrencyPlugin.
     */
    public KartaEmeraldCurrencyPlugin() {
        // Default constructor
    }

    private static KartaEmeraldCurrencyPlugin instance;

    private Storage storage;
    private KartaEmeraldService service;
    private DatabaseManager databaseManager;
    private ChatInputManager chatInputManager;

    private FileConfiguration messagesConfig;
    private FileConfiguration guiConfig;
    private FileConfiguration databaseConfig;

    /**
     * Called when the plugin is enabled.
     * Initializes configurations, database, services, and hooks.
     */
    @Override
    public void onEnable() {
        instance = this;

        // Load all configurations
        loadConfigs();
        MessageUtil.load(this);

        // Setup Database
        if (!setupStorage()) {
            getLogger().severe("Failed to setup database. Disabling plugin.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        // Setup Service
        this.service = new KartaEmeraldServiceImpl(this, this.storage);
        Bukkit.getServicesManager().register(KartaEmeraldService.class, this.service, this, ServicePriority.Normal);

        // Setup Chat Input Manager
        this.chatInputManager = new ChatInputManager(this);

        // Register Hooks, Commands, Listeners
        setupHooks();
        setupCommands();
        setupListeners();

        getLogger().info("KartaEmeraldCurrency has been enabled successfully.");
    }

    /**
     * Called when the plugin is disabled.
     * Closes the database connection.
     */
    @Override
    public void onDisable() {
        if (storage != null) {
            storage.close();
        }
        getLogger().info("KartaEmeraldCurrency has been disabled.");
    }

    private void loadConfigs() {
        saveDefaultConfig();

        this.messagesConfig = loadCustomConfig("messages.yml");
        this.guiConfig = loadCustomConfig("gui.yml");
        this.databaseConfig = loadCustomConfig("database.yml");
    }

    private FileConfiguration loadCustomConfig(String fileName) {
        File file = new File(getDataFolder(), fileName);
        if (!file.exists()) {
            saveResource(fileName, false);
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    private boolean setupStorage() {
        String storageTypeStr = databaseConfig.getString("storage.type", "H2").toUpperCase();
        DatabaseManager.StorageType storageType;
        try {
            storageType = DatabaseManager.StorageType.valueOf(storageTypeStr);
        } catch (IllegalArgumentException e) {
            getLogger().severe("Invalid storage type '" + storageTypeStr + "' in database.yml. Defaulting to H2.");
            storageType = DatabaseManager.StorageType.H2;
        }

        Properties dbProps = new Properties();
        if (storageType == DatabaseManager.StorageType.MYSQL) {
            databaseConfig.getConfigurationSection("mysql").getValues(true).forEach((key, value) -> dbProps.setProperty("mysql." + key, value.toString()));
        } else { // H2
            databaseConfig.getConfigurationSection("h2").getValues(true).forEach((key, value) -> dbProps.setProperty("h2." + key, value.toString()));
        }

        try {
            this.databaseManager = new DatabaseManager(storageType, dbProps, getDataFolder().toPath());
            Executor asyncExecutor = (runnable) -> Bukkit.getScheduler().runTaskAsynchronously(this, runnable);

            if (storageType == DatabaseManager.StorageType.MYSQL) {
                this.storage = new MySqlStorage(databaseManager, asyncExecutor);
            } else { // H2
                this.storage = new H2Storage(databaseManager, asyncExecutor);
            }

            this.storage.initialize(); // Create tables on startup
            getLogger().info("Successfully connected to " + storageType + " database.");
            return true;
        } catch (Exception e) {
            getLogger().severe("Could not connect to the database.");
            e.printStackTrace();
            return false;
        }
    }

    private void setupHooks() {
        // Hook into Vault
        if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
            com.minekarta.kec.vault.VaultEconomyAdapter vaultAdapter = new com.minekarta.kec.vault.VaultEconomyAdapter(this.service);
            Bukkit.getServicesManager().register(net.milkbowl.vault.economy.Economy.class, vaultAdapter, this, ServicePriority.High);
            getLogger().info("Successfully hooked into Vault.");

            // Fire event for other plugins, required by Paper to be async.
            Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                Bukkit.getPluginManager().callEvent(new com.minekarta.kec.api.event.EconomyProviderRegisteredEvent(vaultAdapter.getName()));
            });
        } else {
            getLogger().warning("Vault not found. Economy features will be limited.");
        }

        // Hook into PlaceholderAPI
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new com.minekarta.kec.placeholder.KecPlaceholderExpansion(this).register();
            getLogger().info("Successfully hooked into PlaceholderAPI.");
        } else {
            getLogger().info("PlaceholderAPI not found. No placeholders will be available.");
        }
    }

    private void setupCommands() {
        EmeraldCommand emeraldCommand = new EmeraldCommand(this);
        getCommand("emerald").setExecutor(emeraldCommand);
        getCommand("emerald").setTabCompleter(emeraldCommand);

        EmeraldAdminCommand adminCommand = new EmeraldAdminCommand(this);
        getCommand("emeraldadmin").setExecutor(adminCommand);
        getCommand("emeraldadmin").setTabCompleter(adminCommand);
    }

    private void setupListeners() {
        Bukkit.getPluginManager().registerEvents(new com.minekarta.kec.gui.GuiListener(), this);
        Bukkit.getPluginManager().registerEvents(this.chatInputManager, this);
        // TODO: Register other listeners like PlayerJoinListener
    }

    /**
     * Gets the singleton instance of the plugin.
     * @return The plugin instance.
     */
    public static KartaEmeraldCurrencyPlugin getInstance() {
        return instance;
    }

    /**
     * Gets the active KartaEmeraldService API instance.
     * @return The economy service.
     */
    public KartaEmeraldService getService() {
        return service;
    }

    /**
     * Gets the main plugin configuration (config.yml).
     * @return The main configuration.
     */
    public FileConfiguration getPluginConfig() {
        return getConfig();
    }

    /**
     * Gets the messages configuration (messages.yml).
     * @return The messages configuration.
     */
    public FileConfiguration getMessagesConfig() {
        return messagesConfig;
    }

    /**
     * Gets the GUI configuration (gui.yml).
     * @return The GUI configuration.
     */
    public FileConfiguration getGuiConfig() {
        return guiConfig;
    }

    /**
     * Gets the chat input manager.
     * @return The chat input manager.
     */
    public ChatInputManager getChatInputManager() {
        return chatInputManager;
    }
}
