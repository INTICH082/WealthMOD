package com.mcwealth.mod.economy;

public enum WealthCategory {
    INVENTORY("text.minecraftwealth.category.inventory"),
    ENDER_CHEST("text.minecraftwealth.category.ender_chest"),
    EQUIPMENT("text.minecraftwealth.category.equipment"),
    HAND("text.minecraftwealth.category.hand"),
    BANK("text.minecraftwealth.category.bank");

    private final String translationKey;

    WealthCategory(String translationKey) {
        this.translationKey = translationKey;
    }

    public String translationKey() {
        return translationKey;
    }
}