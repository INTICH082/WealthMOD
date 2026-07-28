package com.mcwealth.mod;

import com.mcwealth.mod.advancement.ModAdvancements;
import com.mcwealth.mod.command.ModCommands;
import com.mcwealth.mod.config.ConfigManager;
import com.mcwealth.mod.economy.PriceRegistry;
import com.mcwealth.mod.economy.WealthCache;
import com.mcwealth.mod.economy.WealthCalculator;
import com.mcwealth.mod.network.ForbesPayload;
import com.mcwealth.mod.network.PriceTableData;
import com.mcwealth.mod.network.PriceTablePayload;
import com.mcwealth.mod.network.WealthChartsPayload;
import com.mcwealth.mod.network.WealthHudPayload;
import com.mcwealth.mod.schedule.AutoUpdateService;
import com.mcwealth.mod.storage.EconomyService;
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

import java.util.Map;
import java.util.stream.Collectors;

public final class MinecraftWealthMod implements ModInitializer {

    public static final String MOD_ID = "minecraftwealth";
    public static final Logger LOGGER = LoggerFactory.getLogger("Minecraft Wealth");

    private static MinecraftWealthMod instance;

    private final PriceRegistry priceRegistry = new PriceRegistry();
    private ConfigManager configManager;
    private WealthCalculator wealthCalculator;
    private WealthCache wealthCache;
    private LeaderboardService leaderboard;
    private WealthHistoryService history;
    private EconomyService economy;
    private AutoUpdateService autoUpdateService;

    @Override
    public void onInitialize() {
        instance = this;

        configManager = new ConfigManager(priceRegistry);
        configManager.initialize();

        economy = new EconomyService(configManager.configDir());
        wealthCalculator = new WealthCalculator(priceRegistry, economy);
        wealthCache = new WealthCache(wealthCalculator);
        leaderboard = new LeaderboardService(configManager.configDir());
        history = new WealthHistoryService(configManager.configDir());
        autoUpdateService = new AutoUpdateService(wealthCalculator, leaderboard, history, wealthCache);

        ModAdvancements.register();

        PayloadTypeRegistry.playS2C().register(WealthChartsPayload.ID, WealthChartsPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ForbesPayload.ID, ForbesPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(WealthHudPayload.ID, WealthHudPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(PriceTablePayload.ID, PriceTablePayload.CODEC);

        ModCommands.register();

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            leaderboard.load();
            history.load();
            economy.load();
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            leaderboard.shutdown();
            history.shutdown();
            economy.shutdown();
        });
        ServerTickEvents.END_SERVER_TICK.register(autoUpdateService::tick);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            var result = wealthCache.getOrCompute(handler.getPlayer());
            leaderboard.update(result.playerId(), result.playerName(), result.total());
            ModAdvancements.WEALTH_MILESTONE.trigger(handler.getPlayer(), result.total());
            if (ServerPlayNetworking.canSend(handler.getPlayer(), WealthHudPayload.ID)) {
                ServerPlayNetworking.send(handler.getPlayer(), new WealthHudPayload(result.total()));
            }
            if (ServerPlayNetworking.canSend(handler.getPlayer(), PriceTablePayload.ID)) {
                ServerPlayNetworking.send(handler.getPlayer(), new PriceTablePayload(buildPriceTableJson()));
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

    public WealthCache wealthCache() {
        return wealthCache;
    }

    public LeaderboardService leaderboard() {
        return leaderboard;
    }

    public WealthHistoryService history() {
        return history;
    }

    public EconomyService economy() {
        return economy;
    }

    public AutoUpdateService autoUpdateService() {
        return autoUpdateService;
    }

    public String buildPriceTableJson() {
        Map<String, Double> stringKeyed = priceRegistry.snapshot().entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey().toString(), Map.Entry::getValue));
        PriceTableData data = new PriceTableData(stringKeyed, priceRegistry.getDefaultPrice());
        return new com.google.gson.Gson().toJson(data);
    }
}