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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public final class LeaderboardService {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type ENTRY_LIST_TYPE = new TypeToken<List<LeaderboardEntry>>() {}.getType();

    private final Path file;
    private final Map<UUID, LeaderboardEntry> entries = new ConcurrentHashMap<>();
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "minecraftwealth-leaderboard-io");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicBoolean dirty = new AtomicBoolean(false);
    private final AtomicBoolean saveScheduled = new AtomicBoolean(false);

    public LeaderboardService(Path configDir) {
        this.file = configDir.resolve("leaderboard.json");
    }

    public void load() {
        if (!Files.exists(file)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            List<LeaderboardEntry> loaded = GSON.fromJson(reader, ENTRY_LIST_TYPE);
            if (loaded != null) {
                for (LeaderboardEntry entry : loaded) {
                    entries.put(entry.playerId(), entry);
                }
            }
            MinecraftWealthMod.LOGGER.info("Loaded {} leaderboard entries", entries.size());
        } catch (IOException e) {
            MinecraftWealthMod.LOGGER.error("Failed to load leaderboard.json", e);
        }
    }

    public void update(UUID playerId, String playerName, double wealth) {
        entries.put(playerId, new LeaderboardEntry(playerId, playerName, wealth, System.currentTimeMillis()));
        dirty.set(true);
        scheduleSave();
    }

    public LeaderboardEntry get(UUID playerId) {
        return entries.get(playerId);
    }

    public List<LeaderboardEntry> top(int count) {
        return entries.values().stream()
                .sorted(Comparator.comparingDouble(LeaderboardEntry::wealth).reversed())
                .limit(Math.max(0, count))
                .collect(Collectors.toList());
    }

    public int rankOf(UUID playerId) {
        List<UUID> ordered = entries.values().stream()
                .sorted(Comparator.comparingDouble(LeaderboardEntry::wealth).reversed())
                .map(LeaderboardEntry::playerId)
                .collect(Collectors.toList());
        int index = ordered.indexOf(playerId);
        return index < 0 ? -1 : index + 1;
    }

    public int size() {
        return entries.size();
    }

    private void scheduleSave() {
        if (saveScheduled.compareAndSet(false, true)) {
            ioExecutor.execute(() -> {
                try {
                    TimeUnit.MILLISECONDS.sleep(1000);
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
            List<LeaderboardEntry> snapshot = List.copyOf(entries.values());
            try (Writer writer = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
                GSON.toJson(snapshot, ENTRY_LIST_TYPE, writer);
            }
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            MinecraftWealthMod.LOGGER.error("Failed to save leaderboard.json", e);
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