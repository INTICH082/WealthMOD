package com.mcwealth.mod.price;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientPriceCache {

    private static final Map<String, Double> PRICES = new ConcurrentHashMap<>();
    private static volatile double defaultPrice = 0.0D;

    private ClientPriceCache() {
    }

    public static void update(Map<String, Double> prices, double newDefaultPrice) {
        PRICES.clear();
        PRICES.putAll(prices);
        defaultPrice = newDefaultPrice;
    }

    public static double getPrice(String itemId) {
        return PRICES.getOrDefault(itemId, defaultPrice);
    }
}