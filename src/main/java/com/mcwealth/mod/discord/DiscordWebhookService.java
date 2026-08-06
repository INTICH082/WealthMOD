package com.mcwealth.mod.discord;

import com.google.gson.Gson;
import com.mcwealth.mod.MinecraftWealthMod;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class DiscordWebhookService {

    private static final Gson GSON = new Gson();
    private static final int COLOR_GOLD = 0xFFD700;

    private final DiscordWebhookConfig config;
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public DiscordWebhookService(DiscordWebhookConfig config) {
        this.config = config;
    }

    public void announceNewRichest(String playerName, double wealth) {
        if (!config.isUsable() || !config.notifyOnNewRichest()) {
            return;
        }
        send(buildEmbedJson(
                "\uD83D\uDC51 New richest player!",
                playerName + " is now #1 with **$" + format(wealth) + "**",
                COLOR_GOLD));
    }

    public boolean sendTestMessage() {
        if (!config.isUsable()) {
            return false;
        }
        send(buildEmbedJson(
                "\u2705 Minecraft Wealth",
                "This is a test message. Your Discord webhook is configured correctly.",
                COLOR_GOLD));
        return true;
    }

    private void send(String jsonBody) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.webhookUrl()))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
        client.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                .exceptionally(ex -> {
                    MinecraftWealthMod.LOGGER.warn("Failed to deliver Discord webhook", ex);
                    return null;
                });
    }

    static String buildEmbedJson(String title, String description, int color) {
        Map<String, Object> embed = Map.of(
                "title", title,
                "description", description,
                "color", color);
        Map<String, Object> payload = Map.of("embeds", List.of(embed));
        return GSON.toJson(payload);
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%,.2f", value);
    }
}