package com.minekarta.kec.storage;

import com.minekarta.kec.storage.provider.PlayerData;
import com.minekarta.kec.storage.provider.StorageProvider;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class DefaultEconomyDataHandler implements EconomyDataHandler {

    private final StorageManager storageManager;
    private final Executor asyncExecutor;
    private final Map<UUID, Lock> userLocks = new ConcurrentHashMap<>();

    public DefaultEconomyDataHandler(StorageManager storageManager, Executor asyncExecutor) {
        this.storageManager = storageManager;
        this.asyncExecutor = asyncExecutor;
    }

    private StorageProvider provider() {
        return storageManager.getProvider();
    }

    private <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, asyncExecutor);
    }

    private CompletableFuture<Void> runAsync(Runnable runnable) {
        return CompletableFuture.runAsync(runnable, asyncExecutor);
    }

    @Override
    public void initialize() {
        // Initialization is handled by the StorageManager
    }

    @Override
    public void close() {
        // Shutdown is handled by the StorageManager
    }

    @Override
    public CompletableFuture<Long> getBalance(@NotNull UUID uuid) {
        return supplyAsync(() -> provider().getPlayerData(uuid)
                .map(PlayerData::getBalance)
                .orElse(0L));
    }

    @Override
    public CompletableFuture<Void> setBalance(@NotNull UUID uuid, long balance) {
        return runAsync(() -> {
            Lock lock = userLocks.computeIfAbsent(uuid, k -> new ReentrantLock());
            lock.lock();
            try {
                provider().savePlayerData(uuid, new PlayerData(balance));
            } finally {
                lock.unlock();
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> hasAccount(@NotNull UUID uuid) {
        return supplyAsync(() -> provider().getPlayerData(uuid).isPresent());
    }

    @Override
    public CompletableFuture<Void> createAccount(@NotNull UUID uuid, long startingBalance) {
        return setBalance(uuid, startingBalance);
    }

    @Override
    public CompletableFuture<Long> addBalance(@NotNull UUID uuid, long amount) {
        return supplyAsync(() -> {
            Lock lock = userLocks.computeIfAbsent(uuid, k -> new ReentrantLock());
            lock.lock();
            try {
                long currentBalance = provider().getPlayerData(uuid).map(PlayerData::getBalance).orElse(0L);
                long newBalance = currentBalance + amount;
                provider().savePlayerData(uuid, new PlayerData(newBalance));
                return newBalance;
            } finally {
                lock.unlock();
            }
        });
    }



    @Override
    public CompletableFuture<Long> removeBalance(@NotNull UUID uuid, long amount) {
        return supplyAsync(() -> {
            Lock lock = userLocks.computeIfAbsent(uuid, k -> new ReentrantLock());
            lock.lock();
            try {
                long currentBalance = provider().getPlayerData(uuid).map(PlayerData::getBalance).orElse(0L);
                long newBalance = Math.max(0, currentBalance - amount);
                provider().savePlayerData(uuid, new PlayerData(newBalance));
                return newBalance;
            } finally {
                lock.unlock();
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> performTransfer(@NotNull UUID from, @NotNull UUID to, long amount, long fee) {
        return supplyAsync(() -> {
            long totalDeduction = amount + fee;
            UUID first = from.compareTo(to) < 0 ? from : to;
            UUID second = from.compareTo(to) < 0 ? to : from;

            Lock lock1 = userLocks.computeIfAbsent(first, k -> new ReentrantLock());
            Lock lock2 = userLocks.computeIfAbsent(second, k -> new ReentrantLock());

            lock1.lock();
            lock2.lock();
            try {
                PlayerData fromData = provider().getPlayerData(from).orElse(new PlayerData(0));

                if (fromData.getBalance() < totalDeduction) {
                    return false; // Insufficient funds
                }

                PlayerData toData = provider().getPlayerData(to).orElse(new PlayerData(0));

                fromData.setBalance(fromData.getBalance() - totalDeduction);
                toData.setBalance(toData.getBalance() + amount);

                provider().savePlayerData(from, fromData);
                provider().savePlayerData(to, toData);

                return true;
            } finally {
                lock2.unlock();
                lock1.unlock();
            }
        });
    }

    @Override
    public CompletableFuture<Map<UUID, Long>> getTopBalances(int limit, int offset) {
        return supplyAsync(() -> {
            Map<UUID, PlayerData> allData = provider().getAllPlayerData();
            return allData.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue(Comparator.comparingLong(PlayerData::getBalance).reversed()))
                    .skip(offset)
                    .limit(limit)
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> entry.getValue().getBalance(),
                            (e1, e2) -> e1,
                            LinkedHashMap::new
                    ));
        });
    }

    @Override
    public CompletableFuture<Integer> getAccountCount() {
        return supplyAsync(() -> provider().getAllPlayerData().size());
    }
}
