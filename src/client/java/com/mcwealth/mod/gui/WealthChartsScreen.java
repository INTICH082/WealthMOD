package com.mcwealth.mod.gui;

import com.mcwealth.mod.network.ChartData;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Locale;

public final class WealthChartsScreen extends Screen {

    private static final int CHART_RES_W = 480;
    private static final int CHART_RES_H = 220;
    private static final int ICON_ROW_HEIGHT = 20;

    private final ChartData data;

    private ChartTextureFactory.RenderedChart historyChart;
    private ChartTextureFactory.RenderedChart compositionChart;

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
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 12, 0xFFFFFF);

        int margin = 20;
        int panelWidth = width - margin * 2;
        int top = 30;
        int panelHeight = (height - top - 40) / 3 - 8;

        drawTexturePanel(context, "text.minecraftwealth.chart.history", historyChart, margin, top, panelWidth, panelHeight);
        drawTexturePanel(context, "text.minecraftwealth.chart.composition", compositionChart, margin, top + panelHeight + 12, panelWidth, panelHeight);
        drawTopItemsPanel(context, margin, top + (panelHeight + 12) * 2, panelWidth, panelHeight, mouseX, mouseY);
    }

    private void drawTexturePanel(DrawContext context, String titleKey, ChartTextureFactory.RenderedChart chart,
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

    private void drawTopItemsPanel(DrawContext context, int x, int y, int w, int h, int mouseX, int mouseY) {
        context.fill(x, y, x + w, y + h, 0x55000000);
        context.drawText(textRenderer, Text.translatable("text.minecraftwealth.chart.items"), x + 4, y + 4, 0xFFFFFF, false);

        List<ChartData.ItemEntry> items = data.topItems();
        if (items.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, Text.translatable("text.minecraftwealth.chart.empty"),
                    x + w / 2, y + h / 2, 0xAAAAAA);
            return;
        }

        double max = items.stream().mapToDouble(ChartData.ItemEntry::value).max().orElse(1.0D);
        if (max <= 0) {
            max = 1.0D;
        }

        int rowX = x + 6;
        int rowY = y + 18;
        int iconSize = 16;
        int barX = rowX + iconSize + 6;
        int barMaxWidth = w - (barX - x) - 90;

        ItemStack hoveredStack = null;

        for (ChartData.ItemEntry item : items) {
            if (rowY + ICON_ROW_HEIGHT > y + h) {
                break;
            }

            ItemStack stack = resolveStack(item.itemId());
            context.drawItem(stack, rowX, rowY + 1);

            int barWidth = Math.max(1, (int) Math.round(item.value() / max * barMaxWidth));
            context.fill(barX, rowY + 2, barX + barWidth, rowY + ICON_ROW_HEIGHT - 4, 0xFF55C878);

            String valueText = "$" + format(item.value());
            context.drawText(textRenderer, Text.literal(valueText), barX + barWidth + 6, rowY + 4, 0xFFFFFF, false);

            if (mouseX >= rowX && mouseX < rowX + iconSize && mouseY >= rowY && mouseY < rowY + iconSize) {
                hoveredStack = stack;
            }

            rowY += ICON_ROW_HEIGHT;
        }

        if (hoveredStack != null) {
            context.drawItemTooltip(textRenderer, hoveredStack, mouseX, mouseY);
        }
    }

    private static ItemStack resolveStack(String itemId) {
        Identifier id = Identifier.tryParse(itemId);
        if (id == null) {
            return ItemStack.EMPTY;
        }
        Item item = Registries.ITEM.getOrEmpty(id).orElse(net.minecraft.item.Items.AIR);
        return new ItemStack(item);
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%,.1f", value);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}