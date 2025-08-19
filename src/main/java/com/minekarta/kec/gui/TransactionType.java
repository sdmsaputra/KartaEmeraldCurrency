package com.minekarta.kec.gui;

/**
 * Represents the type of a pending bank transaction initiated from a GUI.
 */
public enum TransactionType {
    /**
     * A transaction to deposit emeralds from the wallet to the bank.
     */
    DEPOSIT,
    /**
     * A transaction to withdraw emeralds from the bank to the wallet.
     */
    WITHDRAW,
    /**
     * The first step of a transfer: specifying the recipient player.
     */
    TRANSFER_PLAYER,
    /**
     * The second step of a transfer: specifying the amount.
     */
    TRANSFER_AMOUNT
}
