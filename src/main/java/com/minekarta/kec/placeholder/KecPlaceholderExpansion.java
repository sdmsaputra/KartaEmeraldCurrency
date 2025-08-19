package com.minekarta.kec.placeholder;

import com.minekarta.kec.KartaEmeraldCurrencyPlugin;
import com.minekarta.kec.api.CurrencyFormatter;
import com.minekarta.kec.api.KartaEmeraldService;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
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

    private final Cache<UUID, Long> balanceCache;
    private final Cache<String, Map<UUID, Long>> leaderboardCache;

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

        this.balanceCache = CacheBuilder.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .build();

        this.leaderboardCache = CacheBuilder.newBuilder()
                .maximumSize(2)
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .build();
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

        // Leaderboard placeholders
        Matcher matcher = LEADERBOARD_PATTERN.matcher(params);
        if (matcher.matches()) {
            return handleLeaderboardPlaceholder(matcher);
        }

        // Balance placeholders
        Long balance = balanceCache.getIfPresent(player.getUniqueId());

        // If balance is not in cache, request it and return a default value
        if (balance == null) {
            // Request the balance asynchronously and populate the cache
            getBalance(player).thenAccept(b -> balanceCache.put(player.getUniqueId(), b));
            // Return a default value while the balance is being fetched
            return "0";
        }

        switch (params) {
            case "balance":
                return String.valueOf(balance);
            case "balance_formatted":
                return formatBalance(balance);
            case "balance_bank":
                // This will now use the cached value based on 'placeholderSource'
                return String.valueOf(balance);
            case "balance_bank_formatted":
                return formatter.formatWithCommas(balance);
            case "balance_wallet":
                // Wallet balance is synchronous and fast, no need to cache
                return String.valueOf(service.getWalletBalance(player));
            case "balance_wallet_formatted":
                return formatter.formatWithCommas(service.getWalletBalance(player));
            case "balance_total":
                // This will now use the cached value based on 'placeholderSource'
                return String.valueOf(balance);
            case "balance_total_formatted":
                return formatter.formatWithCommas(balance);
        }

        return null;
    }

    private String handleLeaderboardPlaceholder(Matcher matcher) {
        try {
            int rank = Integer.parseInt(matcher.group(1));
            if (rank <= 0) return "Invalid Rank";

            String type = matcher.group(2);
            String leaderboardKey = "TOTAL".equals(placeholderSource) ? "TOTAL" : "BANK";

            Map<UUID, Long> topBalances = leaderboardCache.getIfPresent(leaderboardKey);

            if (topBalances == null) {
                // Fetch asynchronously and populate cache
                service.getTopBalances(50, 0).thenAccept(balances -> leaderboardCache.put(leaderboardKey, balances));
                return ""; // Return empty while loading
            }

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
