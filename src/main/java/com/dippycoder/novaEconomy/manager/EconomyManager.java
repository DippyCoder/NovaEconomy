package com.dippycoder.novaEconomy.manager;

import com.dippycoder.novaEconomy.NovaEconomy;
import com.dippycoder.novaEconomy.api.NovaEconomyAPI;

import java.util.*;

public class EconomyManager implements NovaEconomyAPI {

    private final NovaEconomy plugin;

    public EconomyManager(NovaEconomy plugin) {
        this.plugin = plugin;
    }

    // ── NovaEconomyAPI ────────────────────────────────────────

    @Override
    public String getDefaultCurrency() {
        return plugin.getConfigManager().getDefaultCurrency();
    }

    @Override
    public List<String> getCurrencies() {
        return plugin.getCurrencyManager().getNames();
    }

    @Override
    public boolean hasCurrency(String currency) {
        return plugin.getCurrencyManager().exists(currency);
    }

    @Override
    public double getBalance(UUID uuid, String currency) {
        Currency c = resolveCurrency(currency);
        return plugin.getDatabaseManager().getBalance(uuid.toString(), c.name(), c.defaultBalance());
    }

    @Override
    public void setBalance(UUID uuid, String currency, double amount) {
        Currency c = resolveCurrency(currency);
        double clamped = c.isMaxBalanceUnlimited() ? amount : Math.min(amount, c.maxBalance());
        plugin.getDatabaseManager().setBalance(uuid.toString(), c.name(), clamped);
    }

    @Override
    public void deposit(UUID uuid, String currency, double amount) {
        Currency c = resolveCurrency(currency);
        double current = getBalance(uuid, c.name());
        double next = current + amount;
        if (!c.isMaxBalanceUnlimited()) next = Math.min(next, c.maxBalance());
        plugin.getDatabaseManager().setBalance(uuid.toString(), c.name(), next);
    }

    @Override
    public boolean withdraw(UUID uuid, String currency, double amount) {
        double current = getBalance(uuid, currency);
        if (current < amount) return false;
        plugin.getDatabaseManager().setBalance(uuid.toString(), currency.toUpperCase(), current - amount);
        return true;
    }

    @Override
    public boolean has(UUID uuid, String currency, double amount) {
        return getBalance(uuid, currency) >= amount;
    }

    @Override
    public void resetBalance(UUID uuid, String currency) {
        Currency c = resolveCurrency(currency);
        plugin.getDatabaseManager().setBalance(uuid.toString(), c.name(), c.defaultBalance());
    }

    // ── Top balances ──────────────────────────────────────────

    public List<Map.Entry<String, Double>> getTopBalances(String currency, int limit) {
        return plugin.getDatabaseManager().getTopBalances(currency.toUpperCase(), limit);
    }

    // ── Internal ──────────────────────────────────────────────

    private Currency resolveCurrency(String name) {
        return plugin.getCurrencyManager().get(name)
                .orElseThrow(() -> new IllegalArgumentException("Unknown currency: " + name));
    }
}
