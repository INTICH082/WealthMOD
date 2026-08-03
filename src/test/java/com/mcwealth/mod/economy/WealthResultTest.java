package com.mcwealth.mod.economy;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WealthResultTest {

    @Test
    void sumsCategoriesIndependently() {
        WealthResult result = WealthResult.builder(UUID.randomUUID(), "Steve")
                .add(WealthCategory.INVENTORY, 100.0)
                .add(WealthCategory.INVENTORY, 50.0)
                .add(WealthCategory.EQUIPMENT, 25.0)
                .build();

        assertEquals(150.0, result.byCategory().get(WealthCategory.INVENTORY));
        assertEquals(25.0, result.byCategory().get(WealthCategory.EQUIPMENT));
        assertEquals(175.0, result.total());
    }

    @Test
    void zeroValueCategoriesArePresentButZero() {
        WealthResult result = WealthResult.builder(UUID.randomUUID(), "Steve")
                .add(WealthCategory.HAND, 10.0)
                .build();

        assertEquals(0.0, result.byCategory().get(WealthCategory.BANK));
        assertEquals(10.0, result.total());
    }

    @Test
    void topItemsAreLimitedToTenAndSortedDescending() {
        WealthResult.Builder builder = WealthResult.builder(UUID.randomUUID(), "Steve");
        for (int i = 0; i < 20; i++) {
            builder.addItem("minecraft:item_" + i, i * 10.0);
        }
        WealthResult result = builder.build();

        assertEquals(10, result.topItems().size());
        assertEquals("minecraft:item_19", result.topItems().get(0).itemId());
        assertTrue(result.topItems().get(0).value() > result.topItems().get(1).value());
    }

    @Test
    void repeatedItemContributionsAreAggregated() {
        WealthResult result = WealthResult.builder(UUID.randomUUID(), "Steve")
                .addItem("minecraft:diamond", 250.0)
                .addItem("minecraft:diamond", 250.0)
                .build();

        assertEquals(500.0, result.topItems().get(0).value());
    }
}