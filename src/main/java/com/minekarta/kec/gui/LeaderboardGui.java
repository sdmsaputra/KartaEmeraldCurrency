package com.minekarta.kec.gui;

import com.minekarta.kec.KartaEmeraldCurrencyPlugin;
import com.minekarta.kec.util.MessageUtil;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class LeaderboardGui extends AbstractGui {

    private int page = 0;
    private final int ENTRIES_PER_PAGE = 45; // Slots 0-44

    public LeaderboardGui(KartaEmeraldCurrencyPlugin plugin, Player player) {
        super(plugin, player);
    }

    public LeaderboardGui(KartaEmeraldCurrencyPlugin plugin, Player player, int page) {
        super(plugin, player);
        this.page = page;
    }

    @Override
    public void open() {
        ConfigurationSection guiConfig = plugin.getGuiConfig().getConfigurationSection("leaderboard-menu");
        if (guiConfig == null) {
            MessageUtil.sendRawMessage(player, "<red>Leaderboard GUI not configured!</red>");
            return;
        }

        plugin.getService().getAccountCount().thenAcceptBoth(
                plugin.getService().getTopBalances(ENTRIES_PER_PAGE, page * ENTRIES_PER_PAGE),
                (totalAccounts, topBalances) -> {
                    // Run GUI updates on the main thread
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        populateGui(guiConfig, totalAccounts, topBalances);
                    });
                }
        );
    }

    private void populateGui(ConfigurationSection guiConfig, long totalAccounts, Map<UUID, Long> topBalances) {
        int maxPage = (int) Math.ceil((double) totalAccounts / ENTRIES_PER_PAGE);
        if (maxPage == 0) maxPage = 1;

        TagResolver pageTitleResolver = MessageUtil.placeholder("page", page + 1);
        String title = guiConfig.getString("title", "Leaderboard");
        int size = guiConfig.getInt("size", 54);
        createInventory(title, size, pageTitleResolver);

        // Populate player items
        ConfigurationSection playerItemConfig = guiConfig.getConfigurationSection("player-item");
        AtomicInteger rank = new AtomicInteger(page * ENTRIES_PER_PAGE + 1);
        List<Map.Entry<UUID, Long>> entries = new ArrayList<>(topBalances.entrySet());

        for (int i = 0; i < ENTRIES_PER_PAGE; i++) {
            if (i >= entries.size()) break;

            Map.Entry<UUID, Long> entry = entries.get(i);
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(entry.getKey());

            // For the player item, we only need to resolve the rank.
            // The player's name and balance will be handled by the new PAPI placeholders.
            // Note: We will need to change the player-item lore in gui.yml to use PAPI placeholders for this to work.
            // e.g. lore: - "<white>Balance: <gold>%kartaemerald_top_{rank}_balance_formatted%</gold>"
            // This is not ideal, as PAPI placeholders are resolved relative to the VIEWING player, not the player in the skull.
            // A better approach is to resolve them manually.
            String playerName = offlinePlayer.getName() != null ? offlinePlayer.getName() : "Unknown";
            String formattedBalance = plugin.getService().getFormatter().formatWithCommas(entry.getValue());

            TagResolver itemResolver = TagResolver.builder()
                    .resolver(MessageUtil.placeholder("rank", rank.getAndIncrement()))
                    .resolver(MessageUtil.placeholder("player_name", playerName))
                    .resolver(MessageUtil.placeholder("balance", formattedBalance))
                    .build();

            ItemStack item = createItem(playerItemConfig, itemResolver);
            if (item.getItemMeta() instanceof SkullMeta) {
                SkullMeta meta = (SkullMeta) item.getItemMeta();
                meta.setOwningPlayer(offlinePlayer);
                item.setItemMeta(meta);
            }
            inventory.setItem(i, item);
        }

        // Navigation and page info
        TagResolver pageInfoResolver = TagResolver.builder()
                .resolver(MessageUtil.placeholder("page", page + 1))
                .resolver(MessageUtil.placeholder("max_page", maxPage))
                .build();

        if (page > 0) {
            inventory.setItem(guiConfig.getInt("previous-page.slot", 45), createItem(guiConfig.getConfigurationSection("previous-page"), pageInfoResolver));
        }
        if (page < maxPage - 1) {
            inventory.setItem(guiConfig.getInt("next-page.slot", 53), createItem(guiConfig.getConfigurationSection("next-page"), pageInfoResolver));
        }
        inventory.setItem(guiConfig.getInt("page-info.slot", 49), createItem(guiConfig.getConfigurationSection("page-info"), pageInfoResolver));

        fill(guiConfig);
        player.openInventory(inventory);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        ConfigurationSection guiConfig = plugin.getGuiConfig().getConfigurationSection("leaderboard-menu");
        if (guiConfig == null) return;

        int clickedSlot = event.getSlot();

        if (clickedSlot == guiConfig.getInt("previous-page.slot", 45)) {
            if (page > 0) {
                new LeaderboardGui(plugin, player, page - 1).open();
            }
        } else if (clickedSlot == guiConfig.getInt("next-page.slot", 53)) {
            // Recalculate max page before moving to next
             plugin.getService().getAccountCount().thenAccept(totalAccounts -> {
                int maxPage = (int) Math.ceil((double) totalAccounts / ENTRIES_PER_PAGE);
                if (page < maxPage - 1) {
                    plugin.getServer().getScheduler().runTask(plugin, () -> new LeaderboardGui(plugin, player, page + 1).open());
                }
            });
        }
    }
}
