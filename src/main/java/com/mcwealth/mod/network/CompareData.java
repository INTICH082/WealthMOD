package com.mcwealth.mod.network;

import java.util.Map;

public record CompareData(String selfName, double selfTotal, Map<String, Double> selfByCategory, String otherName, double otherTotal,
    Map<String, Double> otherByCategory, boolean otherOnline) {
}