package com.mcwealth.mod.hud;

public final class WealthHudState {

    private static volatile double lastTotal = 0.0D;
    private static volatile boolean visible = false;
    private static volatile boolean hasData = false;

    private WealthHudState() {
    }

    public static void update(double total) {
        lastTotal = total;
        hasData = true;
    }

    public static double lastTotal() {
        return lastTotal;
    }

    public static boolean hasData() {
        return hasData;
    }

    public static boolean isVisible() {
        return visible;
    }

    public static void toggle() {
        visible = !visible;
    }
}