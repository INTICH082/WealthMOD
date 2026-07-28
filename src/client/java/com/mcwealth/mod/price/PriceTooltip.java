package com.mcwealth.mod.price;

import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Locale;

public final class PriceTooltip {

    private PriceTooltip() {
    }

    public static void register() {
        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
            if (stack.isEmpty()) {
                return;
            }
            String itemId = Registries.ITEM.getId(stack.getItem()).toString();
            double unitPrice = ClientPriceCache.getPrice(itemId);
            if (unitPrice <= 0) {
                return;
            }
            String text = "$" + format(unitPrice);
            if (stack.getCount() > 1) {
                text += " (x" + stack.getCount() + " = $" + format(unitPrice * stack.getCount()) + ")";
            }
            lines.add(Text.literal(text).formatted(Formatting.GREEN));
        });
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%,.2f", value);
    }
}