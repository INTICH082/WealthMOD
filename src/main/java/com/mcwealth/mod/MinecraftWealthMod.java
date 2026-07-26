package com.mcwealth.mod;

import com.mcwealth.mod.command.ModCommands;
import com.mcwealth.mod.config.ConfigManager;
import com.mcwealth.mod.economy.PriceRegistry;
import com.mcwealth.mod.economy.WealthCalculator;
import com.mcwealth.mod.network.ForbesPayload;
import com.mcwealth.mod.network.WealthChartsPayload;
import com.mcwealth.mod.network.WealthHudPayload;
import com.mcwealth.mod.schedule.AutoUpdateService;
import com.mcwealth.mod.storage.LeaderboardService;
import com.mcwealth.mod.storage.WealthHistoryService;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MinecraftWealthMod implements ModInitializer {

    public static final String MOD_ID = "minecraftwealth";
    public static final Logger LOGGER = LoggerFactory.getLogger("Minecraft Wealth");

    private static MinecraftWealthMod instance;

    private final PriceRegistry priceRegistry = new PriceRegistry();
    private ConfigManager configManager;
    private WealthCalculator wealthCalculator;
    private LeaderboardService leaderboard;
    private WealthHistoryService history;
    private AutoUpdateService autoUpdateService;

    @Override
    public void onInitialize() {
        instance = this;

        configManager = new ConfigManager(priceRegistry);
        configManager.initialize();

        wealthCalculator = new WealthCalculator(priceRegistry);
        leaderboard = new LeaderboardService(configManager.configDir());
        history = new WealthHistoryService(configManager.configDir());
        autoUpdateService = new AutoUpdateService(wealthCalculator, leaderboard, history);

        PayloadTypeRegistry.playS2C().register(WealthChartsPayload.ID, WealthChartsPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ForbesPayload.ID, ForbesPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(WealthHudPayload.ID, WealthHudPayload.CODEC);

        ModCommands.register();

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            leaderboard.load();
            history.load();
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            leaderboard.shutdown();
            history.shutdown();
        });
        ServerTickEvents.END_SERVER_TICK.register(autoUpdateService::tick);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            var result = wealthCalculator.calculate(handler.getPlayer());
            leaderboard.update(result.playerId(), result.playerName(), result.total());
            if (ServerPlayNetworking.canSend(handler.getPlayer(), WealthHudPayload.ID)) {
                ServerPlayNetworking.send(handler.getPlayer(), new WealthHudPayload(result.total()));
            }
        });

        LOGGER.info("Minecraft Wealth initialized with {} priced items", priceRegistry.size());
    }

    public static MinecraftWealthMod getInstance() {
        return instance;
    }

    public PriceRegistry priceRegistry() {
        return priceRegistry;
    }

    public ConfigManager configManager() {
        return configManager;
    }

    public WealthCalculator wealthCalculator() {
        return wealthCalculator;
    }

    public LeaderboardService leaderboard() {
        return leaderboard;
    }

    public WealthHistoryService history() {
        return history;
    }

    public AutoUpdateService autoUpdateService() {
        return autoUpdateService;
    }
}