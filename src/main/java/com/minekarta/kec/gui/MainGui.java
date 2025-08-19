package com.minekarta.kec.gui;

import com.minekarta.kec.KartaEmeraldCurrencyPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * The main GUI for the KartaEmeraldCurrency plugin.
 */
public class MainGui extends AbstractGui {

    /**
     * Constructs a new MainGui.
     * @param plugin The plugin instance.
     * @param player The player viewing the GUI.
     */
    public MainGui(KartaEmeraldCurrencyPlugin plugin, Player player) {
        super(plugin, player);
    }

    @Override
    public void open() {
        ConfigurationSection guiConfig = plugin.getGuiConfig().getConfigurationSection("main-menu");
        if (guiConfig == null) {
            player.sendMessage("Main GUI not configured!");
            return;
        }

        String title = guiConfig.getString("title", "Main Menu");
        int size = guiConfig.getInt("size", 36);
        createInventory(title, size);

        // Set items from config
        ConfigurationSection itemsConfig = guiConfig.getConfigurationSection("items");
        if (itemsConfig != null) {
            itemsConfig.getKeys(false).forEach(key -> {
                ConfigurationSection itemConfig = itemsConfig.getConfigurationSection(key);
                int slot = itemConfig.getInt("slot");
                ItemStack item = createItem(itemConfig);
                inventory.setItem(slot, item);
            });
        }

        fill(guiConfig);

        player.openInventory(inventory);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getSlot();
        ConfigurationSection guiConfig = plugin.getGuiConfig().getConfigurationSection("main-menu.items");
        if (guiConfig == null) return;

        // Find which item was clicked by checking slots in config
        for (String key : guiConfig.getKeys(false)) {
            if (guiConfig.getInt(key + ".slot") == slot) {
                switch (key) {
                    case "bank-access":
                        new BankGui(plugin, player).open();
                        break;
                    case "transfer":
                        plugin.getChatInputManager().requestInput(player, TransactionType.TRANSFER_PLAYER);
                        break;
                    case "leaderboard":
                        new LeaderboardGui(plugin, player).open();
                        break;
                    case "close-button":
                        player.closeInventory();
                        break;
                }
                return;
            }
        }
    }
}
