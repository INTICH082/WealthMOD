package com.mcwealth.mod.economy;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public record WealthResult(UUID playerId, String playerName, double total, Map<WealthCategory, Double> byCategory,
                            List<ItemValue> topItems) {

    public record ItemValue(String itemId, double value) {
    }

    public static Builder builder(UUID playerId, String playerName) {
        return new Builder(playerId, playerName);
    }

    public static final class Builder {
        private static final int TOP_ITEMS_LIMIT = 10;

        private final UUID playerId;
        private final String playerName;
        private final Map<WealthCategory, Double> byCategory = new EnumMap<>(WealthCategory.class);
        private final Map<String, Double> itemTotals = new HashMap<>();

        private Builder(UUID playerId, String playerName) {
            this.playerId = playerId;
            this.playerName = playerName;
            for (WealthCategory category : WealthCategory.values()) {
                byCategory.put(category, 0.0D);
            }
        }

        public Builder add(WealthCategory category, double value) {
            byCategory.merge(category, value, Double::sum);
            return this;
        }

        public Builder addItem(String itemId, double value) {
            itemTotals.merge(itemId, value, Double::sum);
            return this;
        }

        public WealthResult build() {
            double total = byCategory.values().stream().mapToDouble(Double::doubleValue).sum();
            List<ItemValue> topItems = itemTotals.entrySet().stream()
                    .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                    .limit(TOP_ITEMS_LIMIT)
                    .map(e -> new ItemValue(e.getKey(), e.getValue()))
                    .collect(Collectors.toList());
            return new WealthResult(playerId, playerName, total, Map.copyOf(byCategory), topItems);
        }
    }
}