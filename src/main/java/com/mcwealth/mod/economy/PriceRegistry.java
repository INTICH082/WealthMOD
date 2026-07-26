package com.mcwealth.mod.economy;

import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class PriceRegistry {

    private final Map<Identifier, Double> prices = new ConcurrentHashMap<>();
    private volatile double defaultPrice = 0.0D;

    public void load(Map<Identifier, Double> newPrices, double defaultPrice) {
        this.prices.clear();
        this.prices.putAll(newPrices);
        this.defaultPrice = defaultPrice;
    }

    public void setPrice(Identifier itemId, double price) {
        prices.put(itemId, price);
    }

    public void setPrice(Item item, double price) {
        setPrice(Registries.ITEM.getId(item), price);
    }

    public double getPrice(Identifier itemId) {
        return prices.getOrDefault(itemId, defaultPrice);
    }

    public double getPrice(Item item) {
        if (item == net.minecraft.item.Items.AIR) {
            return 0.0D;
        }
        return getPrice(Registries.ITEM.getId(item));
    }

    public boolean hasExplicitPrice(Identifier itemId) {
        return prices.containsKey(itemId);
    }

    public int size() {
        return prices.size();
    }

    public double getDefaultPrice() {
        return defaultPrice;
    }

    public Map<Identifier, Double> snapshot() {
        return Map.copyOf(prices);
    }
}