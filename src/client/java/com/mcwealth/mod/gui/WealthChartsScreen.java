package com.mcwealth.mod.gui;

import com.mcwealth.mod.network.ChartData;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.Locale;

public final class WealthChartsScreen extends Screen {

    private static final int CHART_RES_W = 480;
    private static final int CHART_RES_H = 220;

    private final ChartData data;

    private ChartTextureFactory.RenderedChart historyChart;
    private ChartTextureFactory.RenderedChart compositionChart;
    private ChartTextureFactory.RenderedChart itemsChart;

    public WealthChartsScreen(ChartData data) {
        super(Text.literal(data.playerName() + " - $" + format(data.total())));
        this.data = data;
    }

    @Override
    protected void init() {
        historyChart = ChartTextureFactory.historyChart(data.history(), CHART_RES_W, CHART_RES_H);
        compositionChart = ChartTextureFactory.compositionChart(data.byCategory(),
                key -> Text.translatable("text.minecraftwealth.category." + key.toLowerCase(Locale.ROOT)).getString(),
                CHART_RES_W, CHART_RES_H);
        itemsChart = ChartTextureFactory.topItemsChart(data.topItems(), CHART_RES_W, CHART_RES_H);

        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), b -> close())
                .dimensions(width / 2 - 50, height - 28, 100, 20)
                .build());
    }

    @Override
    public void removed() {
        if (historyChart != null) {
            historyChart.close();
        }
        if (compositionChart != null) {
            compositionChart.close();
        }
        if (itemsChart != null) {
            itemsChart.close();
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 12, 0xFFFFFF);

        int margin = 20;
        int panelWidth = width - margin * 2;
        int top = 30;
        int panelHeight = (height - top - 40) / 3 - 8;

        drawPanel(context, "text.minecraftwealth.chart.history", historyChart, margin, top, panelWidth, panelHeight);
        drawPanel(context, "text.minecraftwealth.chart.composition", compositionChart, margin, top + panelHeight + 12, panelWidth, panelHeight);
        drawPanel(context, "text.minecraftwealth.chart.items", itemsChart, margin, top + (panelHeight + 12) * 2, panelWidth, panelHeight);
    }

    private void drawPanel(DrawContext context, String titleKey, ChartTextureFactory.RenderedChart chart,
                            int x, int y, int w, int h) {
        context.fill(x, y, x + w, y + h, 0x55000000);
        context.drawText(textRenderer, Text.translatable(titleKey), x + 4, y + 4, 0xFFFFFF, false);

        if (chart == null) {
            context.drawCenteredTextWithShadow(textRenderer, Text.translatable("text.minecraftwealth.chart.empty"),
                    x + w / 2, y + h / 2, 0xAAAAAA);
            return;
        }

        int imgX = x + 4;
        int imgY = y + 16;
        int imgW = w - 8;
        int imgH = h - 20;
        context.drawTexture(chart.textureId(), imgX, imgY, imgW, imgH, 0, 0, chart.width(), chart.height(), chart.width(), chart.height());
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%,.1f", value);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}