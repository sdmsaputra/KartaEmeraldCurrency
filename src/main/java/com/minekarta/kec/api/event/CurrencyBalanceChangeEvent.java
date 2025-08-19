package com.minekarta.kec.api.event;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Called when a player's bank balance is about to change.
 * This event is cancellable.
 */
public class CurrencyBalanceChangeEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private boolean isCancelled;

    private final UUID playerId;
    private final ChangeReason reason;
    private final long from;
    private long to;

    public CurrencyBalanceChangeEvent(boolean isAsync, @NotNull UUID playerId, @NotNull ChangeReason reason, long from, long to) {
        super(isAsync);
        this.playerId = playerId;
        this.reason = reason;
        this.from = from;
        this.to = to;
        this.isCancelled = false;
    }

    /**
     * Gets the UUID of the player whose balance is changing.
     * @return The player's UUID.
     */
    @NotNull
    public UUID getPlayerId() {
        return playerId;
    }

    /**
     * Gets the reason for the balance change.
     * @return The reason for the change.
     */
    @NotNull
    public ChangeReason getReason() {
        return reason;
    }

    /**
     * Gets the balance before the change.
     * @return The original balance.
     */
    public long getFrom() {
        return from;
    }

    /**
     * Gets the balance after the change.
     * @return The new balance.
     */
    public long getTo() {
        return to;
    }

    /**
     * Sets the final balance.
     * @param to The new balance to be set.
     */
    public void setTo(long to) {
        this.to = to;
    }

    @Override
    public boolean isCancelled() {
        return isCancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.isCancelled = cancel;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return handlers;
    }

    /**
     * Represents the cause of the balance change.
     */
    public enum ChangeReason {
        ADMIN_SET,
        ADMIN_ADD,
        ADMIN_REMOVE,
        DEPOSIT,
        WITHDRAW,
        TRANSFER_SEND,
        TRANSFER_RECEIVE,
        PLUGIN_API,
        OTHER
    }
}
