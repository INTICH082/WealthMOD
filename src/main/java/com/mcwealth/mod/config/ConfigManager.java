package com.mcwealth.mod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mcwealth.mod.MinecraftWealthMod;
import com.mcwealth.mod.economy.PriceRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

public final class ConfigManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String BUNDLED_DEFAULT_PATH = "/data/minecraftwealth/default_prices.json";

    private final Path configDir;
    private final Path pricesFile;
    private final PriceRegistry registry;

    public ConfigManager(PriceRegistry registry) {
        this.registry = registry;
        this.configDir = FabricLoader.getInstance().getConfigDir().resolve("minecraftwealth");
        this.pricesFile = configDir.resolve("prices.json");
    }

    public Path configDir() {
        return configDir;
    }

    public void initialize() {
        try {
            Files.createDirectories(configDir);
            if (!Files.exists(pricesFile)) {
                seedDefaultFile();
            }
            reload();
        } catch (IOException e) {
            MinecraftWealthMod.LOGGER.error("Failed to initialize Minecraft Wealth config, using bundled defaults in-memory", e);
            loadBundledDefaults();
        }
    }

    public int reload() throws IOException {
        try (Reader reader = Files.newBufferedReader(pricesFile, StandardCharsets.UTF_8)) {
            return applyJson(GSON.fromJson(reader, JsonObject.class));
        }
    }

    private void seedDefaultFile() throws IOException {
        try (InputStream in = ConfigManager.class.getResourceAsStream(BUNDLED_DEFAULT_PATH)) {
            if (in == null) {
                throw new IOException("Bundled default price list missing from jar: " + BUNDLED_DEFAULT_PATH);
            }
            Path tmp = pricesFile.resolveSibling(pricesFile.getFileName() + ".tmp");
            Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
            Files.move(tmp, pricesFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            MinecraftWealthMod.LOGGER.info("Seeded default price list at {}", pricesFile);
        }
    }

    private void loadBundledDefaults() {
        try (InputStream in = ConfigManager.class.getResourceAsStream(BUNDLED_DEFAULT_PATH);
             Reader reader = new java.io.InputStreamReader(in, StandardCharsets.UTF_8)) {
            applyJson(GSON.fromJson(reader, JsonObject.class));
        } catch (IOException e) {
            MinecraftWealthMod.LOGGER.error("Could not even load bundled default prices; wealth values will all be 0", e);
        }
    }

    private int applyJson(JsonObject root) {
        Map<Identifier, Double> parsed = new HashMap<>();
        double defaultPrice = root.has("default_price") ? root.get("default_price").getAsDouble() : 0.0D;
        JsonObject prices = root.getAsJsonObject("prices");
        if (prices != null) {
            for (Map.Entry<String, com.google.gson.JsonElement> entry : prices.entrySet()) {
                Identifier id = Identifier.tryParse(entry.getKey());
                if (id == null) {
                    MinecraftWealthMod.LOGGER.warn("Skipping invalid item id in prices.json: {}", entry.getKey());
                    continue;
                }
                parsed.put(id, entry.getValue().getAsDouble());
            }
        }
        registry.load(parsed, defaultPrice);
        MinecraftWealthMod.LOGGER.info("Loaded {} item prices (default price {})", parsed.size(), defaultPrice);
        return parsed.size();
    }

    public void save() throws IOException {
        JsonObject root = new JsonObject();
        root.addProperty("version", 1);
        root.addProperty("default_price", registry.getDefaultPrice());
        JsonObject prices = new JsonObject();
        registry.snapshot().forEach((id, price) -> prices.addProperty(id.toString(), price));
        root.add("prices", prices);
        try (Writer writer = Files.newBufferedWriter(pricesFile, StandardCharsets.UTF_8)) {
            GSON.toJson(root, writer);
        }
    }
}