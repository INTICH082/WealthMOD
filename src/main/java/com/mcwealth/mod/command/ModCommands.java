package com.mcwealth.mod.command;

import com.google.gson.Gson;
import com.mcwealth.mod.MinecraftWealthMod;
import com.mcwealth.mod.advancement.ModAdvancements;
import com.mcwealth.mod.economy.WealthCategory;
import com.mcwealth.mod.economy.WealthResult;
import com.mcwealth.mod.network.ChartData;
import com.mcwealth.mod.network.ForbesData;
import com.mcwealth.mod.network.ForbesPayload;
import com.mcwealth.mod.network.WealthChartsPayload;
import com.mcwealth.mod.storage.HistoryPoint;
import com.mcwealth.mod.storage.LeaderboardEntry;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
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
                                .executes(ctx -> sendCharts(ctx.getSource(), EntityArgumentType.getPlayer(ctx, "player")))))
                .then(CommandManager.literal("pay")
                        .then(CommandManager.argument("player", StringArgumentType.word())
                                .then(CommandManager.argument("amount", DoubleArgumentType.doubleArg(0.01))
                                        .executes(ModCommands::pay))))
                .then(CommandManager.literal("economy")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(CommandManager.literal("give")
                                .then(CommandManager.argument("player", EntityArgumentType.player())
                                        .then(CommandManager.argument("amount", DoubleArgumentType.doubleArg(0))
                                                .executes(ctx -> economyGive(ctx.getSource(),
                                                        EntityArgumentType.getPlayer(ctx, "player"),
                                                        DoubleArgumentType.getDouble(ctx, "amount"))))))
                        .then(CommandManager.literal("take")
                                .then(CommandManager.argument("player", EntityArgumentType.player())
                                        .then(CommandManager.argument("amount", DoubleArgumentType.doubleArg(0))
                                                .executes(ctx -> economyTake(ctx.getSource(),
                                                        EntityArgumentType.getPlayer(ctx, "player"),
                                                        DoubleArgumentType.getDouble(ctx, "amount"))))))
                        .then(CommandManager.literal("set")
                                .then(CommandManager.argument("player", EntityArgumentType.player())
                                        .then(CommandManager.argument("amount", DoubleArgumentType.doubleArg(0))
                                                .executes(ctx -> economySet(ctx.getSource(),
                                                        EntityArgumentType.getPlayer(ctx, "player"),
                                                        DoubleArgumentType.getDouble(ctx, "amount"))))))));

        dispatcher.register(CommandManager.literal("forbes")
                .executes(ctx -> showLeaderboard(ctx.getSource(), DEFAULT_FORBES_SIZE))
                .then(CommandManager.argument("count", IntegerArgumentType.integer(1, 100))
                        .executes(ctx -> showLeaderboard(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "count")))));
    }

    private static int showWealth(ServerCommandSource source, ServerPlayerEntity target) {
        WealthResult result = MinecraftWealthMod.getInstance().wealthCache().getOrCompute(target);
        MinecraftWealthMod.getInstance().leaderboard().update(result.playerId(), result.playerName(), result.total());
        ModAdvancements.WEALTH_MILESTONE.trigger(target, result.total());

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
            String json = MinecraftWealthMod.getInstance().buildPriceTableJson();
            for (ServerPlayerEntity online : source.getServer().getPlayerManager().getPlayerList()) {
                if (ServerPlayNetworking.canSend(online, com.mcwealth.mod.network.PriceTablePayload.ID)) {
                    ServerPlayNetworking.send(online, new com.mcwealth.mod.network.PriceTablePayload(json));
                }
            }
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

        WealthResult result = MinecraftWealthMod.getInstance().wealthCache().getOrCompute(target);
        MinecraftWealthMod.getInstance().leaderboard().update(result.playerId(), result.playerName(), result.total());
        ModAdvancements.WEALTH_MILESTONE.trigger(target, result.total());
        int rank = MinecraftWealthMod.getInstance().leaderboard().rankOf(target.getUuid());
        MinecraftWealthMod.getInstance().history().record(result, rank);

        Map<String, Double> byCategory = new LinkedHashMap<>();
        for (WealthCategory category : WealthCategory.values()) {
            byCategory.put(category.name(), result.byCategory().getOrDefault(category, 0.0D));
        }

        List<ChartData.ItemEntry> topItems = result.topItems().stream()
                .map(item -> new ChartData.ItemEntry(item.itemId(), item.value()))
                .collect(Collectors.toList());

        List<HistoryPoint> historyPoints = MinecraftWealthMod.getInstance().history().get(target.getUuid());
        List<ChartData.HistoryEntry> history = historyPoints.stream()
                .map(p -> new ChartData.HistoryEntry(p.timestampMillis(), p.total(), p.rank()))
                .collect(Collectors.toList());

        ChartData data = new ChartData(result.playerName(), result.total(), byCategory, topItems, history);
        ServerPlayNetworking.send(requester, new WealthChartsPayload(GSON.toJson(data)));
        return 1;
    }

    private static int pay(com.mojang.brigadier.context.CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource source = ctx.getSource();
        if (!(source.getEntity() instanceof ServerPlayerEntity sender)) {
            source.sendError(Text.translatable("command.minecraftwealth.pay.players_only"));
            return 0;
        }
        String targetName = StringArgumentType.getString(ctx, "player");
        double amount = DoubleArgumentType.getDouble(ctx, "amount");

        ServerPlayerEntity target = source.getServer().getPlayerManager().getPlayer(targetName);
        if (target == null) {
            source.sendError(Text.translatable("command.minecraftwealth.pay.not_found", targetName));
            return 0;
        }
        if (target.getUuid().equals(sender.getUuid())) {
            source.sendError(Text.translatable("command.minecraftwealth.pay.self"));
            return 0;
        }

        boolean ok = MinecraftWealthMod.getInstance().economy().transfer(sender.getUuid(), target.getUuid(), amount);
        if (!ok) {
            source.sendError(Text.translatable("command.minecraftwealth.pay.insufficient"));
            return 0;
        }
        source.sendFeedback(() -> Text.translatable("command.minecraftwealth.pay.success", format(amount), target.getGameProfile().getName()), false);
        target.sendMessage(Text.translatable("command.minecraftwealth.pay.received", format(amount), sender.getGameProfile().getName()));
        return 1;
    }

    private static int economyGive(ServerCommandSource source, ServerPlayerEntity target, double amount) {
        double newBalance = MinecraftWealthMod.getInstance().economy().deposit(target.getUuid(), amount);
        source.sendFeedback(() -> Text.translatable("command.minecraftwealth.economy.give", format(amount), target.getGameProfile().getName(), format(newBalance)), true);
        return 1;
    }

    private static int economyTake(ServerCommandSource source, ServerPlayerEntity target, double amount) {
        boolean ok = MinecraftWealthMod.getInstance().economy().withdraw(target.getUuid(), amount);
        if (!ok) {
            source.sendError(Text.translatable("command.minecraftwealth.economy.insufficient", target.getGameProfile().getName()));
            return 0;
        }
        source.sendFeedback(() -> Text.translatable("command.minecraftwealth.economy.take", format(amount), target.getGameProfile().getName()), true);
        return 1;
    }

    private static int economySet(ServerCommandSource source, ServerPlayerEntity target, double amount) {
        MinecraftWealthMod.getInstance().economy().setBalance(target.getUuid(), amount);
        source.sendFeedback(() -> Text.translatable("command.minecraftwealth.economy.set", target.getGameProfile().getName(), format(amount)), true);
        return 1;
    }

    private static String format(double value) {
        return "$" + String.format(Locale.ROOT, "%,.2f", value);
    }
}