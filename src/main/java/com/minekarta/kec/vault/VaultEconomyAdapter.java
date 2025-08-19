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

    // A utility to convert our service's boolean future to a Vault EconomyResponse
    private EconomyResponse handleFutureResponse(boolean success, double amount, String failureMessage) {
        if (success) {
            return new EconomyResponse(amount, getBalance(Bukkit.getOfflinePlayer(failureMessage)), EconomyResponse.ResponseType.SUCCESS, null);
        } else {
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, failureMessage);
        }
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
        return handleFutureResponse(success, amount, "Insufficient funds.");
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, double amount) {
        if (amount < 0) {
            return new EconomyResponse(0, getBalance(player), EconomyResponse.ResponseType.FAILURE, "Cannot deposit negative funds.");
        }
        boolean success = service.addBankBalance(player.getUniqueId(), (long) amount).join();
        return handleFutureResponse(success, amount, "Failed to deposit.");
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player) {
        service.createAccount(player.getUniqueId()).join();
        return true;
    }

    // Unsupported Operations
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
    public boolean hasAccount(OfflinePlayer player, String worldName) { return hasAccount(player); }
    @Override
    public double getBalance(OfflinePlayer player, String world) { return getBalance(player); }
    @Override
    public boolean has(OfflinePlayer player, String worldName, double amount) { return has(player, amount); }
    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, String worldName, double amount) { return withdrawPlayer(player, amount); }
    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, String worldName, double amount) { return depositPlayer(player, amount); }
    @Override
    public boolean createPlayerAccount(OfflinePlayer player, String worldName) { return createPlayerAccount(player); }
}
