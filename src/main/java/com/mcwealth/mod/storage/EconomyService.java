package com.mcwealth.mod.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mcwealth.mod.MinecraftWealthMod;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class EconomyService {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type MAP_TYPE = new TypeToken<Map<UUID, Double>>() {}.getType();

    private final Path file;
    private final Map<UUID, Double> balances = new ConcurrentHashMap<>();
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "minecraftwealth-economy-io");
        t.setDaemon(true);
        return t;
    });
    private final AtomicBoolean dirty = new AtomicBoolean(false);
    private final AtomicBoolean saveScheduled = new AtomicBoolean(false);

    public EconomyService(Path configDir) {
        this.file = configDir.resolve("economy.json");
    }

    public void load() {
        if (!Files.exists(file)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            Map<UUID, Double> loaded = GSON.fromJson(reader, MAP_TYPE);
            if (loaded != null) {
                balances.putAll(loaded);
            }
        } catch (IOException e) {
            MinecraftWealthMod.LOGGER.error("Failed to load economy.json", e);
        }
    }

    public double getBalance(UUID playerId) {
        return balances.getOrDefault(playerId, 0.0D);
    }

    public boolean has(UUID playerId, double amount) {
        return getBalance(playerId) >= amount;
    }

    public void setBalance(UUID playerId, double amount) {
        balances.put(playerId, Math.max(0.0D, amount));
        markDirty();
    }

    public double deposit(UUID playerId, double amount) {
        double newBalance = balances.merge(playerId, Math.max(0.0D, amount), Double::sum);
        markDirty();
        return newBalance;
    }

    public boolean withdraw(UUID playerId, double amount) {
        if (amount <= 0) {
            return true;
        }
        double[] result = new double[1];
        boolean[] ok = new boolean[]{false};
        balances.compute(playerId, (id, current) -> {
            double balance = current == null ? 0.0D : current;
            if (balance >= amount) {
                ok[0] = true;
                result[0] = balance - amount;
                return result[0];
            }
            return current;
        });
        if (ok[0]) {
            markDirty();
        }
        return ok[0];
    }

    public boolean transfer(UUID fromPlayer, UUID toPlayer, double amount) {
        if (amount <= 0) {
            return false;
        }
        if (!withdraw(fromPlayer, amount)) {
            return false;
        }
        deposit(toPlayer, amount);
        return true;
    }

    private void markDirty() {
        dirty.set(true);
        scheduleSave();
    }

    private void scheduleSave() {
        if (saveScheduled.compareAndSet(false, true)) {
            ioExecutor.execute(() -> {
                try {
                    TimeUnit.SECONDS.sleep(2);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                } finally {
                    saveScheduled.set(false);
                }
                if (dirty.compareAndSet(true, false)) {
                    saveNow();
                }
            });
        }
    }

    public void saveNow() {
        try {
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            try (Writer writer = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
                GSON.toJson(balances, MAP_TYPE, writer);
            }
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            MinecraftWealthMod.LOGGER.error("Failed to save economy.json", e);
        }
    }

    public void shutdown() {
        if (dirty.get()) {
            saveNow();
        }
        ioExecutor.shutdown();
        try {
            ioExecutor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}