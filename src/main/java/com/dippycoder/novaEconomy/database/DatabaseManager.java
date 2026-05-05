package com.dippycoder.novaEconomy.database;

import com.dippycoder.novaEconomy.NovaEconomy;

import java.util.List;
import java.util.Map;

public class DatabaseManager {

    private final StorageBackend backend;

    public DatabaseManager(NovaEconomy plugin) {
        String type = plugin.getConfigManager().getDatabaseType();
        backend = switch (type.toUpperCase()) {
            case "MYSQL" -> new MySQLBackend(plugin);
            case "REDIS" -> new RedisBackend(plugin);
            default      -> new SQLiteBackend(plugin);
        };
    }

    // ── Lifecycle ─────────────────────────────────────────────

    public void connect()    { backend.connect(); }
    public void disconnect() { backend.disconnect(); }

    // ── Balances ──────────────────────────────────────────────

    public double getBalance(String uuid, String currency, double defaultBalance) {
        return backend.getBalance(uuid, currency, defaultBalance);
    }

    public void setBalance(String uuid, String currency, double amount) {
        backend.setBalance(uuid, currency, amount);
    }

    public List<Map.Entry<String, Double>> getTopBalances(String currency, int limit) {
        return backend.getTopBalances(currency, limit);
    }

    // ── Dynamic Currencies ────────────────────────────────────

    public List<DynamicCurrencyRow> loadDynamicCurrencies() {
        return backend.loadDynamicCurrencies();
    }

    public void saveDynamicCurrency(String name, String displayName, String symbol,
                                    String singular, String plural,
                                    double defaultBalance, int decimals, double maxBalance) {
        backend.saveDynamicCurrency(name, displayName, symbol, singular, plural, defaultBalance, decimals, maxBalance);
    }

    public void deleteDynamicCurrency(String name) {
        backend.deleteDynamicCurrency(name);
    }

    public boolean isDynamicCurrency(String name) {
        return backend.isDynamicCurrency(name);
    }
}
