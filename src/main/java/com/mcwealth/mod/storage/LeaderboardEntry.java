package com.mcwealth.mod.storage;

import java.util.UUID;

public record LeaderboardEntry(UUID playerId, String playerName, double wealth, long updatedAtEpochMillis) {
}