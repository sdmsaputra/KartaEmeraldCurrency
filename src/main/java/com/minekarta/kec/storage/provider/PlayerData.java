package com.minekarta.kec.storage.provider;

import java.util.Objects;

/**
 * A record to hold player currency data.
 * Using a class for SnakeYAML compatibility.
 */
public final class PlayerData {
    private long balance;

    /**
     * Default constructor for deserializers.
     */
    public PlayerData() {
        this.balance = 0L;
    }

    /**
     * Constructs a new PlayerData with a given balance.
     * @param balance The balance.
     */
    public PlayerData(long balance) {
        this.balance = balance;
    }

    /**
     * Gets the balance.
     * @return The balance.
     */
    public long getBalance() {
        return balance;
    }

    /**
     * Sets the balance.
     * @param balance The new balance.
     */
    public void setBalance(long balance) {
        this.balance = balance;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (PlayerData) obj;
        return this.balance == that.balance;
    }

    @Override
    public int hashCode() {
        return Objects.hash(balance);
    }

    @Override
    public String toString() {
        return "PlayerData[" +
                "balance=" + balance + ']';
    }
}
