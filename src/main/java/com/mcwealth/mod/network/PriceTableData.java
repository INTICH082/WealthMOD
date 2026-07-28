package com.mcwealth.mod.network;

import java.util.Map;

public record PriceTableData(Map<String, Double> prices, double defaultPrice) {
}