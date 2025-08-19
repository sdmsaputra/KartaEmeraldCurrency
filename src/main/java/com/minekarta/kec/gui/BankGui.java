package com.minekarta.kec.gui;

import com.minekarta.kec.KartaEmeraldCurrencyPlugin;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * The bank GUI for depositing and withdrawing emeralds.
 */
public class BankGui extends AbstractGui {

    /**
     * Constructs a new BankGui.
     *
     * @param plugin The plugin instance.
     * @param player The player viewing the GUI.
     */
    public BankGui(KartaEmeraldCurrencyPlugin plugin, Player player) {
        super(plugin, player);
    }

    @Override
    public void open() {
        ConfigurationSection guiConfig = plugin.getGuiConfig().getConfigurationSection("bank-menu");
        if (guiConfig == null) {
            player.sendMessage("Bank GUI not configured!");
            return;
        }

        String title = guiConfig.getString("title", "Bank");
        int size = guiConfig.getInt("size", 54);
        createInventory(title, size);

        // Asynchronously fetch balance and then populate the GUI
        plugin.getService().getBankBalance(player.getUniqueId()).thenAccept(bankBalance -> {
            long walletBalance = plugin.getService().getWalletBalance(player);

            TagResolver balanceResolvers = TagResolver.builder()
                    .resolver(Placeholder.unparsed("player_name", player.getName()))
                    .resolver(Placeholder.unparsed("balance_bank", plugin.getService().getFormatter().formatWithCommas(bankBalance)))
                    .resolver(Placeholder.unparsed("balance_wallet", plugin.getService().getFormatter().formatWithCommas(walletBalance)))
                    .build();

            // Set items from config
            guiConfig.getConfigurationSection("items").getKeys(false).forEach(key -> {
                ConfigurationSection itemConfig = guiConfig.getConfigurationSection("items." + key);
                int slot = itemConfig.getInt("slot");
                ItemStack item = createItem(itemConfig, balanceResolvers);
                inventory.setItem(slot, item);
            });

            fill(guiConfig);

            // Open inventory on main thread
            plugin.getServer().getScheduler().runTask(plugin, () -> player.openInventory(inventory));
        });
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getSlot();
        ConfigurationSection guiConfig = plugin.getGuiConfig().getConfigurationSection("bank-menu.items");
        if (guiConfig == null) return;

        // Find which item was clicked by checking slots in config
        for (String key : guiConfig.getKeys(false)) {
            if (guiConfig.getInt(key + ".slot") == slot) {
                switch (key) {
                    case "deposit-custom":
                        plugin.getChatInputManager().requestInput(player, TransactionType.DEPOSIT);
                        break;
                    case "withdraw-custom":
                        plugin.getChatInputManager().requestInput(player, TransactionType.WITHDRAW);
                        break;
                    case "deposit-all":
                        handleDepositAll();
                        break;
                    case "back-button":
                        new MainGui(plugin, player).open();
                        break;
                    case "close-button":
                        player.closeInventory();
                        break;
                }
                return;
            }
        }
    }

    private void handleDepositAll() {
        long walletBalance = plugin.getService().getWalletBalance(player);
        if (walletBalance <= 0) {
            //TODO: Move to messages.yml
            player.sendMessage("§cYou don't have any emeralds in your inventory to deposit.");
            return;
        }

        plugin.getService().depositToBank(player.getUniqueId(), walletBalance).thenAccept(success -> {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (success) {
                    //TODO: Move to messages.yml
                    player.sendMessage("§aSuccessfully deposited all " + plugin.getService().getFormatter().formatWithCommas(walletBalance) + " emeralds.");
                    // Re-open the GUI to show updated balance
                    open();
                } else {
                    // This case should ideally not happen if walletBalance > 0
                    player.sendMessage("§cAn unexpected error occurred during deposit.");
                }
            });
        });
    }

}
