package com.minekarta.kec.api;

import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * The main public API for interacting with the KartaEmeraldCurrency economy.
 * <p>
 * This service provides methods to manage player balances, perform transactions,
 * and query economic data. All data-mutating operations and potentially slow
un-cached
 * queries return a {@link CompletableFuture} to ensure server performance.
 * <p>
 * Example usage:
 * <pre>{@code
 * KartaEmeraldService service = Bukkit.getServicesManager().load(KartaEmeraldService.class);
 * if (service != null) {
 *     service.getBankBalance(player.getUniqueId()).thenAccept(balance -> {
 *         player.sendMessage("Your bank balance is: " + balance);
 *     });
 * }
 * }</pre>
 */
public interface KartaEmeraldService {

    /**
     * Checks if a player has an account in the database.
     *
     * @param playerId The UUID of the player.
     * @return A CompletableFuture that will resolve to true if an account exists, false otherwise.
     */
    CompletableFuture<Boolean> hasAccount(@NotNull UUID playerId);

    /**
     * Creates a new economy account for a player, typically with a starting balance
     * defined in the configuration. If an account already exists, this method does nothing.
     *
     * @param playerId The UUID of the player.
     * @return A CompletableFuture that completes when the operation is finished.
     */
    CompletableFuture<Void> createAccount(@NotNull UUID playerId);

    /**
     * Gets the player's virtual bank balance.
     *
     * @param playerId The UUID of the player.
     * @return A CompletableFuture resolving to the player's bank balance. Returns 0 if the account doesn't exist.
     */
    CompletableFuture<Long> getBankBalance(@NotNull UUID playerId);

    /**
     * Gets the player's physical wallet balance (number of emeralds in inventory).
     * This is a synchronous operation as it queries the live inventory.
     *
     * @param player The online player.
     * @return The number of emeralds in the player's inventory.
     */
    long getWalletBalance(@NotNull OfflinePlayer player);

    /**
     * Deposits a specific amount of physical emeralds from a player's inventory into their bank account.
     *
     * @param playerId The UUID of the player.
     * @param amount The amount of emeralds to deposit. Must be positive.
     * @return A CompletableFuture resolving to true if the deposit was successful, false otherwise.
     */
    CompletableFuture<Boolean> depositToBank(@NotNull UUID playerId, long amount);

    /**
     * Withdraws a specific amount from a player's bank account into physical emeralds in their inventory.
     *
     * @param playerId The UUID of the player.
     * @param amount The amount to withdraw. Must be positive.
     * @return A CompletableFuture resolving to true if the withdrawal was successful, false otherwise.
     *         Fails if balance is insufficient or inventory is full.
     */
    CompletableFuture<Boolean> withdrawFromBank(@NotNull UUID playerId, long amount);

    /**
     * Transfers an amount from one player's bank account to another's.
     *
     * @param from   The UUID of the player sending the money.
     * @param to     The UUID of the player receiving the money.
     * @param amount The amount to transfer. Must be positive.
     * @param reason An optional reason for the transfer, for logging or event purposes.
     * @return A CompletableFuture resolving to true if the transfer was successful.
     */
    CompletableFuture<Boolean> transfer(@NotNull UUID from, @NotNull UUID to, long amount, @Nullable TransferReason reason);

    /**
     * Sets a player's bank balance to a specific amount. This is an admin operation.
     *
     * @param playerId The UUID of the player.
     * @param amount The new balance.
     * @return A CompletableFuture resolving to true if the operation was successful.
     */
    CompletableFuture<Boolean> setBankBalance(@NotNull UUID playerId, long amount);

    /**
     * Adds a specific amount to a player's bank balance. This is an admin operation.
     *
     * @param playerId The UUID of the player.
     * @param delta The amount to add.
     * @return A CompletableFuture resolving to true if the operation was successful.
     */
    CompletableFuture<Boolean> addBankBalance(@NotNull UUID playerId, long delta);

    /**
     * Removes a specific amount from a player's bank balance. This is an admin operation.
     *
     * @param playerId The UUID of the player.
     * @param delta The amount to remove.
     * @return A CompletableFuture resolving to true if the operation was successful.
     */
    CompletableFuture<Boolean> removeBankBalance(@NotNull UUID playerId, long delta);

    /**
     * Provides access to the currency formatting utility.
     *
     * @return The currency formatter.
     */
    @NotNull
    CurrencyFormatter getFormatter();
}
