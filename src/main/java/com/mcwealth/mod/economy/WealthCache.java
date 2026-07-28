package com.mcwealth.mod.economy;

import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class WealthCache {

    private static final long TTL_MILLIS = 3000L;

    private record Entry(WealthResult result, long computedAtMillis) {
    }

    private final WealthCalculator calculator;
    private final Map<UUID, Entry> cache = new ConcurrentHashMap<>();

    public WealthCache(WealthCalculator calculator) {
        this.calculator = calculator;
    }

    public WealthResult getOrCompute(ServerPlayerEntity player) {
        Entry cached = cache.get(player.getUuid());
        long now = System.currentTimeMillis();
        if (cached != null && now - cached.computedAtMillis() < TTL_MILLIS) {
            return cached.result();
        }
        WealthResult result = calculator.calculate(player);
        cache.put(player.getUuid(), new Entry(result, now));
        return result;
    }

    public void put(WealthResult result) {
        cache.put(result.playerId(), new Entry(result, System.currentTimeMillis()));
    }

    public void invalidate(UUID playerId) {
        cache.remove(playerId);
    }
}