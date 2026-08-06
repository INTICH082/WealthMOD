package com.mcwealth.mod.discord;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mcwealth.mod.MinecraftWealthMod;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class DiscordWebhookConfigLoader {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private DiscordWebhookConfigLoader() {
    }

    public static DiscordWebhookConfig load(Path configDir) {
        Path file = configDir.resolve("discord.json");
        try {
            if (!Files.exists(file)) {
                seedDefaultFile(file);
                return DiscordWebhookConfig.defaults();
            }
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                JsonObject root = GSON.fromJson(reader, JsonObject.class);
                if (root == null) {
                    return DiscordWebhookConfig.defaults();
                }
                boolean enabled = root.has("enabled") && root.get("enabled").getAsBoolean();
                String url = root.has("webhookUrl") ? root.get("webhookUrl").getAsString() : "";
                boolean notifyOnNewRichest = !root.has("notifyOnNewRichest") || root.get("notifyOnNewRichest").getAsBoolean();
                return new DiscordWebhookConfig(enabled, url, notifyOnNewRichest);
            }
        } catch (IOException e) {
            MinecraftWealthMod.LOGGER.error("Failed to load discord.json, Discord webhook stays disabled", e);
            return DiscordWebhookConfig.defaults();
        }
    }

    private static void seedDefaultFile(Path file) throws IOException {
        JsonObject root = new JsonObject();
        root.addProperty("_comment", "Create a webhook in your Discord channel settings (Integrations -> Webhooks), "
                + "paste its URL below, and set enabled to true. Restart the server or re-run /wealth discord test to verify.");
        root.addProperty("enabled", false);
        root.addProperty("webhookUrl", "");
        root.addProperty("notifyOnNewRichest", true);
        Files.createDirectories(file.getParent());
        try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            GSON.toJson(root, writer);
        }
    }
}