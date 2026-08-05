package com.mcwealth.mod.web;

public record WebDashboardConfig(boolean enabled, int port) {

    public static WebDashboardConfig defaults() {
        return new WebDashboardConfig(false, 8642);
    }
}