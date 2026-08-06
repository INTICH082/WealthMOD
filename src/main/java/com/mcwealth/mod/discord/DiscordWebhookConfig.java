package com.mcwealth.mod.discord;

public record DiscordWebhookConfig(boolean enabled, String webhookUrl, boolean notifyOnNewRichest) {

    public static DiscordWebhookConfig defaults() {
        return new DiscordWebhookConfig(false, "", true);
    }

    public boolean isUsable() {
        return enabled && webhookUrl != null && !webhookUrl.isBlank();
    }
}