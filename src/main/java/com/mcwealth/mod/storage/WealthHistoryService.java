package com.mcwealth.mod.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mcwealth.mod.MinecraftWealthMod;
import com.mcwealth.mod.economy.WealthCategory;
import com.mcwealth.mod.economy.WealthResult;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class WealthHistoryService {

    private static final int MAX_POINTS_PER_PLAYER = 200;
    private static final Gson GSON = new GsonBuilder().create();
    private static final Type MAP_TYPE = new TypeToken<Map<UUID, List<HistoryPoint>>>() {}.getType();

    private final Path file;
    private final Map<UUID, List<HistoryPoint>> history = new ConcurrentHashMap<>();
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "minecraftwealth-history-io");
        t.setDaemon(true);
        return t;
    });
    private final AtomicBoolean dirty = new AtomicBoolean(false);
    private final AtomicBoolean saveScheduled = new AtomicBoolean(false);

    public WealthHistoryService(Path configDir) {
        this.file = configDir.resolve("history.json");
    }

    public void load() {
        if (!Files.exists(file)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            Map<UUID, List<HistoryPoint>> loaded = GSON.fromJson(reader, MAP_TYPE);
            if (loaded != null) {
                history.putAll(loaded);
            }
        } catch (IOException e) {
            MinecraftWealthMod.LOGGER.error("Failed to load history.json", e);
        }
    }

    public synchronized void record(WealthResult result) {
        List<HistoryPoint> points = history.computeIfAbsent(result.playerId(), id -> new ArrayList<>());
        Map<WealthCategory, Double> byCategory = result.byCategory();
        points.add(new HistoryPoint(
                System.currentTimeMillis(),
                result.total(),
                byCategory.getOrDefault(WealthCategory.INVENTORY, 0.0D),
                byCategory.getOrDefault(WealthCategory.ENDER_CHEST, 0.0D),
                byCategory.getOrDefault(WealthCategory.EQUIPMENT, 0.0D),
                byCategory.getOrDefault(WealthCategory.HAND, 0.0D)));
        while (points.size() > MAX_POINTS_PER_PLAYER) {
            points.remove(0);
        }
        dirty.set(true);
        scheduleSave();
    }

    public List<HistoryPoint> get(UUID playerId) {
        return history.getOrDefault(playerId, List.of());
    }

    private void scheduleSave() {
        if (saveScheduled.compareAndSet(false, true)) {
            ioExecutor.execute(() -> {
                try {
                    TimeUnit.SECONDS.sleep(5);
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
                GSON.toJson(history, MAP_TYPE, writer);
            }
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            MinecraftWealthMod.LOGGER.error("Failed to save history.json", e);
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