package com.mcwealth.mod.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LeaderboardServiceTest {

    @Test
    void topOrdersByWealthDescending(@TempDir Path tempDir) {
        LeaderboardService leaderboard = new LeaderboardService(tempDir);
        UUID rich = UUID.randomUUID();
        UUID poor = UUID.randomUUID();
        UUID mid = UUID.randomUUID();

        leaderboard.update(poor, "Poor", 10.0);
        leaderboard.update(rich, "Rich", 1000.0);
        leaderboard.update(mid, "Mid", 500.0);

        List<LeaderboardEntry> top = leaderboard.top(10);
        assertEquals("Rich", top.get(0).playerName());
        assertEquals("Mid", top.get(1).playerName());
        assertEquals("Poor", top.get(2).playerName());
    }

    @Test
    void topRespectsLimit(@TempDir Path tempDir) {
        LeaderboardService leaderboard = new LeaderboardService(tempDir);
        for (int i = 0; i < 20; i++) {
            leaderboard.update(UUID.randomUUID(), "Player" + i, i);
        }

        assertEquals(5, leaderboard.top(5).size());
    }

    @Test
    void rankOfReflectsPosition(@TempDir Path tempDir) {
        LeaderboardService leaderboard = new LeaderboardService(tempDir);
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();

        leaderboard.update(first, "First", 1000.0);
        leaderboard.update(second, "Second", 500.0);

        assertEquals(1, leaderboard.rankOf(first));
        assertEquals(2, leaderboard.rankOf(second));
        assertEquals(-1, leaderboard.rankOf(UUID.randomUUID()));
    }

    @Test
    void findByNameIsCaseInsensitive(@TempDir Path tempDir) {
        LeaderboardService leaderboard = new LeaderboardService(tempDir);
        leaderboard.update(UUID.randomUUID(), "Notch", 42.0);

        assertEquals(42.0, leaderboard.findByName("notch").wealth());
        assertEquals(42.0, leaderboard.findByName("NOTCH").wealth());
        assertNull(leaderboard.findByName("Herobrine"));
    }

    @Test
    void persistsAndReloadsFromDisk(@TempDir Path tempDir) {
        UUID player = UUID.randomUUID();

        LeaderboardService first = new LeaderboardService(tempDir);
        first.update(player, "Alex", 777.0);
        first.saveNow();

        LeaderboardService second = new LeaderboardService(tempDir);
        second.load();

        assertEquals(777.0, second.get(player).wealth());
    }

    @Test
    void topChangeListenerFiresOnlyWhenTopPlayerActuallyChanges(@TempDir Path tempDir) {
        LeaderboardService leaderboard = new LeaderboardService(tempDir);
        List<String> newTopNames = new java.util.ArrayList<>();
        leaderboard.setTopChangeListener((newTop, previousTop) -> newTopNames.add(newTop.playerName()));

        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();

        leaderboard.update(a, "Alice", 100.0);
        assertEquals(List.of("Alice"), newTopNames);

        leaderboard.update(b, "Bob", 50.0);
        assertEquals(List.of("Alice"), newTopNames, "Bob didn't overtake Alice, no new event expected");

        leaderboard.update(a, "Alice", 120.0);
        assertEquals(List.of("Alice"), newTopNames, "Alice was already top, updating her own value isn't a new #1 event");

        leaderboard.update(b, "Bob", 500.0);
        assertEquals(List.of("Alice", "Bob"), newTopNames, "Bob overtaking Alice is a genuine new-top event");
    }

    @Test
    void loadingExistingDataDoesNotFireTopChangeListener(@TempDir Path tempDir) {
        UUID player = UUID.randomUUID();
        LeaderboardService first = new LeaderboardService(tempDir);
        first.update(player, "Alex", 777.0);
        first.saveNow();

        LeaderboardService second = new LeaderboardService(tempDir);
        List<String> events = new java.util.ArrayList<>();
        second.setTopChangeListener((newTop, previousTop) -> events.add(newTop.playerName()));
        second.load();

        assertTrue(events.isEmpty(), "load() must not fire the listener, only genuine post-startup changes should");
    }
}