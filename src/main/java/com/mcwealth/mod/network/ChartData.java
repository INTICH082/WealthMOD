package com.mcwealth.mod.network;

import java.util.List;
import java.util.Map;

public record ChartData(String playerName, double total, Map<String, Double> byCategory, List<ItemEntry> topItems, List<HistoryEntry> history) {

    public record ItemEntry(String itemId, double value) {
    }

    public record HistoryEntry(long timestampMillis, double total) {
    }
}