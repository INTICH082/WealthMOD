package com.mcwealth.mod.api;

import com.mcwealth.mod.MinecraftWealthMod;
import com.mcwealth.mod.economy.WealthResult;
import com.mcwealth.mod.storage.LeaderboardEntry;
import net.minecraft.item.Item;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class WealthAPI {

    private WealthAPI() {
    }

    public static WealthResult getWealth(ServerPlayerEntity player) {
        WealthResult result = MinecraftWealthMod.getInstance().wealthCalculator().calculate(player);
        MinecraftWealthMod.getInstance().leaderboard().update(result.playerId(), result.playerName(), result.total());
        WealthChangeCallback.EVENT.invoker().onWealthComputed(player, result);
        return result;
    }

    public static Optional<Double> getCachedWealth(UUID playerId) {
        LeaderboardEntry entry = MinecraftWealthMod.getInstance().leaderboard().get(playerId);
        return entry == null ? Optional.empty() : Optional.of(entry.wealth());
    }

    public static void registerPrice(Item item, double price) {
        MinecraftWealthMod.getInstance().priceRegistry().setPrice(item, price);
    }

    public static void registerPrice(Identifier itemId, double price) {
        MinecraftWealthMod.getInstance().priceRegistry().setPrice(itemId, price);
    }

    public static double getPrice(Item item) {
        return MinecraftWealthMod.getInstance().priceRegistry().getPrice(item);
    }

    public static List<LeaderboardEntry> getLeaderboard(int count) {
        return MinecraftWealthMod.getInstance().leaderboard().top(count);
    }
}