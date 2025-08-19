package com.minekarta.kec.service;

import com.minekarta.kec.KartaEmeraldCurrencyPlugin;
import com.minekarta.kec.api.CurrencyFormatter;
import com.minekarta.kec.api.KartaEmeraldService;
import com.minekarta.kec.api.TransferReason;
import com.minekarta.kec.api.event.BankDepositEvent;
import com.minekarta.kec.api.event.BankWithdrawEvent;
import com.minekarta.kec.api.event.CurrencyBalanceChangeEvent;
import com.minekarta.kec.api.event.CurrencyTransferEvent;
import com.minekarta.kec.storage.Storage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class KartaEmeraldServiceImpl implements KartaEmeraldService {

    private final KartaEmeraldCurrencyPlugin plugin;
    private final Storage storage;
    private final CurrencyFormatter formatter = new Formatter();

    public KartaEmeraldServiceImpl(KartaEmeraldCurrencyPlugin plugin, Storage storage) {
        this.plugin = plugin;
        this.storage = storage;
    }

    @Override
    public CompletableFuture<Boolean> hasAccount(@NotNull UUID playerId) {
        return storage.hasAccount(playerId);
    }

    @Override
    public CompletableFuture<Void> createAccount(@NotNull UUID playerId) {
        // In our storage impl, setBalance/addBalance will create an account if it doesn't exist.
        // We can just set the starting balance.
        long startingBalance = plugin.getPluginConfig().getLong("currency.starting-bank-balance", 0);
        return storage.createAccount(playerId, startingBalance);
    }

    @Override
    public CompletableFuture<Long> getBankBalance(@NotNull UUID playerId) {
        return storage.getBalance(playerId);
    }

    @Override
    public long getWalletBalance(@NotNull OfflinePlayer player) {
        if (!player.isOnline() || player.getPlayer() == null) {
            return 0;
        }
        Player onlinePlayer = player.getPlayer();
        long amount = 0;
        Material currencyMaterial = Material.valueOf(plugin.getPluginConfig().getString("currency.material", "EMERALD"));
        for (ItemStack item : onlinePlayer.getInventory().getContents()) {
            if (item != null && item.getType() == currencyMaterial) {
                amount += item.getAmount();
            }
        }
        return amount;
    }

    @Override
    public CompletableFuture<Boolean> depositToBank(@NotNull UUID playerId, long amount) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null || !player.isOnline()) {
            return CompletableFuture.completedFuture(false);
        }

        return CompletableFuture.supplyAsync(() -> {
            // Must run on main thread for inventory access
            BankDepositEvent event = new BankDepositEvent(player, amount);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return false;
            }
            long finalAmount = event.getAmount();

            Material currencyMaterial = Material.valueOf(plugin.getPluginConfig().getString("currency.material", "EMERALD"));
            long itemsToRemove = finalAmount;
            if (getWalletBalance(player) < itemsToRemove) {
                return false; // Not enough items
            }

            // Remove items
            player.getInventory().removeItem(new ItemStack(currencyMaterial, (int) itemsToRemove));

            // Add to balance
            storage.addBalance(playerId, finalAmount).join();

            // Fire balance change event
            long oldBalance = getBankBalance(playerId).join() - finalAmount;
            CurrencyBalanceChangeEvent changeEvent = new CurrencyBalanceChangeEvent(true, playerId, CurrencyBalanceChangeEvent.ChangeReason.DEPOSIT, oldBalance, oldBalance + finalAmount);
            Bukkit.getPluginManager().callEvent(changeEvent);

            return true;
        }, runnable -> Bukkit.getScheduler().runTask(plugin, runnable));
    }

    @Override
    public CompletableFuture<Boolean> withdrawFromBank(@NotNull UUID playerId, long amount) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null || !player.isOnline()) {
            return CompletableFuture.completedFuture(false);
        }

        return getBankBalance(playerId).thenApplyAsync(balance -> {
            if (balance < amount) {
                return false; // Insufficient funds
            }

            // Must run on main thread for inventory access
            BankWithdrawEvent event = new BankWithdrawEvent(player, amount);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return false;
            }
            long finalAmount = event.getAmount();

            Material currencyMaterial = Material.valueOf(plugin.getPluginConfig().getString("currency.material", "EMERALD"));
            if (player.getInventory().firstEmpty() == -1) {
                // A simple check, might not be sufficient for large amounts. A better check is needed.
                return false; // Inventory is full
            }

            // Remove from balance first
            storage.removeBalance(playerId, finalAmount).join();

            // Give items
            player.getInventory().addItem(new ItemStack(currencyMaterial, (int) finalAmount));

            // Fire balance change event
            long oldBalance = balance;
            CurrencyBalanceChangeEvent changeEvent = new CurrencyBalanceChangeEvent(true, playerId, CurrencyBalanceChangeEvent.ChangeReason.WITHDRAW, oldBalance, oldBalance - finalAmount);
            Bukkit.getPluginManager().callEvent(changeEvent);

            return true;
        }, runnable -> Bukkit.getScheduler().runTask(plugin, runnable));
    }

    @Override
    public CompletableFuture<Boolean> transfer(@NotNull UUID from, @NotNull UUID to, long amount, @Nullable TransferReason reason) {
        return getBankBalance(from).thenCompose(fromBalance -> {
            if (fromBalance < amount) {
                return CompletableFuture.completedFuture(false);
            }

            CurrencyTransferEvent event = new CurrencyTransferEvent(true, from, to, reason, amount, 0); // Assuming 0 fee for now
            Bukkit.getPluginManager().callEvent(event);
            if(event.isCancelled()) {
                return CompletableFuture.completedFuture(false);
            }

            long finalAmount = event.getAmount();

            return storage.performTransfer(from, to, finalAmount, event.getFee());
        });
    }

    @Override
    public CompletableFuture<Boolean> setBankBalance(@NotNull UUID playerId, long amount) {
        return storage.setBalance(playerId, amount).thenApply(v -> true);
    }

    @Override
    public CompletableFuture<Boolean> addBankBalance(@NotNull UUID playerId, long delta) {
        return storage.addBalance(playerId, delta).thenApply(newBalance -> true);
    }

    @Override
    public CompletableFuture<Boolean> removeBankBalance(@NotNull UUID playerId, long delta) {
        return storage.removeBalance(playerId, delta).thenApply(newBalance -> true);
    }

    @Override
    @NotNull
    public CurrencyFormatter getFormatter() {
        return formatter;
    }

    private static class Formatter implements CurrencyFormatter {
        private final NumberFormat commaFormat = NumberFormat.getNumberInstance(Locale.US);

        @Override
        public @NotNull String formatWithCommas(long amount) {
            return commaFormat.format(amount);
        }

        @Override
        public @NotNull String formatCompact(long amount) {
            if (amount < 1000) {
                return String.valueOf(amount);
            }
            int exp = (int) (Math.log(amount) / Math.log(1000));
            return String.format("%.1f%c", amount / Math.pow(1000, exp), "kMGTPE".charAt(exp-1));
        }

        @Override
        public @NotNull String formatDefault(long amount) {
            // This should read from config, but for now, we default to commas
            return formatWithCommas(amount);
        }
    }
}
