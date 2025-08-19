package com.minekarta.kec.api;

/**
 * Represents the reason for a currency transfer.
 * This can be used for logging, event handling, or applying different rules based on the context of the transaction.
 */
public enum TransferReason {
    /**
     * A transfer initiated by a player via the `/pay` command.
     */
    PLAYER_PAYMENT,

    /**
     * A transaction initiated by an administrator command.
     */
    ADMIN_COMMAND,

    /**
     * A transaction initiated by another plugin as a reward.
     */
    PLUGIN_REWARD,

    /**
     * A transaction initiated by another plugin for a purchase or service.
     */
    PLUGIN_PURCHASE,

    /**
     * A generic or unspecified reason for a transaction made by another plugin.
     */
    PLUGIN_GENERIC,

    /**
     * For any other reason not covered by the above categories.
     */
    OTHER
}
