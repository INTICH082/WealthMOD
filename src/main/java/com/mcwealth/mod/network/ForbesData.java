package com.mcwealth.mod.network;

import java.util.List;

public record ForbesData(List<Entry> entries) {

    public record Entry(int rank, String playerName, double wealth) {
    }
}