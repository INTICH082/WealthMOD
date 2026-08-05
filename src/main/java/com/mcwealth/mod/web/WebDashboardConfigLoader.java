package com.mcwealth.mod.web;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mcwealth.mod.MinecraftWealthMod;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class WebDashboardConfigLoader {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private WebDashboardConfigLoader() {
    }

    public static WebDashboardConfig load(Path configDir) {
        Path file = configDir.resolve("web.json");
        try {
            if (!Files.exists(file)) {
                WebDashboardConfig defaults = WebDashboardConfig.defaults();
                Files.createDirectories(configDir);
                try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                    GSON.toJson(defaults, writer);
                }
                return defaults;
            }
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                WebDashboardConfig loaded = GSON.fromJson(reader, WebDashboardConfig.class);
                return loaded != null ? loaded : WebDashboardConfig.defaults();
            }
        } catch (IOException e) {
            MinecraftWealthMod.LOGGER.error("Failed to load web.json, web dashboard stays disabled", e);
            return WebDashboardConfig.defaults();
        }
    }
}