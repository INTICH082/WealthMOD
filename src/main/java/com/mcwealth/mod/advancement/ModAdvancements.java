package com.mcwealth.mod.advancement;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class ModAdvancements {

    public static final WealthMilestoneCriterion WEALTH_MILESTONE = new WealthMilestoneCriterion();

    private ModAdvancements() {
    }

    public static void register() {
        Registry.register(Registries.CRITERION, WealthMilestoneCriterion.ID, WEALTH_MILESTONE);
    }
}