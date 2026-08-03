package com.mcwealth.mod.gui;

import com.mcwealth.mod.network.CompareData;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Locale;
import java.util.Map;

public final class CompareScreen extends Screen {

    private static final int CHART_RES_W = 480;
    private static final int CHART_RES_H = 260;

    private final CompareData data;
    private ChartTextureFactory.RenderedChart chart;

    public CompareScreen(CompareData data) {
        super(Text.literal(data.selfName() + " vs " + data.otherName()));
        this.data = data;
    }

    @Override
    protected void init() {
        if (data.otherOnline() && hasNonZero(data.otherByCategory())) {
            chart = ChartTextureFactory.compareChart(
                    data.selfName(), data.selfByCategory(),
                    data.otherName(), data.otherByCategory(),
                    key -> Text.translatable("text.minecraftwealth.category." + key.toLowerCase(Locale.ROOT)).getString(),
                    CHART_RES_W, CHART_RES_H);
        }

        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), b -> close())
                .dimensions(width / 2 - 50, height - 28, 100, 20)
                .build());
    }

    private static boolean hasNonZero(Map<String, Double> map) {
        return map.values().stream().anyMatch(v -> v > 0);
    }

    @Override
    public void removed() {
        if (chart != null) {
            chart.close();
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 12, 0xFFFFFF);

        double diff = data.selfTotal() - data.otherTotal();
        String diffText;
        int diffColor;
        if (diff > 0) {
            diffText = Text.translatable("text.minecraftwealth.compare.richer", format(diff)).getString();
            diffColor = Formatting.GREEN.getColorValue();
        } else if (diff < 0) {
            diffText = Text.translatable("text.minecraftwealth.compare.poorer", format(-diff)).getString();
            diffColor = Formatting.RED.getColorValue();
        } else {
            diffText = Text.translatable("text.minecraftwealth.compare.equal").getString();
            diffColor = 0xFFFFFF;
        }

        int summaryY = 26;
        String selfLine = data.selfName() + ": $" + format(data.selfTotal());
        String otherLine = data.otherName() + ": $" + format(data.otherTotal())
                + (data.otherOnline() ? "" : " " + Text.translatable("text.minecraftwealth.compare.offline").getString());
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(selfLine), width / 2, summaryY, 0x55FF55);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(otherLine), width / 2, summaryY + 11, 0xFF7777);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(diffText), width / 2, summaryY + 26, diffColor);

        int panelTop = summaryY + 46;
        int panelHeight = height - panelTop - 40;
        int margin = 20;
        int panelWidth = width - margin * 2;
        context.fill(margin, panelTop, margin + panelWidth, panelTop + panelHeight, 0x55000000);

        if (chart != null) {
            int imgX = margin + 4;
            int imgY = panelTop + 4;
            int imgW = panelWidth - 8;
            int imgH = panelHeight - 8;
            context.drawTexture(chart.textureId(), imgX, imgY, imgW, imgH, 0, 0, chart.width(), chart.height(), chart.width(), chart.height());
        } else {
            context.drawCenteredTextWithShadow(textRenderer, Text.translatable("text.minecraftwealth.compare.no_breakdown"),
                    width / 2, panelTop + panelHeight / 2, 0xAAAAAA);
        }
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%,.2f", value);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}