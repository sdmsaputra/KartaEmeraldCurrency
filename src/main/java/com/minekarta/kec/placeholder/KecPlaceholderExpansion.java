package com.minekarta.kec.placeholder;

import com.minekarta.kec.KartaEmeraldCurrencyPlugin;
import com.minekarta.kec.api.CurrencyFormatter;
import com.minekarta.kec.api.KartaEmeraldService;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The PlaceholderAPI expansion for KartaEmeraldCurrency.
 */
public class KecPlaceholderExpansion extends PlaceholderExpansion {

    private final KartaEmeraldCurrencyPlugin plugin;
    private final KartaEmeraldService service;
    private final CurrencyFormatter formatter;

    private final String placeholderSource;
    private final boolean compactFormatting;

    private static final Pattern LEADERBOARD_PATTERN = Pattern.compile("top_(\\d+)_(\\w+)");

    /**
     * Constructs a new KecPlaceholderExpansion.
     * @param plugin The plugin instance.
     */
    public KecPlaceholderExpansion(KartaEmeraldCurrencyPlugin plugin) {
        this.plugin = plugin;
        this.service = plugin.getService();
        this.formatter = service.getFormatter();
        this.placeholderSource = plugin.getConfig().getString("placeholders.source", "BANK").toUpperCase();
        this.compactFormatting = plugin.getConfig().getBoolean("placeholders.compact", false);
    }

    @Override
    public @NotNull String getIdentifier() {
        return "kartaemerald";
    }

    @Override
    public @NotNull String getAuthor() {
        return "MinekartaStudio";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) {
            return "";
        }

        // Balance placeholders
        switch (params) {
            case "balance":
                return String.valueOf(getBalance(player).join());
            case "balance_formatted":
                return formatBalance(getBalance(player).join());
            case "balance_bank":
                return String.valueOf(service.getBankBalance(player.getUniqueId()).join());
            case "balance_bank_formatted":
                return formatter.formatWithCommas(service.getBankBalance(player.getUniqueId()).join());
            case "balance_wallet":
                return String.valueOf(service.getWalletBalance(player));
            case "balance_wallet_formatted":
                return formatter.formatWithCommas(service.getWalletBalance(player));
            case "balance_total":
                return String.valueOf(getTotalBalance(player).join());
            case "balance_total_formatted":
                return formatter.formatWithCommas(getTotalBalance(player).join());
        }

        // Leaderboard placeholders
        Matcher matcher = LEADERBOARD_PATTERN.matcher(params);
        if (matcher.matches()) {
            try {
                int rank = Integer.parseInt(matcher.group(1));
                if (rank <= 0) return "Invalid Rank";

                String type = matcher.group(2);

                // Fetch top balances - we fetch one more than needed in case of rank lookup
                Map<UUID, Long> topBalances = service.getTopBalances(rank, 0).join();
                if (topBalances.size() < rank) {
                    return ""; // or a default value like "N/A"
                }

                // Convert map to list to get by index
                List<Map.Entry<UUID, Long>> entries = new ArrayList<>(topBalances.entrySet());
                Map.Entry<UUID, Long> entry = entries.get(rank - 1);

                switch (type) {
                    case "name":
                        OfflinePlayer topPlayer = Bukkit.getOfflinePlayer(entry.getKey());
                        return topPlayer.getName() != null ? topPlayer.getName() : "Unknown";
                    case "balance":
                        return String.valueOf(entry.getValue());
                    case "balance_formatted":
                        return formatter.formatWithCommas(entry.getValue());
                    default:
                        return "Invalid Type";
                }

            } catch (NumberFormatException e) {
                return "Invalid Rank Number";
            } catch (IndexOutOfBoundsException e) {
                return ""; // Rank doesn't exist
            }
        }

        return null;
    }

    private CompletableFuture<Long> getBalance(OfflinePlayer player) {
        if ("TOTAL".equals(placeholderSource)) {
            return getTotalBalance(player);
        }
        return service.getBankBalance(player.getUniqueId());
    }

    private CompletableFuture<Long> getTotalBalance(OfflinePlayer player) {
        return service.getBankBalance(player.getUniqueId()).thenApply(bank -> bank + service.getWalletBalance(player));
    }

    private String formatBalance(long balance) {
        if (compactFormatting) {
            return formatter.formatDefault(balance);
        }
        return formatter.formatWithCommas(balance);
    }
}
