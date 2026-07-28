package com.mcwealth.mod.schedule;

import com.mcwealth.mod.advancement.ModAdvancements;
import com.mcwealth.mod.economy.WealthCache;
import com.mcwealth.mod.economy.WealthCalculator;
import com.mcwealth.mod.economy.WealthResult;
import com.mcwealth.mod.network.WealthHudPayload;
import com.mcwealth.mod.storage.LeaderboardService;
import com.mcwealth.mod.storage.WealthHistoryService;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AutoUpdateService {

    private static final int REFRESH_INTERVAL_TICKS = 600;
    private static final int PLAYERS_PER_TICK = 1;
    private static final int HISTORY_INTERVAL_TICKS = 6000;

    private final WealthCalculator calculator;
    private final LeaderboardService leaderboard;
    private final WealthHistoryService history;
    private final WealthCache cache;
    private final Map<UUID, WealthResult> lastResults = new ConcurrentHashMap<>();
    private final Deque<ServerPlayerEntity> queue = new ArrayDeque<>();
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

        if (queue.isEmpty() && tickCounter % REFRESH_INTERVAL_TICKS == 0) {
            queue.addAll(server.getPlayerManager().getPlayerList());
        }

        for (int i = 0; i < PLAYERS_PER_TICK && !queue.isEmpty(); i++) {
            ServerPlayerEntity player = queue.poll();
            if (player == null || player.isRemoved()) {
                continue;
            }
            WealthResult result = calculator.calculate(player);
            cache.put(result);
            leaderboard.update(result.playerId(), result.playerName(), result.total());
            lastResults.put(result.playerId(), result);
            ModAdvancements.WEALTH_MILESTONE.trigger(player, result.total());

            if (ServerPlayNetworking.canSend(player, WealthHudPayload.ID)) {
                ServerPlayNetworking.send(player, new WealthHudPayload(result.total()));
            }
        }

        if (tickCounter % HISTORY_INTERVAL_TICKS == 0) {
            for (WealthResult result : lastResults.values()) {
                int rank = leaderboard.rankOf(result.playerId());
                history.record(result, rank);
            }
        }
    }

    public WealthResult getLastResult(UUID playerId) {
        return lastResults.get(playerId);
    }
}