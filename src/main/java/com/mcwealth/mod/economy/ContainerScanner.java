package com.mcwealth.mod.economy;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.item.ItemStack;

import java.util.function.ObjDoubleConsumer;

public final class ContainerScanner {

    private static final int MAX_DEPTH = 8;

    private final PriceRegistry prices;

    public ContainerScanner(PriceRegistry prices) {
        this.prices = prices;
    }

    public double valueStack(ItemStack stack, ObjDoubleConsumer<ItemStack> leafSink) {
        return valueStack(stack, leafSink, 0);
    }

    private double valueStack(ItemStack stack, ObjDoubleConsumer<ItemStack> leafSink, int depth) {
        if (stack == null || stack.isEmpty()) {
            return 0.0D;
        }

        double unitPrice = prices.getPrice(stack.getItem());
        double total = unitPrice * stack.getCount();
        if (unitPrice != 0.0D && leafSink != null) {
            leafSink.accept(stack, total);
        }

        if (depth < MAX_DEPTH) {
            ContainerComponent container = stack.get(DataComponentTypes.CONTAINER);
            if (container != null) {
                for (ItemStack inner : container.iterateNonEmpty()) {
                    total += valueStack(inner, leafSink, depth + 1);
                }
            }
        }

        return total;
    }
}