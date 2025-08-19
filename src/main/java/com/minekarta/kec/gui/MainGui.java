package com.minekarta.kec.gui;

import com.minekarta.kec.KartaEmeraldCurrencyPlugin;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.stream.Collectors;

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

        // Asynchronously fetch balance and then populate the GUI
        plugin.getService().getBankBalance(player.getUniqueId()).thenAccept(bankBalance -> {
            long walletBalance = plugin.getService().getWalletBalance(player);
            long totalBalance = bankBalance + walletBalance;

            TagResolver balanceResolvers = TagResolver.builder()
                    .resolver(Placeholder.unparsed("player_name", player.getName()))
                    .resolver(Placeholder.unparsed("balance_bank", plugin.getService().getFormatter().formatWithCommas(bankBalance)))
                    .resolver(Placeholder.unparsed("balance_wallet", plugin.getService().getFormatter().formatWithCommas(walletBalance)))
                    .resolver(Placeholder.unparsed("balance_total", plugin.getService().getFormatter().formatWithCommas(totalBalance)))
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
        ConfigurationSection guiConfig = plugin.getGuiConfig().getConfigurationSection("main-menu.items");
        if (guiConfig == null) return;

        // Find which item was clicked by checking slots in config
        for (String key : guiConfig.getKeys(false)) {
            if (guiConfig.getInt(key + ".slot") == slot) {
                switch (key) {
                    case "bank-access":
                        // new BankGui(plugin, player).open(); // TODO: Create BankGui
                        player.sendMessage("Bank GUI coming soon!");
                        break;
                    case "transfer":
                        player.sendMessage("Transfer GUI coming soon!");
                        break;
                    case "leaderboard":
                        player.sendMessage("Leaderboard GUI coming soon!");
                        break;
                    case "close-button":
                        player.closeInventory();
                        break;
                }
                return;
            }
        }
    }

    // Override createItem to handle placeholders
    private ItemStack createItem(ConfigurationSection itemConfig, TagResolver resolvers) {
        ItemStack item = super.createItem(itemConfig);
        if (item == null) return null;

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (meta.hasDisplayName()) {
                meta.displayName(MiniMessage.miniMessage().deserialize(MiniMessage.miniMessage().serialize(meta.displayName()), resolvers));
            }
            if (meta.hasLore()) {
                List<String> loreLines = itemConfig.getStringList("lore");
                meta.lore(loreLines.stream()
                        .map(line -> MiniMessage.miniMessage().deserialize(line, resolvers))
                        .collect(Collectors.toList()));
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}
