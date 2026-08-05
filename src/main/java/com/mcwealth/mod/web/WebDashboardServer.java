package com.mcwealth.mod.web;

import com.google.gson.Gson;
import com.mcwealth.mod.MinecraftWealthMod;
import com.mcwealth.mod.storage.LeaderboardEntry;
import com.mcwealth.mod.storage.LeaderboardService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public final class WebDashboardServer {

    private static final Gson GSON = new Gson();
    private static final int DEFAULT_COUNT = 100;
    private static final int MAX_COUNT = 500;

    private final LeaderboardService leaderboard;
    private HttpServer server;

    public WebDashboardServer(LeaderboardService leaderboard) {
        this.leaderboard = leaderboard;
    }

    public void start(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", this::handleIndex);
        server.createContext("/api/leaderboard", this::handleApiLeaderboard);
        server.setExecutor(Executors.newFixedThreadPool(2, r -> {
            Thread thread = new Thread(r, "minecraftwealth-web");
            thread.setDaemon(true);
            return thread;
        }));
        server.start();
        MinecraftWealthMod.LOGGER.info("Minecraft Wealth web dashboard listening on port {}", port);
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    public int getPort() {
        return server != null ? server.getAddress().getPort() : -1;
    }

    private void handleIndex(HttpExchange exchange) throws IOException {
        byte[] body = INDEX_HTML.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
        exchange.sendResponseHeaders(200, body.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body);
        }
    }

    private void handleApiLeaderboard(HttpExchange exchange) throws IOException {
        int count = parseCount(exchange.getRequestURI().getQuery());
        List<LeaderboardEntry> top = leaderboard.top(count);

        List<Map<String, Object>> payload = new ArrayList<>();
        int rank = 1;
        for (LeaderboardEntry entry : top) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("rank", rank++);
            row.put("name", entry.playerName());
            row.put("wealth", entry.wealth());
            payload.add(row);
        }

        byte[] body = GSON.toJson(payload).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(200, body.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body);
        }
    }

    private static int parseCount(String query) {
        if (query == null) {
            return DEFAULT_COUNT;
        }
        for (String part : query.split("&")) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2 && kv[0].equals("count")) {
                try {
                    return Math.max(1, Math.min(MAX_COUNT, Integer.parseInt(kv[1])));
                } catch (NumberFormatException ignored) {
                    return DEFAULT_COUNT;
                }
            }
        }
        return DEFAULT_COUNT;
    }

    private static final String INDEX_HTML = """
            <!doctype html>
            <html lang="en">
            <head>
            <meta charset="utf-8">
            <title>Minecraft Wealth</title>
            <style>
              body { background:#141414; color:#eee; font-family: system-ui, sans-serif; padding: 24px; }
              h1 { color: #55c878; font-weight: 600; }
              table { border-collapse: collapse; width: 100%; max-width: 600px; }
              th, td { text-align: left; padding: 6px 12px; border-bottom: 1px solid #333; }
              th { color: #999; font-weight: normal; }
              .wealth { color: #55c878; text-align: right; }
              .rank { color: #888; width: 40px; }
              .empty { color: #777; padding: 12px; }
            </style>
            </head>
            <body>
            <h1>Minecraft Wealth &mdash; Leaderboard</h1>
            <table id="board">
              <thead><tr><th class="rank">#</th><th>Player</th><th class="wealth">$</th></tr></thead>
              <tbody></tbody>
            </table>
            <p class="empty" id="empty" style="display:none">No data yet.</p>
            <script>
              async function refresh() {
                const res = await fetch('/api/leaderboard?count=100');
                const rows = await res.json();
                const tbody = document.querySelector('#board tbody');
                const empty = document.querySelector('#empty');
                if (!rows.length) {
                  tbody.innerHTML = '';
                  empty.style.display = 'block';
                  return;
                }
                empty.style.display = 'none';
                tbody.innerHTML = rows.map(r =>
                  `<tr><td class="rank">#${r.rank}</td><td>${r.name}</td><td class="wealth">$${r.wealth.toLocaleString(undefined, {maximumFractionDigits: 2})}</td></tr>`
                ).join('');
              }
              refresh();
              setInterval(refresh, 5000);
            </script>
            </body>
            </html>
            """;
}