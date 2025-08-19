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

    /**
     * Constructs a new CurrencyBalanceChangeEvent.
     *
     * @param isAsync Whether the event is asynchronous.
     * @param playerId The UUID of the player.
     * @param reason The reason for the change.
     * @param from The original balance.
     * @param to The new balance.
     */
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

    /**
     * Gets the handler list for this event.
     * @return The handler list.
     */
    @NotNull
    public static HandlerList getHandlerList() {
        return handlers;
    }

    /**
     * Represents the cause of the balance change.
     */
    public enum ChangeReason {
        /**
         * Balance set by an administrator.
         */
        ADMIN_SET,
        /**
         * Balance increased by an administrator.
         */
        ADMIN_ADD,
        /**
         * Balance decreased by an administrator.
         */
        ADMIN_REMOVE,
        /**
         * Balance changed due to a deposit.
         */
        DEPOSIT,
        /**
         * Balance changed due to a withdrawal.
         */
        WITHDRAW,
        /**
         * Balance changed due to sending a transfer.
         */
        TRANSFER_SEND,
        /**
         * Balance changed due to receiving a transfer.
         */
        TRANSFER_RECEIVE,
        /**
         * Balance changed by another plugin via the API.
         */
        PLUGIN_API,
        /**
         * Balance changed for another reason.
         */
        OTHER
    }
}
