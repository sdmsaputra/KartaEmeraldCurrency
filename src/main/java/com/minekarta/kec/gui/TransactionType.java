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
    WITHDRAW
}
