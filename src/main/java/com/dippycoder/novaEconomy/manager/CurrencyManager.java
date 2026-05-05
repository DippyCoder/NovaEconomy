package com.dippycoder.novaEconomy.manager;

import com.dippycoder.novaEconomy.NovaEconomy;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

public class CurrencyManager {

    private final NovaEconomy plugin;
    private final Map<String, Currency> currencies = new LinkedHashMap<>();

    public CurrencyManager(NovaEconomy plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        currencies.clear();
        loadFromConfig();
        loadFromDatabase();
    }

    private void loadFromConfig() {
        ConfigurationSection section = plugin.getConfigManager().getCurrenciesSection();
        if (section == null) return;
        for (String name : section.getKeys(false)) {
            ConfigurationSection cs = section.getConfigurationSection(name);
            if (cs == null) continue;
            currencies.put(name.toUpperCase(), new Currency(
                    name.toUpperCase(),
                    cs.getString("display-name", name),
                    cs.getString("symbol", "$"),
                    cs.getString("singular", name),
                    cs.getString("plural", name),
                    cs.getDouble("default-balance", 0.0),
                    Math.max(0, Math.min(8, cs.getInt("decimals", 2))),
                    cs.getDouble("max-balance", -1),
                    false
            ));
        }
    }

    private void loadFromDatabase() {
        for (var row : plugin.getDatabaseManager().loadDynamicCurrencies()) {
            String key = row.name().toUpperCase();
            if (currencies.containsKey(key)) continue; // config takes priority
            currencies.put(key, new Currency(
                    key,
                    row.displayName(),
                    row.symbol(),
                    row.singular(),
                    row.plural(),
                    row.defaultBalance(),
                    row.decimals(),
                    row.maxBalance(),
                    true
            ));
        }
    }

    // ── Queries ───────────────────────────────────────────────

    public Optional<Currency> get(String name) {
        return Optional.ofNullable(currencies.get(name.toUpperCase()));
    }

    public boolean exists(String name) {
        return currencies.containsKey(name.toUpperCase());
    }

    public Collection<Currency> getAll() {
        return Collections.unmodifiableCollection(currencies.values());
    }

    public List<String> getNames() {
        return List.copyOf(currencies.keySet());
    }

    public Currency getDefault() {
        String def = plugin.getConfigManager().getDefaultCurrency();
        return currencies.getOrDefault(def, currencies.isEmpty() ? null : currencies.values().iterator().next());
    }

    // ── Mutations ─────────────────────────────────────────────

    public boolean addDynamic(String name) {
        String key = name.toUpperCase();
        if (currencies.containsKey(key)) return false;
        Currency c = new Currency(key, name, "$", name, name + "s", 0.0, 2, -1, true);
        currencies.put(key, c);
        plugin.getDatabaseManager().saveDynamicCurrency(
                c.name(), c.displayName(), c.symbol(),
                c.singular(), c.plural(),
                c.defaultBalance(), c.decimals(), c.maxBalance()
        );
        return true;
    }

    /** Returns false if the currency does not exist or is config-defined. */
    public boolean removeDynamic(String name) {
        String key = name.toUpperCase();
        Currency c = currencies.get(key);
        if (c == null || !c.dynamic()) return false;
        currencies.remove(key);
        plugin.getDatabaseManager().deleteDynamicCurrency(key);
        return true;
    }

    public boolean isConfigDefined(String name) {
        Currency c = currencies.get(name.toUpperCase());
        return c != null && !c.dynamic();
    }
}
