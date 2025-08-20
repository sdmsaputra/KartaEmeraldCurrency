package com.minekarta.kec.storage.provider;

import com.minekarta.kec.KartaEmeraldCurrencyPlugin;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.representer.Representer;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class FileStorageProvider implements StorageProvider {

    private final KartaEmeraldCurrencyPlugin plugin;
    private final Path dataFolderPath;
    private final Yaml yaml;
    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> dirtyPlayers = new ConcurrentHashMap<>();

    private BukkitTask autoSaveTask;

    public FileStorageProvider(KartaEmeraldCurrencyPlugin plugin) {
        this.plugin = plugin;
        this.dataFolderPath = plugin.getDataFolder().toPath().resolve("data");

        // Configure SnakeYAML
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        Representer representer = new Representer(new DumperOptions());
        representer.getPropertyUtils().setSkipMissingProperties(true);
        this.yaml = new Yaml(new Constructor(PlayerData.class, new org.yaml.snakeyaml.LoaderOptions()), representer, options);
    }

    @Override
    public void initialize() {
        try {
            Files.createDirectories(dataFolderPath);
            loadAllPlayerData();
            startAutoSave();
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create data directory", e);
        }
    }

    @Override
    public void shutdown() {
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
        }
        saveAllDirtyData(false); // Save synchronously on shutdown
    }

    @Override
    public Optional<PlayerData> getPlayerData(@NotNull UUID uuid) {
        return Optional.ofNullable(cache.get(uuid));
    }

    @Override
    public void savePlayerData(@NotNull UUID uuid, @NotNull PlayerData data) {
        cache.put(uuid, data);
        dirtyPlayers.put(uuid, true);
    }

    @Override
    public void deletePlayerData(@NotNull UUID uuid) {
        cache.remove(uuid);
        // Also mark as dirty to ensure file deletion
        dirtyPlayers.put(uuid, true);
    }

    @Override
    public Map<UUID, PlayerData> getAllPlayerData() {
        return new ConcurrentHashMap<>(cache);
    }

    private void loadAllPlayerData() {
        File[] playerFiles = dataFolderPath.toFile().listFiles((dir, name) -> name.endsWith(".yml"));
        if (playerFiles == null) return;

        for (File file : playerFiles) {
            String fileName = file.getName();
            try {
                UUID uuid = UUID.fromString(fileName.substring(0, fileName.length() - 4));
                try (FileReader reader = new FileReader(file)) {
                    PlayerData data = yaml.load(reader);
                    if (data != null) {
                        cache.put(uuid, data);
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to load player data from " + fileName, e);
            }
        }
        plugin.getLogger().info("Loaded data for " + cache.size() + " players from files.");
    }

    private void startAutoSave() {
        // TODO: Make this configurable
        long interval = 20L * 60 * 5; // 5 minutes
        this.autoSaveTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> saveAllDirtyData(true), interval, interval);
    }

    private void saveAllDirtyData(boolean async) {
        // Create a snapshot of dirty players to work on
        Map<UUID, Boolean> snapshot = new ConcurrentHashMap<>(dirtyPlayers);
        dirtyPlayers.clear();

        if (snapshot.isEmpty()) {
            return;
        }

        Runnable saveTask = () -> {
            int savedCount = 0;
            int deletedCount = 0;
            for (UUID uuid : snapshot.keySet()) {
                Path playerFile = dataFolderPath.resolve(uuid + ".yml");
                PlayerData data = cache.get(uuid);

                if (data != null) { // Player has data, so save it
                    try (FileWriter writer = new FileWriter(playerFile.toFile())) {
                        yaml.dump(data, writer);
                        savedCount++;
                    } catch (IOException e) {
                        plugin.getLogger().log(Level.SEVERE, "Failed to save data for " + uuid, e);
                        // Put it back in the dirty map to retry next time
                        dirtyPlayers.put(uuid, true);
                    }
                } else { // Player data was removed, so delete the file
                    try {
                        Files.deleteIfExists(playerFile);
                        deletedCount++;
                    } catch (IOException e) {
                        plugin.getLogger().log(Level.SEVERE, "Failed to delete data file for " + uuid, e);
                        // Put it back in the dirty map to retry next time
                        dirtyPlayers.put(uuid, true);
                    }
                }
            }
            if(savedCount > 0 || deletedCount > 0) {
                plugin.getLogger().info("Auto-saved data for " + savedCount + " players and deleted data for " + deletedCount + " players.");
            }
        };

        if (async) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, saveTask);
        } else {
            saveTask.run();
        }
    }
}
