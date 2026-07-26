package com.mcwealth.mod.api;

import com.mcwealth.mod.economy.WealthResult;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.network.ServerPlayerEntity;

public interface WealthChangeCallback {

    Event<WealthChangeCallback> EVENT = EventFactory.createArrayBacked(WealthChangeCallback.class,
            listeners -> (player, result) -> {
                for (WealthChangeCallback listener : listeners) {
                    listener.onWealthComputed(player, result);
                }
            });

    void onWealthComputed(ServerPlayerEntity player, WealthResult result);
}