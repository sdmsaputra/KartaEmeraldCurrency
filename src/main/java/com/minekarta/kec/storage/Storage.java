package com.minekarta.kec.storage;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * An interface for abstracting the data storage operations of the plugin.
 * All operations are asynchronous and return a {@link CompletableFuture}.
 */
public interface Storage {
    /**
     * Retrieves a sorted list of top balances.
     *
     * @param limit The maximum number of results to return.
     * @param offset The starting offset for the results (for pagination).
     * @return A CompletableFuture that resolves to a map of Player UUIDs to their balances, sorted descending.
     */
    CompletableFuture<Map<UUID, Long>> getTopBalances(int limit, int offset);

    /**
     * Gets the total number of player accounts in the database.
     *
     * @return A CompletableFuture that resolves to the total account count.
     */
    CompletableFuture<Integer> getAccountCount();


    /**
     * Initializes the storage backend. This can include creating tables if they don't exist.
     * This method is expected to be called synchronously on startup.
     */
    void initialize();

    /**
     * Closes any connections and cleans up resources used by the storage backend.
     */
    void close();

    /**
     * Retrieves the bank balance for a given player.
     *
     * @param uuid The UUID of the player.
     * @return A CompletableFuture that resolves to the player's balance, or 0 if not found.
     */
    CompletableFuture<Long> getBalance(@NotNull UUID uuid);

    /**
     * Sets the bank balance for a given player. If the player does not exist,
     * an account may be created.
     *
     * @param uuid The UUID of the player.
     * @param balance The new balance to set.
     * @return A CompletableFuture that completes when the operation is finished.
     */
    CompletableFuture<Void> setBalance(@NotNull UUID uuid, long balance);

    /**
     * Atomically performs a transaction involving a sender and a receiver.
     *
     * @param from The UUID of the sender.
     * @param to The UUID of the receiver.
     * @param amount The amount to transfer.
     * @param fee The fee for the transaction.
     * @return A CompletableFuture that resolves to true if the transaction was successful, false otherwise.
     */
    CompletableFuture<Boolean> performTransfer(@NotNull UUID from, @NotNull UUID to, long amount, long fee);

    /**
     * Checks if a player has an account.
     *
     * @param uuid The UUID of the player to check.
     * @return A CompletableFuture that resolves to true if the account exists.
     */
    CompletableFuture<Boolean> hasAccount(@NotNull UUID uuid);

    /**
     * Creates an account for a player, usually with a default starting balance.
     *
     * @param uuid The UUID of the player.
     * @param startingBalance The initial balance for the account.
     * @return A CompletableFuture that completes when the account is created.
     */
    CompletableFuture<Void> createAccount(@NotNull UUID uuid, long startingBalance);

    /**
     * Atomically adds an amount to a player's balance.
     * @param uuid The UUID of the player.
     * @param amount The amount to add.
     * @return A CompletableFuture that resolves to the new balance.
     */
    CompletableFuture<Long> addBalance(@NotNull UUID uuid, long amount);

    /**
     * Atomically removes an amount from a player's balance.
     * @param uuid The UUID of the player.
     * @param amount The amount to remove.
     * @return A CompletableFuture that resolves to the new balance.
     */
    CompletableFuture<Long> removeBalance(@NotNull UUID uuid, long amount);
}
