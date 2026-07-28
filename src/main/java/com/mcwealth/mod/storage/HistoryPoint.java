package com.mcwealth.mod.storage;

public record HistoryPoint(long timestampMillis, double total, double inventory, double enderChest, double equipment, double hand, int rank) {
}