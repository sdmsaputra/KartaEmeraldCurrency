package com.minekarta.kec.gui;

import com.minekarta.kec.KartaEmeraldCurrencyPlugin;
import com.minekarta.kec.util.MessageUtil;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages chat input from players for GUI-based transactions.
 */
public class ChatInputManager implements Listener {

    private final KartaEmeraldCurrencyPlugin plugin;
    private final Map<UUID, TransactionType> pendingTransactions = new ConcurrentHashMap<>();
    private final Map<UUID, String> transferTargets = new ConcurrentHashMap<>();


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
        switch (type) {
            case DEPOSIT:
                MessageUtil.sendMessage(player, "chat-input-deposit-prompt");
                break;
            case WITHDRAW:
                MessageUtil.sendMessage(player, "chat-input-withdraw-prompt");
                break;
            case TRANSFER_PLAYER:
                MessageUtil.sendMessage(player, "chat-input-recipient-prompt");
                break;
            case TRANSFER_AMOUNT:
                String targetName = transferTargets.get(player.getUniqueId());
                MessageUtil.sendMessage(player, "chat-input-amount-prompt", MessageUtil.placeholder("player", targetName));
                break;
        }
    }

    /**
     * Handles chat messages from players who are in the middle of a transaction.
     * @param event The chat event.
     */
    @EventHandler
    public void onPlayerChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        UUID playerUuid = player.getUniqueId();

        if (pendingTransactions.containsKey(playerUuid)) {
            event.setCancelled(true);

            TransactionType type = pendingTransactions.remove(playerUuid);
            String message = PlainTextComponentSerializer.plainText().serialize(event.message());

            if (message.equalsIgnoreCase("cancel")) {
                MessageUtil.sendMessage(player, "chat-input-cancelled");
                plugin.getServer().getScheduler().runTask(plugin, () -> new MainGui(plugin, player).open());
                return;
            }

            switch (type) {
                case DEPOSIT:
                case WITHDRAW:
                    handleAmountInput(player, message, type);
                    break;
                case TRANSFER_PLAYER:
                    handlePlayerInput(player, message);
                    break;
                case TRANSFER_AMOUNT:
                    handleTransferAmountInput(player, message);
                    break;
            }
        }
    }

    private void handleAmountInput(Player player, String message, TransactionType type) {
        try {
            long amount = Long.parseLong(message);
            if (amount <= 0) {
                MessageUtil.sendMessage(player, "chat-input-must-be-positive");
                plugin.getServer().getScheduler().runTask(plugin, () -> new BankGui(plugin, player).open());
                return;
            }
            handleTransaction(player, type, amount);
        } catch (NumberFormatException e) {
            MessageUtil.sendMessage(player, "chat-input-invalid-number", MessageUtil.placeholder("input", message));
            plugin.getServer().getScheduler().runTask(plugin, () -> new BankGui(plugin, player).open());
        }
    }

    private void handlePlayerInput(Player player, String targetName) {
        if (targetName.equalsIgnoreCase(player.getName())) {
            MessageUtil.sendMessage(player, "cannot-pay-self");
            plugin.getServer().getScheduler().runTask(plugin, () -> new MainGui(plugin, player).open());
            return;
        }

        Player target = Bukkit.getPlayer(targetName);
        if (target == null || !target.isOnline()) {
            MessageUtil.sendMessage(player, "player-not-found", MessageUtil.placeholder("player", targetName));
            plugin.getServer().getScheduler().runTask(plugin, () -> new MainGui(plugin, player).open());
            return;
        }

        transferTargets.put(player.getUniqueId(), target.getName());
        requestInput(player, TransactionType.TRANSFER_AMOUNT);
    }

    private void handleTransferAmountInput(Player player, String message) {
        String targetName = transferTargets.remove(player.getUniqueId());
        if (targetName == null) {
            // This should not happen, but as a safeguard.
            plugin.getServer().getScheduler().runTask(plugin, () -> new MainGui(plugin, player).open());
            return;
        }

        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            MessageUtil.sendMessage(player, "player-not-found", MessageUtil.placeholder("player", targetName));
            plugin.getServer().getScheduler().runTask(plugin, () -> new MainGui(plugin, player).open());
            return;
        }

        try {
            long amount = Long.parseLong(message);
            if (amount <= 0) {
                MessageUtil.sendMessage(player, "chat-input-must-be-positive");
                plugin.getServer().getScheduler().runTask(plugin, () -> new MainGui(plugin, player).open());
                return;
            }

            plugin.getService().transfer(player.getUniqueId(), target.getUniqueId(), amount, com.minekarta.kec.api.TransferReason.PLAYER_PAYMENT)
                    .thenAccept(success -> {
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            if (success) {
                                MessageUtil.sendMessage(player, "pay-success-sender",
                                        MessageUtil.placeholder("amount", plugin.getService().getFormatter().formatWithCommas(amount)),
                                        MessageUtil.placeholder("target", targetName));
                                Player onlineTarget = Bukkit.getPlayer(target.getUniqueId());
                                if (onlineTarget != null) {
                                    MessageUtil.sendMessage(onlineTarget, "pay-received-notification",
                                            MessageUtil.placeholder("amount", plugin.getService().getFormatter().formatWithCommas(amount)),
                                            MessageUtil.placeholder("sender", player.getName()));
                                }
                            } else {
                                MessageUtil.sendMessage(player, "insufficient-funds");
                            }
                            new MainGui(plugin, player).open();
                        });
                    });

        } catch (NumberFormatException e) {
            MessageUtil.sendMessage(player, "chat-input-invalid-number", MessageUtil.placeholder("input", message));
            plugin.getServer().getScheduler().runTask(plugin, () -> new MainGui(plugin, player).open());
        }
    }


    private void handleTransaction(Player player, TransactionType type, long amount) {
        if (type == TransactionType.DEPOSIT) {
            plugin.getService().depositToBank(player.getUniqueId(), amount).thenAccept(success -> {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (success) {
                        MessageUtil.sendMessage(player, "deposit-success", MessageUtil.placeholder("amount", plugin.getService().getFormatter().formatWithCommas(amount)));
                    } else {
                        MessageUtil.sendMessage(player, "insufficient-items");
                    }
                    new BankGui(plugin, player).open();
                });
            });
        } else { // WITHDRAW
            plugin.getService().withdrawFromBank(player.getUniqueId(), amount).thenAccept(success -> {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (success) {
                        MessageUtil.sendMessage(player, "withdraw-success", MessageUtil.placeholder("amount", plugin.getService().getFormatter().formatWithCommas(amount)));
                    } else {
                        MessageUtil.sendMessage(player, "insufficient-funds");
                    }
                    new BankGui(plugin, player).open();
                });
            });
        }
    }
}
