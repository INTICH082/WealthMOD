package com.mcwealth.mod;

import com.mcwealth.mod.hud.WealthHud;
import com.mcwealth.mod.hud.WealthHudState;
import com.mcwealth.mod.network.ModNetworkingClient;
import com.mcwealth.mod.price.PriceTooltip;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public final class MinecraftWealthModClient implements ClientModInitializer {

    private static KeyBinding toggleHudKey;

    @Override
    public void onInitializeClient() {
        ModNetworkingClient.register();
        WealthHud.register();
        PriceTooltip.register();

        toggleHudKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.minecraftwealth.toggle_hud",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                "key.category.minecraftwealth"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleHudKey.wasPressed()) {
                WealthHudState.toggle();
            }
        });
    }
}