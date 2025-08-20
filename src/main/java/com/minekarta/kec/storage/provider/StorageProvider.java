package com.minekarta.kec.storage.provider;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * An interface for a raw data storage provider.
 * Implementations of this interface handle the low-level details of persisting player data
 * to a specific backend (e.g., files, a database).
 *
 * All methods in this interface are expected to be synchronous and should be called
 * on the appropriate thread by the consuming class (e.g., {@code EconomyDataHandler}).
 */
public interface StorageProvider {

    /**
     * Initializes the storage provider.
     * This could involve creating files, directories, or preparing database connections.
     */
    void initialize();

    /**
     * Shuts down the storage provider.
     * This is where data should be saved and connections should be closed.
     */
    void shutdown();

    /**
     * Retrieves the data for a specific player.
     *
     * @param uuid The UUID of the player.
     * @return An {@link Optional} containing the {@link PlayerData} if found, otherwise empty.
     */
    Optional<PlayerData> getPlayerData(@NotNull UUID uuid);

    /**
     * Saves or updates the data for a specific player.
     *
     * @param uuid The UUID of the player.
     * @param data The {@link PlayerData} to save.
     */
    void savePlayerData(@NotNull UUID uuid, @NotNull PlayerData data);

    /**
     * Deletes the data for a specific player.
     *
     * @param uuid The UUID of the player to delete.
     */
    void deletePlayerData(@NotNull UUID uuid);

    /**
     * Retrieves all player data from the storage.
     * This can be an expensive operation and should be used with caution.
     *
     * @return A map of all player UUIDs to their {@link PlayerData}.
     */
    Map<UUID, PlayerData> getAllPlayerData();
}
