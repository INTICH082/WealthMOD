package com.mcwealth.mod.hud;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;

import java.util.Locale;

public final class WealthHud {

    private WealthHud() {
    }

    public static void register() {
        HudRenderCallback.EVENT.register((context, tickCounter) -> {
            if (!WealthHudState.isVisible() || !WealthHudState.hasData()) {
                return;
            }
            MinecraftClient client = MinecraftClient.getInstance();
            String text = "$" + String.format(Locale.ROOT, "%,.2f", WealthHudState.lastTotal());
            int x = 6;
            int y = 6;
            int textWidth = client.textRenderer.getWidth(text);
            context.fill(x - 3, y - 2, x + textWidth + 3, y + 10, 0x80000000);
            context.drawTextWithShadow(client.textRenderer, text, x, y, 0x55FF55);
        });
    }
}