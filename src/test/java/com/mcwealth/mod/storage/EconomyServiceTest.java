package com.mcwealth.mod.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EconomyServiceTest {

    @Test
    void depositAndWithdrawAdjustBalance(@TempDir Path tempDir) {
        EconomyService economy = new EconomyService(tempDir);
        UUID player = UUID.randomUUID();

        assertEquals(0.0, economy.getBalance(player));

        economy.deposit(player, 100.0);
        assertEquals(100.0, economy.getBalance(player));

        assertTrue(economy.withdraw(player, 40.0));
        assertEquals(60.0, economy.getBalance(player));
    }

    @Test
    void withdrawFailsOnInsufficientFunds() {
        EconomyService economy = new EconomyService(Path.of(System.getProperty("java.io.tmpdir")));
        UUID player = UUID.randomUUID();
        economy.deposit(player, 10.0);

        assertFalse(economy.withdraw(player, 50.0));
        assertEquals(10.0, economy.getBalance(player));
    }

    @Test
    void transferMovesBalanceBetweenPlayers() {
        EconomyService economy = new EconomyService(Path.of(System.getProperty("java.io.tmpdir")));
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        economy.deposit(a, 100.0);

        assertTrue(economy.transfer(a, b, 30.0));
        assertEquals(70.0, economy.getBalance(a));
        assertEquals(30.0, economy.getBalance(b));
    }

    @Test
    void transferFailsWithoutMutatingEitherBalanceOnInsufficientFunds() {
        EconomyService economy = new EconomyService(Path.of(System.getProperty("java.io.tmpdir")));
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        economy.deposit(a, 10.0);

        assertFalse(economy.transfer(a, b, 50.0));
        assertEquals(10.0, economy.getBalance(a));
        assertEquals(0.0, economy.getBalance(b));
    }

    @Test
    void persistsAndReloadsFromDisk(@TempDir Path tempDir) {
        UUID player = UUID.randomUUID();

        EconomyService first = new EconomyService(tempDir);
        first.deposit(player, 250.0);
        first.saveNow();

        EconomyService second = new EconomyService(tempDir);
        second.load();

        assertEquals(250.0, second.getBalance(player));
    }
}