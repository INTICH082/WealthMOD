package com.mcwealth.mod.gui;

import com.mcwealth.mod.network.ChartData;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.CategoryChart;
import org.knowm.xchart.CategoryChartBuilder;
import org.knowm.xchart.PieChart;
import org.knowm.xchart.PieChartBuilder;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.XYSeries;
import org.knowm.xchart.style.markers.SeriesMarkers;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public final class ChartTextureFactory {

    static {
        System.setProperty("java.awt.headless", "true");
    }

    private static final Color BG = new Color(24, 24, 24);
    private static final Color PLOT_BG = new Color(18, 18, 18);
    private static final Color GRID = new Color(55, 55, 55);
    private static final Color FONT = new Color(230, 230, 230);
    private static final Color ACCENT = new Color(85, 200, 120);

    private static final AtomicInteger COUNTER = new AtomicInteger();

    private ChartTextureFactory() {
    }

    public record RenderedChart(Identifier textureId, NativeImageBackedTexture texture, int width, int height) {
        public void close() {
            texture.close();
        }
    }

    public static RenderedChart historyChart(List<ChartData.HistoryEntry> history, int width, int height) {
        if (history.size() < 2) {
            return null;
        }
        XYChart chart = new XYChartBuilder().width(width).height(height).build();
        styleBase(chart.getStyler());
        chart.getStyler().setLegendVisible(true);
        chart.getStyler().setLegendBackgroundColor(BG);

        double[] xData = new double[history.size()];
        double[] wealthData = new double[history.size()];
        double[] rankData = new double[history.size()];
        boolean hasRank = false;
        for (int i = 0; i < history.size(); i++) {
            xData[i] = i;
            wealthData[i] = history.get(i).total();
            int rank = history.get(i).rank();
            rankData[i] = rank > 0 ? rank : Double.NaN;
            hasRank |= rank > 0;
        }

        XYSeries wealthSeries = chart.addSeries("$", xData, wealthData);
        wealthSeries.setMarker(SeriesMarkers.CIRCLE);
        wealthSeries.setMarkerColor(ACCENT);
        wealthSeries.setLineColor(ACCENT);
        wealthSeries.setLineWidth(2f);
        wealthSeries.setXYSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Area);
        wealthSeries.setFillColor(new Color(ACCENT.getRed(), ACCENT.getGreen(), ACCENT.getBlue(), 60));
        wealthSeries.setYAxisGroup(0);

        if (hasRank) {
            Color rankColor = new Color(255, 170, 50);
            chart.setYAxisGroupTitle(1, "#");
            chart.getStyler().setYAxisGroupPosition(1, org.knowm.xchart.style.Styler.YAxisPosition.Right);
            XYSeries rankSeries = chart.addSeries("Rank", xData, rankData);
            rankSeries.setMarker(SeriesMarkers.DIAMOND);
            rankSeries.setMarkerColor(rankColor);
            rankSeries.setLineColor(rankColor);
            rankSeries.setYAxisGroup(1);
        } else {
            chart.getStyler().setLegendVisible(false);
        }

        return upload(BitmapEncoder.getBufferedImage(chart), "history");
    }

    public static RenderedChart compositionChart(Map<String, Double> byCategory, Function<String, String> labelFn,
                                                   int width, int height) {
        PieChart chart = new PieChartBuilder().width(width).height(height).build();
        chart.getStyler().setChartBackgroundColor(BG);
        chart.getStyler().setPlotBackgroundColor(BG);
        chart.getStyler().setChartFontColor(FONT);
        chart.getStyler().setLegendBackgroundColor(BG);
        chart.getStyler().setLegendVisible(true);
        chart.getStyler().setPlotBorderVisible(false);
        chart.getStyler().setAnnotationType(org.knowm.xchart.style.PieStyler.AnnotationType.LabelAndPercentage);

        boolean any = false;
        for (Map.Entry<String, Double> entry : byCategory.entrySet()) {
            if (entry.getValue() <= 0) {
                continue;
            }
            chart.addSeries(labelFn.apply(entry.getKey()), entry.getValue());
            any = true;
        }
        if (!any) {
            return null;
        }
        return upload(BitmapEncoder.getBufferedImage(chart), "composition");
    }

    public static RenderedChart topItemsChart(List<ChartData.ItemEntry> items, int width, int height) {
        if (items.isEmpty()) {
            return null;
        }
        CategoryChart chart = new CategoryChartBuilder().width(width).height(height).build();
        styleBase(chart.getStyler());
        chart.getStyler().setLegendVisible(false);
        chart.getStyler().setXAxisLabelRotation(30);
        chart.getStyler().setHasAnnotations(false);

        List<String> labels = new ArrayList<>();
        List<Double> values = new ArrayList<>();
        for (ChartData.ItemEntry item : items) {
            labels.add(item.itemId().replace("minecraft:", ""));
            values.add(item.value());
        }
        chart.addSeries("$", labels, values).setFillColor(ACCENT);

        return upload(BitmapEncoder.getBufferedImage(chart), "items");
    }

    public static RenderedChart compareChart(String selfName, Map<String, Double> selfByCategory,
                                              String otherName, Map<String, Double> otherByCategory,
                                              Function<String, String> labelFn, int width, int height) {
        CategoryChart chart = new CategoryChartBuilder().width(width).height(height).build();
        styleBase(chart.getStyler());
        chart.getStyler().setLegendVisible(true);
        chart.getStyler().setXAxisLabelRotation(20);
        chart.getStyler().setHasAnnotations(false);

        List<String> labels = new ArrayList<>();
        List<Double> selfValues = new ArrayList<>();
        List<Double> otherValues = new ArrayList<>();
        for (String key : selfByCategory.keySet()) {
            double a = selfByCategory.getOrDefault(key, 0.0D);
            double b = otherByCategory.getOrDefault(key, 0.0D);
            if (a <= 0 && b <= 0) {
                continue;
            }
            labels.add(labelFn.apply(key));
            selfValues.add(a);
            otherValues.add(b);
        }
        if (labels.isEmpty()) {
            return null;
        }

        chart.addSeries(selfName, labels, selfValues).setFillColor(ACCENT);
        chart.addSeries(otherName, labels, otherValues).setFillColor(new Color(220, 90, 90));

        return upload(BitmapEncoder.getBufferedImage(chart), "compare");
    }

    private static void styleBase(org.knowm.xchart.style.AxesChartStyler styler) {
        styler.setChartBackgroundColor(BG);
        styler.setPlotBackgroundColor(PLOT_BG);
        styler.setPlotGridLinesColor(GRID);
        styler.setChartFontColor(FONT);
        styler.setAxisTickLabelsColor(FONT);
        styler.setPlotBorderVisible(false);
        styler.setLegendBackgroundColor(BG);
    }

    private static RenderedChart upload(BufferedImage image, String key) {
        int w = image.getWidth();
        int h = image.getHeight();
        NativeImage nativeImage = new NativeImage(w, h, false);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                nativeImage.setColorArgb(x, y, image.getRGB(x, y));
            }
        }
        NativeImageBackedTexture texture = new NativeImageBackedTexture(() -> "minecraftwealth_" + key, nativeImage);
        String uniqueKey = "minecraftwealth_" + key + "_" + COUNTER.incrementAndGet();
        Identifier id = MinecraftClient.getInstance().getTextureManager().registerDynamicTexture(uniqueKey, texture);
        return new RenderedChart(id, texture, w, h);
    }
}