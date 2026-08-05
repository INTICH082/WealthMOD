package com.mcwealth.mod.web;

import com.mcwealth.mod.storage.LeaderboardService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebDashboardServerTest {

    private WebDashboardServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    void servesLeaderboardAsJson(@TempDir Path tempDir) throws IOException, InterruptedException {
        LeaderboardService leaderboard = new LeaderboardService(tempDir);
        leaderboard.update(UUID.randomUUID(), "Notch", 1000.0);
        leaderboard.update(UUID.randomUUID(), "Jeb", 500.0);

        server = new WebDashboardServer(leaderboard);
        server.start(0);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + server.getPort() + "/api/leaderboard"))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Notch"));
        assertTrue(response.body().contains("Jeb"));
        assertTrue(response.body().indexOf("Notch") < response.body().indexOf("Jeb"));
    }

    @Test
    void servesIndexPageAsHtml(@TempDir Path tempDir) throws IOException, InterruptedException {
        LeaderboardService leaderboard = new LeaderboardService(tempDir);
        server = new WebDashboardServer(leaderboard);
        server.start(0);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + server.getPort() + "/"))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("<html"));
        assertTrue(response.body().contains("Minecraft Wealth"));
    }

    @Test
    void countQueryParamLimitsResults(@TempDir Path tempDir) throws IOException, InterruptedException {
        LeaderboardService leaderboard = new LeaderboardService(tempDir);
        for (int i = 0; i < 10; i++) {
            leaderboard.update(UUID.randomUUID(), "Player" + i, i);
        }

        server = new WebDashboardServer(leaderboard);
        server.start(0);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + server.getPort() + "/api/leaderboard?count=3"))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        long rankOccurrences = response.body().split("\"rank\"").length - 1;
        assertEquals(3, rankOccurrences);
    }
}