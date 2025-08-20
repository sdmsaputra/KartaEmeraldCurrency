package com.minekarta.kec;

import com.minekarta.kec.api.KartaEmeraldService;
import com.minekarta.kec.command.EmeraldAdminCommand;
import com.minekarta.kec.command.EmeraldCommand;
import com.minekarta.kec.gui.ChatInputManager;
import com.minekarta.kec.placeholder.KecPlaceholderExpansion;
import com.minekarta.kec.service.KartaEmeraldServiceImpl;
import com.minekarta.kec.storage.DefaultEconomyDataHandler;
import com.minekarta.kec.storage.EconomyDataHandler;
import com.minekarta.kec.storage.StorageManager;
import com.minekarta.kec.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.concurrent.Executor;

/**
 * The main plugin class for KartaEmeraldCurrency.
 * Handles loading, enabling, and disabling of the plugin's features.
 */
public class KartaEmeraldCurrencyPlugin extends JavaPlugin {

    private static KartaEmeraldCurrencyPlugin instance;

    private StorageManager storageManager;
    private EconomyDataHandler economyDataHandler;
    private KartaEmeraldService service;
    private ChatInputManager chatInputManager;
    private KecPlaceholderExpansion placeholderExpansion;

    private static boolean papiHooked = false;

    private FileConfiguration messagesConfig;
    private FileConfiguration guiConfig;

    @Override
    public void onEnable() {
        instance = this;

        loadConfigs();
        MessageUtil.load(this);

        if (!setupStorage()) {
            getLogger().severe("Failed to initialize storage. Disabling plugin.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        this.service = new KartaEmeraldServiceImpl(this, this.economyDataHandler);
        Bukkit.getServicesManager().register(KartaEmeraldService.class, this.service, this, ServicePriority.Normal);

        this.chatInputManager = new ChatInputManager(this);

        setupHooks();
        setupCommands();
        setupListeners();

        getLogger().info("KartaEmeraldCurrency has been enabled successfully.");
    }

    @Override
    public void onDisable() {
        if (storageManager != null) {
            storageManager.shutdown();
        }
        getLogger().info("KartaEmeraldCurrency has been disabled.");
    }

    public void reload() {
        reloadConfig();
        this.messagesConfig = loadCustomConfig("messages.yml");
        this.guiConfig = loadCustomConfig("gui.yml");
        MessageUtil.load(this);
        storageManager.reload();
        getLogger().info("KartaEmeraldCurrency has been reloaded.");
    }

    private void loadConfigs() {
        saveDefaultConfig();
        this.messagesConfig = loadCustomConfig("messages.yml");
        this.guiConfig = loadCustomConfig("gui.yml");
    }

    private FileConfiguration loadCustomConfig(String fileName) {
        File file = new File(getDataFolder(), fileName);
        if (!file.exists()) {
            saveResource(fileName, false);
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    private boolean setupStorage() {
        try {
            this.storageManager = new StorageManager(this);
            this.storageManager.initialize();

            Executor asyncExecutor = (runnable) -> Bukkit.getScheduler().runTaskAsynchronously(this, runnable);
            this.economyDataHandler = new DefaultEconomyDataHandler(this.storageManager, asyncExecutor);
            return true;
        } catch (Exception e) {
            getLogger().severe("Could not initialize the storage manager.");
            e.printStackTrace();
            return false;
        }
    }

    private void setupHooks() {
        if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
            com.minekarta.kec.vault.VaultEconomyAdapter vaultAdapter = new com.minekarta.kec.vault.VaultEconomyAdapter(this.service);
            Bukkit.getServicesManager().register(net.milkbowl.vault.economy.Economy.class, vaultAdapter, this, ServicePriority.High);
            getLogger().info("Successfully hooked into Vault.");

            Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                Bukkit.getPluginManager().callEvent(new com.minekarta.kec.api.event.EconomyProviderRegisteredEvent(vaultAdapter.getName()));
            });
        } else {
            getLogger().warning("Vault not found. Economy features will be limited.");
        }

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            papiHooked = true;
            this.placeholderExpansion = new com.minekarta.kec.placeholder.KecPlaceholderExpansion(this);
            this.placeholderExpansion.register();
            getLogger().info("Successfully hooked into PlaceholderAPI.");
        } else {
            papiHooked = false;
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
    }

    public static KartaEmeraldCurrencyPlugin getInstance() {
        return instance;
    }

    public KartaEmeraldService getService() {
        return service;
    }

    public EconomyDataHandler getEconomyDataHandler() {
        return economyDataHandler;
    }

    public FileConfiguration getPluginConfig() {
        return getConfig();
    }

    public FileConfiguration getMessagesConfig() {
        return messagesConfig;
    }

    public FileConfiguration getGuiConfig() {
        return guiConfig;
    }

    public ChatInputManager getChatInputManager() {
        return chatInputManager;
    }

    public KecPlaceholderExpansion getPlaceholderExpansion() {
        return placeholderExpansion;
    }

    public static boolean isPlaceholderApiHooked() {
        return papiHooked;
    }
}
