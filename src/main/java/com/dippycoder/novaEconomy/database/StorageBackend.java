package com.dippycoder.novaEconomy.database;

import java.util.List;
import java.util.Map;

public interface StorageBackend {
    void connect();
    void disconnect();

    double getBalance(String uuid, String currency, double defaultBalance);
    void setBalance(String uuid, String currency, double amount);
    List<Map.Entry<String, Double>> getTopBalances(String currency, int limit);

    List<DynamicCurrencyRow> loadDynamicCurrencies();
    void saveDynamicCurrency(String name, String displayName, String symbol,
                             String singular, String plural,
                             double defaultBalance, int decimals, double maxBalance);
    void deleteDynamicCurrency(String name);
    boolean isDynamicCurrency(String name);
}
