package com.mcwealth.mod.economy;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;

public final class InventoryFingerprint {

    private static final int MAX_DEPTH = 8;

    private InventoryFingerprint() {
    }

    public static long compute(ServerPlayerEntity player) {
        long hash = 1125899906842597L;
        PlayerInventory inventory = player.getInventory();

        hash = hashList(hash, inventory.main);
        hash = hashList(hash, inventory.armor);
        hash = hashList(hash, inventory.offHand);

        Inventory enderChest = player.getEnderChestInventory();
        for (int slot = 0; slot < enderChest.size(); slot++) {
            hash = hashStack(hash, enderChest.getStack(slot), 0);
        }

        return hash;
    }

    private static long hashList(long hash, Iterable<ItemStack> stacks) {
        for (ItemStack stack : stacks) {
            hash = hashStack(hash, stack, 0);
        }
        return hash;
    }

    private static long hashStack(long hash, ItemStack stack, int depth) {
        if (stack == null || stack.isEmpty()) {
            return hash * 31;
        }
        hash = hash * 31 + Registries.ITEM.getRawId(stack.getItem());
        hash = hash * 31 + stack.getCount();

        if (depth < MAX_DEPTH) {
            ContainerComponent container = stack.get(DataComponentTypes.CONTAINER);
            if (container != null) {
                for (ItemStack inner : container.iterateNonEmpty()) {
                    hash = hashStack(hash, inner, depth + 1);
                }
            }
        }
        return hash;
    }
}