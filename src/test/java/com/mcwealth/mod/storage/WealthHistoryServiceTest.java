package com.mcwealth.mod.storage;

import com.mcwealth.mod.economy.WealthCategory;
import com.mcwealth.mod.economy.WealthResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WealthHistoryServiceTest {

    private static WealthResult fakeResult(UUID player, double total) {
        return WealthResult.builder(player, "Steve")
                .add(WealthCategory.INVENTORY, total)
                .build();
    }

    @Test
    void recordAppendsPoints(@TempDir Path tempDir) {
        WealthHistoryService history = new WealthHistoryService(tempDir);
        UUID player = UUID.randomUUID();

        history.record(fakeResult(player, 100.0), 5);
        history.record(fakeResult(player, 200.0), 3);

        List<HistoryPoint> points = history.get(player);
        assertEquals(2, points.size());
        assertEquals(100.0, points.get(0).total());
        assertEquals(5, points.get(0).rank());
        assertEquals(200.0, points.get(1).total());
        assertEquals(3, points.get(1).rank());
    }

    @Test
    void historyIsCappedPerPlayer(@TempDir Path tempDir) {
        WealthHistoryService history = new WealthHistoryService(tempDir);
        UUID player = UUID.randomUUID();

        for (int i = 0; i < 250; i++) {
            history.record(fakeResult(player, i), i);
        }

        List<HistoryPoint> points = history.get(player);
        assertTrue(points.size() <= 200);
        assertEquals(249.0, points.get(points.size() - 1).total());
    }

    @Test
    void persistsAndReloadsFromDisk(@TempDir Path tempDir) {
        UUID player = UUID.randomUUID();

        WealthHistoryService first = new WealthHistoryService(tempDir);
        first.record(fakeResult(player, 42.0), 1);
        first.saveNow();

        WealthHistoryService second = new WealthHistoryService(tempDir);
        second.load();

        List<HistoryPoint> points = second.get(player);
        assertEquals(1, points.size());
        assertEquals(42.0, points.get(0).total());
    }
}