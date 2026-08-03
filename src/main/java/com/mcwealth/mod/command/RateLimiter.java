package com.mcwealth.mod.command;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RateLimiter {

    private final Map<String, Long> lastUse = new ConcurrentHashMap<>();

    public boolean tryAcquire(UUID playerId, String key, long cooldownMillis) {
        String mapKey = playerId + ":" + key;
        long now = System.currentTimeMillis();
        Long previous = lastUse.get(mapKey);
        if (previous != null && now - previous < cooldownMillis) {
            return false;
        }
        lastUse.put(mapKey, now);
        return true;
    }

    public long remainingMillis(UUID playerId, String key, long cooldownMillis) {
        String mapKey = playerId + ":" + key;
        Long previous = lastUse.get(mapKey);
        if (previous == null) {
            return 0;
        }
        long elapsed = System.currentTimeMillis() - previous;
        return Math.max(0, cooldownMillis - elapsed);
    }
}