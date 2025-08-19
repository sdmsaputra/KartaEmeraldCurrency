package com.minekarta.kec.placeholder;

import com.minekarta.kec.KartaEmeraldCurrencyPlugin;
import com.minekarta.kec.api.KartaEmeraldService;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

/**
 * The PlaceholderAPI expansion for KartaEmeraldCurrency.
 */
public class KecPlaceholderExpansion extends PlaceholderExpansion {

    private final KartaEmeraldCurrencyPlugin plugin;
    private final KartaEmeraldService service;

    /**
     * Constructs a new KecPlaceholderExpansion.
     * @param plugin The plugin instance.
     */
    public KecPlaceholderExpansion(KartaEmeraldCurrencyPlugin plugin) {
        this.plugin = plugin;
        this.service = plugin.getService();
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
        return true; // We want to keep this registered
    }

    @Override
    public boolean canRegister(){
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) {
            return "";
        }

        // Placeholders like %kartaemerald_balance%
        switch (params) {
            case "balance":
                // TODO: Add config option for source (BANK | TOTAL)
                return String.valueOf(service.getBankBalance(player.getUniqueId()).join());
            case "balance_formatted":
                long balance = service.getBankBalance(player.getUniqueId()).join();
                return service.getFormatter().formatDefault(balance);
            case "balance_comma":
                long balanceComma = service.getBankBalance(player.getUniqueId()).join();
                return service.getFormatter().formatWithCommas(balanceComma);
            case "bank":
                long bankBalance = service.getBankBalance(player.getUniqueId()).join();
                return String.valueOf(bankBalance);
            case "wallet":
                return String.valueOf(service.getWalletBalance(player));
        }

        // Leaderboard placeholders: %kartaemerald_top_1_name%, %kartaemerald_top_1_amount%
        if (params.startsWith("top_")) {
            // TODO: Implement leaderboard cache and retrieval
            return "N/A";
        }

        return null; // Let PAPI know we couldn't handle this placeholder
    }
}
