package com.minekarta.kec.api.event;

import com.minekarta.kec.api.TransferReason;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Called when a currency transfer between two parties is initiated.
 * This event allows for modification of the amount and cancellation of the transfer.
 */
public class CurrencyTransferEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private boolean isCancelled;

    private final UUID from;
    private final UUID to;
    private final TransferReason reason;
    private long amount;
    private long fee;

    public CurrencyTransferEvent(boolean isAsync, @NotNull UUID from, @NotNull UUID to, @Nullable TransferReason reason, long amount, long fee) {
        super(isAsync);
        this.from = from;
        this.to = to;
        this.reason = reason;
        this.amount = amount;
        this.fee = fee;
    }

    /**
     * Gets the UUID of the party sending the currency.
     * @return The sender's UUID.
     */
    @NotNull
    public UUID getFrom() {
        return from;
    }

    /**
     * Gets the UUID of the party receiving the currency.
     * @return The receiver's UUID.
     */
    @NotNull
    public UUID getTo() {
        return to;
    }

    /**
     * Gets the reason for the transfer.
     * @return The transfer reason, or null if not specified.
     */
    @Nullable
    public TransferReason getReason() {
        return reason;
    }

    /**
     * Gets the amount of currency being transferred before fees.
     * @return The transfer amount.
     */
    public long getAmount() {
        return amount;
    }

    /**
     * Sets the amount of currency to be transferred.
     * @param amount The new amount.
     */
    public void setAmount(long amount) {
        this.amount = amount;
    }

    /**
     * Gets the fee that will be applied to this transaction.
     * @return The transaction fee.
     */
    public long getFee() {
        return fee;
    }

    /**
     * Sets the fee to be applied to this transaction.
     * @param fee The new fee.
     */
    public void setFee(long fee) {
        this.fee = fee;
    }

    /**
     * Gets the final amount the sender will lose (amount + fee).
     * @return The total amount deducted from the sender.
     */
    public long getFinalDeduction() {
        return amount + fee;
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
}
