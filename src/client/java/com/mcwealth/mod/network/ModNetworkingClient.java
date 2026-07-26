package com.mcwealth.mod.network;

import com.google.gson.Gson;
import com.mcwealth.mod.gui.ForbesScreen;
import com.mcwealth.mod.gui.WealthChartsScreen;
import com.mcwealth.mod.hud.WealthHudState;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class ModNetworkingClient {

    private static final Gson GSON = new Gson();

    private ModNetworkingClient() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(WealthChartsPayload.ID, (payload, context) -> {
            ChartData data = GSON.fromJson(payload.json(), ChartData.class);
            context.client().execute(() -> context.client().setScreen(new WealthChartsScreen(data)));
        });

        ClientPlayNetworking.registerGlobalReceiver(ForbesPayload.ID, (payload, context) -> {
            ForbesData data = GSON.fromJson(payload.json(), ForbesData.class);
            context.client().execute(() -> context.client().setScreen(new ForbesScreen(data)));
        });

        ClientPlayNetworking.registerGlobalReceiver(WealthHudPayload.ID, (payload, context) ->
                context.client().execute(() -> WealthHudState.update(payload.total())));
    }
}