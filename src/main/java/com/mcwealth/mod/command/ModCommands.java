package com.mcwealth.mod.command;

import com.google.gson.Gson;
import com.mcwealth.mod.MinecraftWealthMod;
import com.mcwealth.mod.economy.WealthCategory;
import com.mcwealth.mod.economy.WealthResult;
import com.mcwealth.mod.network.ChartData;
import com.mcwealth.mod.network.ForbesData;
import com.mcwealth.mod.network.ForbesPayload;
import com.mcwealth.mod.network.WealthChartsPayload;
import com.mcwealth.mod.storage.HistoryPoint;
import com.mcwealth.mod.storage.LeaderboardEntry;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public final class ModCommands {

    private static final int DEFAULT_FORBES_SIZE = 50;
    private static final Gson GSON = new Gson();

    private ModCommands() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> registerCommands(dispatcher));
    }

    private static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("wealth")
                .executes(ctx -> showWealth(ctx.getSource(), ctx.getSource().getPlayerOrThrow()))
                .then(CommandManager.argument("player", EntityArgumentType.player())
                        .executes(ctx -> showWealth(ctx.getSource(), EntityArgumentType.getPlayer(ctx, "player"))))
                .then(CommandManager.literal("reload")
                        .requires(source -> source.hasPermissionLevel(2))
                        .executes(ModCommands::reload))
                .then(CommandManager.literal("graph")
                        .executes(ctx -> sendCharts(ctx.getSource(), ctx.getSource().getPlayerOrThrow()))
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .executes(ctx -> sendCharts(ctx.getSource(), EntityArgumentType.getPlayer(ctx, "player"))))));

        dispatcher.register(CommandManager.literal("forbes")
                .executes(ctx -> showLeaderboard(ctx.getSource(), DEFAULT_FORBES_SIZE))
                .then(CommandManager.argument("count", IntegerArgumentType.integer(1, 100))
                        .executes(ctx -> showLeaderboard(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "count")))));
    }

    private static int showWealth(ServerCommandSource source, ServerPlayerEntity target) {
        WealthResult result = MinecraftWealthMod.getInstance().wealthCalculator().calculate(target);
        MinecraftWealthMod.getInstance().leaderboard().update(result.playerId(), result.playerName(), result.total());

        boolean self = source.getEntity() instanceof ServerPlayerEntity requester && requester.getUuid().equals(target.getUuid());
        String headerKey = self ? "command.minecraftwealth.wealth.self" : "command.minecraftwealth.wealth.other";
        Object[] headerArgs = self
                ? new Object[]{format(result.total())}
                : new Object[]{target.getGameProfile().getName(), format(result.total())};
        source.sendFeedback(() -> Text.translatable(headerKey, headerArgs), false);

        source.sendFeedback(() -> Text.translatable("command.minecraftwealth.wealth.breakdown.header"), false);
        for (WealthCategory category : WealthCategory.values()) {
            double value = result.byCategory().getOrDefault(category, 0.0D);
            if (value <= 0.0D) {
                continue;
            }
            source.sendFeedback(() -> Text.translatable("command.minecraftwealth.wealth.breakdown.line",
                    Text.translatable(category.translationKey()), format(value)), false);
        }
        return (int) Math.min(Integer.MAX_VALUE, result.total());
    }

    private static int showLeaderboard(ServerCommandSource source, int count) {
        List<LeaderboardEntry> top = MinecraftWealthMod.getInstance().leaderboard().top(count);
        if (top.isEmpty()) {
            source.sendFeedback(() -> Text.translatable("command.minecraftwealth.forbes.empty"), false);
            return 0;
        }

        if (source.getEntity() instanceof ServerPlayerEntity player && ServerPlayNetworking.canSend(player, ForbesPayload.ID)) {
            List<ForbesData.Entry> entries = new ArrayList<>(top.size());
            for (int i = 0; i < top.size(); i++) {
                LeaderboardEntry entry = top.get(i);
                entries.add(new ForbesData.Entry(i + 1, entry.playerName(), entry.wealth()));
            }
            ServerPlayNetworking.send(player, new ForbesPayload(GSON.toJson(new ForbesData(entries))));
            return top.size();
        }

        source.sendFeedback(() -> Text.translatable("command.minecraftwealth.forbes.header"), false);
        for (int i = 0; i < top.size(); i++) {
            LeaderboardEntry entry = top.get(i);
            int rank = i + 1;
            source.sendFeedback(() -> Text.translatable("command.minecraftwealth.forbes.line",
                    rank, entry.playerName(), format(entry.wealth())), false);
        }
        return top.size();
    }

    private static int reload(com.mojang.brigadier.context.CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource source = ctx.getSource();
        try {
            int count = MinecraftWealthMod.getInstance().configManager().reload();
            source.sendFeedback(() -> Text.translatable("command.minecraftwealth.reload.success", count), true);
            return count;
        } catch (IOException e) {
            MinecraftWealthMod.LOGGER.error("Failed to reload prices.json", e);
            source.sendError(Text.translatable("command.minecraftwealth.reload.error"));
            return 0;
        }
    }

    private static int sendCharts(ServerCommandSource source, ServerPlayerEntity target) {
        if (!(source.getEntity() instanceof ServerPlayerEntity requester)) {
            source.sendError(Text.translatable("command.minecraftwealth.graph.players_only"));
            return 0;
        }

        WealthResult result = MinecraftWealthMod.getInstance().wealthCalculator().calculate(target);
        MinecraftWealthMod.getInstance().leaderboard().update(result.playerId(), result.playerName(), result.total());
        MinecraftWealthMod.getInstance().history().record(result);

        Map<String, Double> byCategory = new LinkedHashMap<>();
        for (WealthCategory category : WealthCategory.values()) {
            byCategory.put(category.name(), result.byCategory().getOrDefault(category, 0.0D));
        }

        List<ChartData.ItemEntry> topItems = result.topItems().stream()
                .map(item -> new ChartData.ItemEntry(item.itemId(), item.value()))
                .collect(Collectors.toList());

        List<HistoryPoint> historyPoints = MinecraftWealthMod.getInstance().history().get(target.getUuid());
        List<ChartData.HistoryEntry> history = historyPoints.stream()
                .map(p -> new ChartData.HistoryEntry(p.timestampMillis(), p.total()))
                .collect(Collectors.toList());

        ChartData data = new ChartData(result.playerName(), result.total(), byCategory, topItems, history);
        ServerPlayNetworking.send(requester, new WealthChartsPayload(GSON.toJson(data)));
        return 1;
    }

    private static String format(double value) {
        return "$" + String.format(Locale.ROOT, "%,.2f", value);
    }
}