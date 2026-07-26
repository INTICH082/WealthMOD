package com.mcwealth.mod.config;

import com.mcwealth.mod.MinecraftWealthMod;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.text.Text;

public final class WealthModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            ConfigBuilder builder = ConfigBuilder.create()
                    .setParentScreen(parent)
                    .setTitle(Text.translatable("modmenu.minecraftwealth.name"));

            ConfigCategory general = builder.getOrCreateCategory(Text.translatable("category.minecraftwealth.commands"));
            ConfigEntryBuilder entry = builder.entryBuilder();

            double currentDefault = MinecraftWealthMod.getInstance().priceRegistry().getDefaultPrice();

            general.addEntry(entry.startDoubleField(Text.translatable("modmenu.minecraftwealth.name"), currentDefault)
                    .setDefaultValue(0.0D)
                    .setTooltip(Text.of("Fallback price used for any item not listed in prices.json"))
                    .setSaveConsumer(value -> {
                        MinecraftWealthMod.getInstance().priceRegistry()
                                .load(MinecraftWealthMod.getInstance().priceRegistry().snapshot(), value);
                        try {
                            MinecraftWealthMod.getInstance().configManager().save();
                        } catch (java.io.IOException e) {
                            MinecraftWealthMod.LOGGER.error("Failed to persist config from Mod Menu screen", e);
                        }
                    })
                    .build());

            builder.setSavingRunnable(() -> {  });

            return builder.build();
        };
    }
}