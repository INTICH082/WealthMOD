package com.mcwealth.mod.schedule;

import com.mcwealth.mod.advancement.ModAdvancements;
import com.mcwealth.mod.economy.InventoryFingerprint;
import com.mcwealth.mod.economy.WealthCache;
import com.mcwealth.mod.economy.WealthCalculator;
import com.mcwealth.mod.economy.WealthResult;
import com.mcwealth.mod.network.WealthHudPayload;
import com.mcwealth.mod.storage.LeaderboardService;
import com.mcwealth.mod.storage.WealthHistoryService;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AutoUpdateService {

    private static final int FINGERPRINT_CHECKS_PER_TICK = 4;
    private static final int HISTORY_INTERVAL_TICKS = 6000;

    private final WealthCalculator calculator;
    private final LeaderboardService leaderboard;
    private final WealthHistoryService history;
    private final WealthCache cache;

    private final Map<UUID, WealthResult> lastResults = new ConcurrentHashMap<>();
    private final Map<UUID, Long> fingerprints = new ConcurrentHashMap<>();
    private final List<ServerPlayerEntity> checkOrder = new ArrayList<>();
    private int checkCursor = 0;
    private long tickCounter = 0;

    public AutoUpdateService(WealthCalculator calculator, LeaderboardService leaderboard,
                              WealthHistoryService history, WealthCache cache) {
        this.calculator = calculator;
        this.leaderboard = leaderboard;
        this.history = history;
        this.cache = cache;
    }

    public void tick(MinecraftServer server) {
        tickCounter++;

        List<ServerPlayerEntity> online = server.getPlayerManager().getPlayerList();
        if (online.isEmpty()) {
            checkCursor = 0;
        } else {
            if (checkCursor >= online.size()) {
                checkCursor = 0;
            }

            int checked = 0;
            int start = checkCursor;
            while (checked < FINGERPRINT_CHECKS_PER_TICK && checked < online.size()) {
                ServerPlayerEntity player = online.get(checkCursor);
                checkCursor = (checkCursor + 1) % online.size();
                checked++;

                long currentFingerprint = InventoryFingerprint.compute(player);
                Long previous = fingerprints.put(player.getUuid(), currentFingerprint);
                if (previous == null || previous != currentFingerprint) {
                    recalculate(player);
                }

                if (checkCursor == start) {
                    break;
                }
            }
        }

        if (tickCounter % HISTORY_INTERVAL_TICKS == 0) {
            for (WealthResult result : lastResults.values()) {
                int rank = leaderboard.rankOf(result.playerId());
                history.record(result, rank);
            }
        }
    }

    public void recalculate(ServerPlayerEntity player) {
        WealthResult result = calculator.calculate(player);
        cache.put(result);
        leaderboard.update(result.playerId(), result.playerName(), result.total());
        lastResults.put(result.playerId(), result);
        ModAdvancements.WEALTH_MILESTONE.trigger(player, result.total());

        if (ServerPlayNetworking.canSend(player, WealthHudPayload.ID)) {
            ServerPlayNetworking.send(player, new WealthHudPayload(result.total()));
        }
    }

    public void forget(UUID playerId) {
        fingerprints.remove(playerId);
    }

    public WealthResult getLastResult(UUID playerId) {
        return lastResults.get(playerId);
    }
}