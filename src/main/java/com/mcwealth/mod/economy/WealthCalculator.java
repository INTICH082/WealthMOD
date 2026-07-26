package com.mcwealth.mod.economy;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;

public final class WealthCalculator {

    private final PriceRegistry prices;
    private final ContainerScanner containerScanner;

    public WealthCalculator(PriceRegistry prices) {
        this.prices = prices;
        this.containerScanner = new ContainerScanner(prices);
    }

    public WealthResult calculate(ServerPlayerEntity player) {
        WealthResult.Builder builder = WealthResult.builder(player.getUuid(), player.getGameProfile().getName());
        java.util.function.ObjDoubleConsumer<ItemStack> leafSink =
                (stack, value) -> builder.addItem(Registries.ITEM.getId(stack.getItem()).toString(), value);

        PlayerInventory inventory = player.getInventory();

        for (ItemStack stack : inventory.main) {
            builder.add(WealthCategory.INVENTORY, containerScanner.valueStack(stack, leafSink));
        }

        for (ItemStack stack : inventory.armor) {
            builder.add(WealthCategory.EQUIPMENT, containerScanner.valueStack(stack, leafSink));
        }

        for (ItemStack stack : inventory.offHand) {
            builder.add(WealthCategory.HAND, containerScanner.valueStack(stack, leafSink));
        }

        Inventory enderChest = player.getEnderChestInventory();
        for (int slot = 0; slot < enderChest.size(); slot++) {
            builder.add(WealthCategory.ENDER_CHEST, containerScanner.valueStack(enderChest.getStack(slot), leafSink));
        }

        return builder.build();
    }

    public PriceRegistry prices() {
        return prices;
    }
}