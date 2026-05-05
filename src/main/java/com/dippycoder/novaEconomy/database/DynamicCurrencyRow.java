package com.dippycoder.novaEconomy.database;

public record DynamicCurrencyRow(
        String name, String displayName, String symbol,
        String singular, String plural,
        double defaultBalance, int decimals, double maxBalance
) {}
