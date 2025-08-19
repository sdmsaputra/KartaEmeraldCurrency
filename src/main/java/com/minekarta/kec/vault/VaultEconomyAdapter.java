package com.minekarta.kec.vault;

import com.minekarta.kec.api.KartaEmeraldService;
import net.milkbowl.vault.economy.AbstractEconomy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.Collections;
import java.util.List;

public class VaultEconomyAdapter extends AbstractEconomy {

    private final KartaEmeraldService service;

    public VaultEconomyAdapter(KartaEmeraldService service) {
        this.service = service;
    }


    @Override
    public boolean isEnabled() {
        return service != null;
    }

    @Override
    public String getName() {
        return "KartaEmeraldCurrency";
    }

    @Override
    public boolean hasBankSupport() {
        return false; // We are the bank
    }

    @Override
    public int fractionalDigits() {
        // Read from config later
        return 0;
    }

    @Override
    public String format(double amount) {
        return service.getFormatter().formatWithCommas((long) amount);
    }

    @Override
    public String currencyNamePlural() {
        return "Emeralds";
    }

    @Override
    public String currencyNameSingular() {
        return "Emerald";
    }

    @Override
    public boolean hasAccount(OfflinePlayer player) {
        return service.hasAccount(player.getUniqueId()).join();
    }

    @Override
    public double getBalance(OfflinePlayer player) {
        return service.getBankBalance(player.getUniqueId()).join();
    }

    @Override
    public boolean has(OfflinePlayer player, double amount) {
        return getBalance(player) >= amount;
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, double amount) {
        if (amount < 0) {
            return new EconomyResponse(0, getBalance(player), EconomyResponse.ResponseType.FAILURE, "Cannot withdraw negative funds.");
        }
        boolean success = service.removeBankBalance(player.getUniqueId(), (long) amount).join();
        if (success) {
            return new EconomyResponse(amount, getBalance(player), EconomyResponse.ResponseType.SUCCESS, null);
        } else {
            return new EconomyResponse(0, getBalance(player), EconomyResponse.ResponseType.FAILURE, "Insufficient funds.");
        }
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, double amount) {
        if (amount < 0) {
            return new EconomyResponse(0, getBalance(player), EconomyResponse.ResponseType.FAILURE, "Cannot deposit negative funds.");
        }
        boolean success = service.addBankBalance(player.getUniqueId(), (long) amount).join();
        if (success) {
            return new EconomyResponse(amount, getBalance(player), EconomyResponse.ResponseType.SUCCESS, null);
        } else {
            return new EconomyResponse(0, getBalance(player), EconomyResponse.ResponseType.FAILURE, "Failed to deposit.");
        }
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player) {
        service.createAccount(player.getUniqueId()).join();
        return true;
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean hasAccount(String playerName) {
        return hasAccount(Bukkit.getOfflinePlayer(playerName));
    }

    @SuppressWarnings("deprecation")
    @Override
    public double getBalance(String playerName) {
        return getBalance(Bukkit.getOfflinePlayer(playerName));
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean has(String playerName, double amount) {
        return has(Bukkit.getOfflinePlayer(playerName), amount);
    }

    @SuppressWarnings("deprecation")
    @Override
    public EconomyResponse withdrawPlayer(String playerName, double amount) {
        return withdrawPlayer(Bukkit.getOfflinePlayer(playerName), amount);
    }

    @SuppressWarnings("deprecation")
    @Override
    public EconomyResponse depositPlayer(String playerName, double amount) {
        return depositPlayer(Bukkit.getOfflinePlayer(playerName), amount);
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean createPlayerAccount(String playerName) {
        return createPlayerAccount(Bukkit.getOfflinePlayer(playerName));
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean hasAccount(String playerName, String worldName) {
        return hasAccount(Bukkit.getOfflinePlayer(playerName));
    }

    @SuppressWarnings("deprecation")
    @Override
    public double getBalance(String playerName, String world) {
        return getBalance(Bukkit.getOfflinePlayer(playerName));
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean has(String playerName, String worldName, double amount) {
        return has(Bukkit.getOfflinePlayer(playerName), amount);
    }

    @SuppressWarnings("deprecation")
    @Override
    public EconomyResponse withdrawPlayer(String playerName, String worldName, double amount) {
        return withdrawPlayer(Bukkit.getOfflinePlayer(playerName), amount);
    }

    @SuppressWarnings("deprecation")
    @Override
    public EconomyResponse depositPlayer(String playerName, String worldName, double amount) {
        return depositPlayer(Bukkit.getOfflinePlayer(playerName), amount);
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean createPlayerAccount(String playerName, String worldName) {
        return createPlayerAccount(Bukkit.getOfflinePlayer(playerName));
    }

    @SuppressWarnings("deprecation")
    @Override
    public EconomyResponse isBankOwner(String name, String playerName) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "KEC does not support multiple banks.");
    }

    @SuppressWarnings("deprecation")
    @Override
    public EconomyResponse isBankMember(String name, String playerName) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "KEC does not support multiple banks.");
    }

    @SuppressWarnings("deprecation")
    @Override
    public EconomyResponse createBank(String name, String player) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "KEC does not support multiple banks.");
    }

    // Unsupported Bank Operations
    @Override
    public EconomyResponse createBank(String name, OfflinePlayer player) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "KEC does not support multiple banks.");
    }
    @Override
    public EconomyResponse deleteBank(String name) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "KEC does not support multiple banks.");
    }
    @Override
    public EconomyResponse bankBalance(String name) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "KEC does not support multiple banks.");
    }
    @Override
    public EconomyResponse bankHas(String name, double amount) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "KEC does not support multiple banks.");
    }
    @Override
    public EconomyResponse bankWithdraw(String name, double amount) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "KEC does not support multiple banks.");
    }
    @Override
    public EconomyResponse bankDeposit(String name, double amount) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "KEC does not support multiple banks.");
    }
    @Override
    public EconomyResponse isBankOwner(String name, OfflinePlayer player) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "KEC does not support multiple banks.");
    }
    @Override
    public EconomyResponse isBankMember(String name, OfflinePlayer player) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "KEC does not support multiple banks.");
    }
    @Override
    public List<String> getBanks() {
        return Collections.emptyList();
    }
    @Override
    public boolean hasAccount(OfflinePlayer player, String worldName) {
        return hasAccount(player);
    }
    @Override
    public double getBalance(OfflinePlayer player, String world) {
        return getBalance(player);
    }
    @Override
    public boolean has(OfflinePlayer player, String worldName, double amount) {
        return has(player, amount);
    }
    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, String worldName, double amount) {
        return withdrawPlayer(player, amount);
    }
    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, String worldName, double amount) {
        return depositPlayer(player, amount);
    }
    @Override
    public boolean createPlayerAccount(OfflinePlayer player, String worldName) {
        return createPlayerAccount(player);
    }
}
