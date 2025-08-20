package com.minekarta.kec.service;

import com.minekarta.kec.KartaEmeraldCurrencyPlugin;
import com.minekarta.kec.api.CurrencyFormatter;
import com.minekarta.kec.api.KartaEmeraldService;
import com.minekarta.kec.api.TransferReason;
import com.minekarta.kec.api.event.BankDepositEvent;
import com.minekarta.kec.api.event.BankWithdrawEvent;
import com.minekarta.kec.api.event.CurrencyBalanceChangeEvent;
import com.minekarta.kec.api.event.CurrencyTransferEvent;
import com.minekarta.kec.storage.EconomyDataHandler;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * The implementation of the KartaEmeraldService.
 */
public class KartaEmeraldServiceImpl implements KartaEmeraldService {

    private final KartaEmeraldCurrencyPlugin plugin;
    private final EconomyDataHandler economyDataHandler;
    private final CurrencyFormatter formatter = new Formatter();

    /**
     * Constructs a new KartaEmeraldServiceImpl.
     * @param plugin The plugin instance.
     * @param economyDataHandler The economy data handler.
     */
    public KartaEmeraldServiceImpl(KartaEmeraldCurrencyPlugin plugin, EconomyDataHandler economyDataHandler) {
        this.plugin = plugin;
        this.economyDataHandler = economyDataHandler;
    }

    @Override
    public CompletableFuture<Boolean> hasAccount(@NotNull UUID playerId) {
        return economyDataHandler.hasAccount(playerId);
    }

    @Override
    public CompletableFuture<Void> createAccount(@NotNull UUID playerId) {
        long startingBalance = plugin.getPluginConfig().getLong("currency.starting-bank-balance", 0);
        return economyDataHandler.createAccount(playerId, startingBalance);
    }

    @Override
    public CompletableFuture<Long> getBankBalance(@NotNull UUID playerId) {
        return economyDataHandler.getBalance(playerId);
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

        CompletableFuture<Long> syncPart = new CompletableFuture<>();
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                BankDepositEvent event = new BankDepositEvent(player, amount);
                Bukkit.getPluginManager().callEvent(event);
                if (event.isCancelled()) {
                    syncPart.completeExceptionally(new RuntimeException("Deposit event was cancelled."));
                    return;
                }

                long finalAmount = event.getAmount();
                Material currencyMaterial = Material.valueOf(plugin.getPluginConfig().getString("currency.material", "EMERALD"));
                if (getWalletBalance(player) < finalAmount) {
                    syncPart.completeExceptionally(new RuntimeException("Player does not have enough items in wallet."));
                    return;
                }

                player.getInventory().removeItem(new ItemStack(currencyMaterial, (int) finalAmount));
                syncPart.complete(finalAmount);
            } catch (Exception e) {
                syncPart.completeExceptionally(e);
            }
        });

        return syncPart.thenCompose(finalAmount -> {
            return economyDataHandler.addBalance(playerId, finalAmount).thenApply(newBalance -> {
                long oldBalance = newBalance - finalAmount;
                Bukkit.getScheduler().runTask(plugin, () -> {
                    CurrencyBalanceChangeEvent changeEvent = new CurrencyBalanceChangeEvent(true, playerId, CurrencyBalanceChangeEvent.ChangeReason.DEPOSIT, oldBalance, newBalance);
                    Bukkit.getPluginManager().callEvent(changeEvent);
                });
                return true;
            });
        }).exceptionally(e -> {
            plugin.getLogger().warning("Failed to deposit currency for " + playerId + ": " + e.getMessage());
            return false;
        });
    }

    @Override
    public CompletableFuture<Boolean> withdrawFromBank(@NotNull UUID playerId, long amount) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null || !player.isOnline()) {
            return CompletableFuture.completedFuture(false);
        }

        return getBankBalance(playerId).thenCompose(balance -> {
            if (balance < amount) {
                return CompletableFuture.completedFuture(false);
            }

            CompletableFuture<Long> syncPart = new CompletableFuture<>();
            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    BankWithdrawEvent event = new BankWithdrawEvent(player, amount);
                    Bukkit.getPluginManager().callEvent(event);
                    if (event.isCancelled()) {
                        syncPart.completeExceptionally(new RuntimeException("Withdraw event was cancelled."));
                        return;
                    }

                    long finalAmount = event.getAmount();
                    if (balance < finalAmount) {
                        syncPart.completeExceptionally(new RuntimeException("Insufficient funds after event modification."));
                        return;
                    }

                    if (player.getInventory().firstEmpty() == -1) {
                        syncPart.completeExceptionally(new RuntimeException("Player inventory is full."));
                        return;
                    }
                    syncPart.complete(finalAmount);
                } catch (Exception e) {
                    syncPart.completeExceptionally(e);
                }
            });

            return syncPart.thenCompose(finalAmount -> {
                return economyDataHandler.removeBalance(playerId, finalAmount).thenApply(newBalance -> {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        Material currencyMaterial = Material.valueOf(plugin.getPluginConfig().getString("currency.material", "EMERALD"));
                        player.getInventory().addItem(new ItemStack(currencyMaterial, (int) (long)finalAmount));

                        long oldBalance = balance;
                        CurrencyBalanceChangeEvent changeEvent = new CurrencyBalanceChangeEvent(true, playerId, CurrencyBalanceChangeEvent.ChangeReason.WITHDRAW, oldBalance, newBalance);
                        Bukkit.getPluginManager().callEvent(changeEvent);
                    });
                    return true;
                });
            });
        }).exceptionally(e -> {
            plugin.getLogger().warning("Failed to withdraw currency for " + playerId + ": " + e.getMessage());
            return false;
        });
    }

    @Override
    public CompletableFuture<Boolean> transfer(@NotNull UUID from, @NotNull UUID to, long amount, @Nullable TransferReason reason) {
        return getBankBalance(from).thenCompose(fromBalance -> {
            if (fromBalance < amount) {
                return CompletableFuture.completedFuture(false);
            }

            CurrencyTransferEvent event = new CurrencyTransferEvent(true, from, to, reason, amount, 0);
            Bukkit.getPluginManager().callEvent(event);
            if(event.isCancelled()) {
                return CompletableFuture.completedFuture(false);
            }

            long finalAmount = event.getAmount();

            return economyDataHandler.performTransfer(from, to, finalAmount, event.getFee());
        });
    }

    @Override
    public CompletableFuture<Boolean> setBankBalance(@NotNull UUID playerId, long amount) {
        return economyDataHandler.setBalance(playerId, amount).thenApply(v -> true);
    }

    @Override
    public CompletableFuture<Boolean> addBankBalance(@NotNull UUID playerId, long delta) {
        return economyDataHandler.addBalance(playerId, delta).thenApply(newBalance -> true);
    }

    @Override
    public CompletableFuture<Boolean> removeBankBalance(@NotNull UUID playerId, long delta) {
        return economyDataHandler.removeBalance(playerId, delta).thenApply(newBalance -> true);
    }

    @Override
    @NotNull
    public CurrencyFormatter getFormatter() {
        return formatter;
    }

    @Override
    public CompletableFuture<Map<UUID, Long>> getTopBalances(int limit, int offset) {
        return economyDataHandler.getTopBalances(limit, offset);
    }

    @Override
    public CompletableFuture<Integer> getAccountCount() {
        return economyDataHandler.getAccountCount();
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
            return formatWithCommas(amount);
        }
    }
}
