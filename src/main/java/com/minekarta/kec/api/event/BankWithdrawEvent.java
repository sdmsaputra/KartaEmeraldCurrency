package com.minekarta.kec.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Called when a player is about to withdraw currency from their bank account into physical items.
 */
public class BankWithdrawEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private boolean isCancelled;

    private final Player player;
    private long amount;

    public BankWithdrawEvent(@NotNull Player player, long amount) {
        super(false); // This event involves inventory, so it must be synchronous
        this.player = player;
        this.amount = amount;
    }

    /**
     * Gets the player who is withdrawing.
     * @return The player.
     */
    @NotNull
    public Player getPlayer() {
        return player;
    }

    /**
     * Gets the amount being withdrawn.
     * @return The amount.
     */
    public long getAmount() {
        return amount;
    }

    /**
     * Sets the amount to be withdrawn.
     * @param amount The new amount.
     */
    public void setAmount(long amount) {
        this.amount = amount;
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
