package com.mcwealth.mod.discord;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiscordWebhookServiceTest {

    @Test
    void embedJsonContainsTitleDescriptionAndColor() {
        String json = DiscordWebhookService.buildEmbedJson("Title", "Description", 0xFFD700);

        assertTrue(json.contains("\"title\":\"Title\""));
        assertTrue(json.contains("\"description\":\"Description\""));
        assertTrue(json.contains("\"color\":16766720"));
        assertTrue(json.contains("\"embeds\""));
    }

    @Test
    void configIsNotUsableWhenDisabledOrMissingUrl() {
        assertFalse(new DiscordWebhookConfig(false, "https://discord.com/api/webhooks/x", true).isUsable());
        assertFalse(new DiscordWebhookConfig(true, "", true).isUsable());
        assertFalse(new DiscordWebhookConfig(true, "   ", true).isUsable());
        assertTrue(new DiscordWebhookConfig(true, "https://discord.com/api/webhooks/x", true).isUsable());
    }

    @Test
    void announceNewRichestIsANoOpWhenNotUsable() {
        DiscordWebhookService service = new DiscordWebhookService(DiscordWebhookConfig.defaults());
        service.announceNewRichest("Notch", 1000.0);
    }

    @Test
    void sendTestMessageReturnsFalseWhenNotConfigured() {
        DiscordWebhookService service = new DiscordWebhookService(DiscordWebhookConfig.defaults());
        assertFalse(service.sendTestMessage());
    }
}