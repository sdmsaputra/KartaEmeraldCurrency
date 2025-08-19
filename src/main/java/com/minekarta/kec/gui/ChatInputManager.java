package com.minekarta.kec.gui;

import com.minekarta.kec.KartaEmeraldCurrencyPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages chat input from players for GUI-based transactions.
 */
public class ChatInputManager implements Listener {

    private final KartaEmeraldCurrencyPlugin plugin;
    private final Map<UUID, TransactionType> pendingTransactions = new ConcurrentHashMap<>();

    /**
     * Constructs a new ChatInputManager.
     * @param plugin The plugin instance.
     */
    public ChatInputManager(KartaEmeraldCurrencyPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Requests chat input from a player for a specific transaction type.
     * @param player The player to request input from.
     * @param type The type of transaction.
     */
    public void requestInput(Player player, TransactionType type) {
        pendingTransactions.put(player.getUniqueId(), type);
        player.closeInventory();
        // TODO: Move these messages to messages.yml
        String prompt = (type == TransactionType.DEPOSIT) ? "deposit" : "withdraw";
        player.sendMessage("§aPlease enter the amount to " + prompt + " in chat, or type 'cancel' to abort.");
    }

    /**
     * Handles chat messages from players who are in the middle of a transaction.
     * @param event The chat event.
     */
    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID playerUuid = player.getUniqueId();

        if (pendingTransactions.containsKey(playerUuid)) {
            event.setCancelled(true);

            TransactionType type = pendingTransactions.remove(playerUuid);
            String message = event.getMessage();

            if (message.equalsIgnoreCase("cancel")) {
                // TODO: Move message to config
                player.sendMessage("§cTransaction cancelled.");
                // Re-open BankGui on the main thread
                plugin.getServer().getScheduler().runTask(plugin, () -> new BankGui(plugin, player).open());
                return;
            }

            try {
                long amount = Long.parseLong(message);
                if (amount <= 0) {
                    // TODO: Move message to config
                    player.sendMessage("§cAmount must be a positive number.");
                    plugin.getServer().getScheduler().runTask(plugin, () -> new BankGui(plugin, player).open());
                    return;
                }

                // Run the transaction and handle the result
                handleTransaction(player, type, amount);

            } catch (NumberFormatException e) {
                // TODO: Move message to config
                player.sendMessage("§c'" + message + "' is not a valid number.");
                plugin.getServer().getScheduler().runTask(plugin, () -> new BankGui(plugin, player).open());
            }
        }
    }

    private void handleTransaction(Player player, TransactionType type, long amount) {
        if (type == TransactionType.DEPOSIT) {
            plugin.getService().depositToBank(player.getUniqueId(), amount).thenAccept(success -> {
                // Must run on main thread to send messages and open GUI
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (success) {
                        // TODO: Move message to config
                        player.sendMessage("§aSuccessfully deposited " + plugin.getService().getFormatter().formatWithCommas(amount) + " emeralds.");
                    } else {
                        // TODO: Move message to config
                        player.sendMessage("§cDeposit failed. You may not have enough emeralds in your inventory.");
                    }
                    new BankGui(plugin, player).open();
                });
            });
        } else { // WITHDRAW
            plugin.getService().withdrawFromBank(player.getUniqueId(), amount).thenAccept(success -> {
                // Must run on main thread to send messages and open GUI
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (success) {
                        // TODO: Move message to config
                        player.sendMessage("§aSuccessfully withdrew " + plugin.getService().getFormatter().formatWithCommas(amount) + " emeralds.");
                    } else {
                        // TODO: Move message to config
                        player.sendMessage("§cWithdrawal failed. You may not have enough funds or your inventory is full.");
                    }
                    new BankGui(plugin, player).open();
                });
            });
        }
    }
}
