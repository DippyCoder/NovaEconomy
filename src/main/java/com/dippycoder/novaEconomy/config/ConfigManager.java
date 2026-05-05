package com.dippycoder.novaEconomy.config;

import com.dippycoder.novaEconomy.NovaEconomy;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {

    private final NovaEconomy plugin;
    private FileConfiguration config;

    public ConfigManager(NovaEconomy plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        config = plugin.getConfig();
    }

    // ── General ───────────────────────────────────────────────

    public String getLanguage() {
        return config.getString("general.language", "en");
    }

    public boolean isDebug() {
        return config.getBoolean("general.debug", false);
    }

    public boolean isCheckForUpdates() {
        return config.getBoolean("general.check-for-updates", true);
    }

    public void setCheckForUpdates(boolean value) {
        plugin.getConfig().set("general.check-for-updates", value);
        plugin.saveConfig();
    }

    // ── Economy ───────────────────────────────────────────────

    public String getDefaultCurrency() {
        return config.getString("economy.default-currency", "COINS").toUpperCase();
    }

    public boolean isShortAmountFormatting() {
        return config.getBoolean("economy.short-amount-formatting", true);
    }

    // ── Small Caps ────────────────────────────────────────────

    public boolean isSmallCapsEnabled() {
        return config.getBoolean("chat.small-caps.enabled", false);
    }

    public boolean isSmallCapsPermissionRequired() {
        return config.getBoolean("chat.small-caps.require-permission", false);
    }

    // ── Pay ───────────────────────────────────────────────────

    public double getPayMinAmount() {
        return config.getDouble("pay.min-amount", 1.0);
    }

    public double getPayTax() {
        return config.getDouble("pay.tax", 0.0);
    }

    public boolean isPaySelfAllowed() {
        return config.getBoolean("pay.self-pay", false);
    }

    // ── Vault ─────────────────────────────────────────────────

    public enum VaultMode { VAULT, VAULT_UNLOCKED, BOTH }

    public VaultMode getVaultMode() {
        String raw = config.getString("vault.mode", "BOTH").toUpperCase();
        try { return VaultMode.valueOf(raw); }
        catch (IllegalArgumentException e) { return VaultMode.BOTH; }
    }

    public String getVaultCurrency() {
        return config.getString("vault.currency", getDefaultCurrency()).toUpperCase();
    }

    // ── Currencies ────────────────────────────────────────────

    public ConfigurationSection getCurrenciesSection() {
        return config.getConfigurationSection("currencies");
    }

    public ConfigurationSection getCurrencySection(String name) {
        var section = getCurrenciesSection();
        return section == null ? null : section.getConfigurationSection(name.toUpperCase());
    }

    // ── Database ──────────────────────────────────────────────

    public String getDatabaseType() {
        return config.getString("database.type", "SQLITE").toUpperCase();
    }

    public String getMysqlHost()              { return config.getString("database.mysql.host", "localhost"); }
    public int    getMysqlPort()              { return config.getInt("database.mysql.port", 3306); }
    public String getMysqlDatabase()          { return config.getString("database.mysql.database", "novaeconomy"); }
    public String getMysqlUsername()          { return config.getString("database.mysql.username", "root"); }
    public String getMysqlPassword()          { return config.getString("database.mysql.password", ""); }
    public int    getMysqlPoolSize()          { return config.getInt("database.mysql.pool-size", 10); }
    public long   getMysqlConnectionTimeout() { return config.getLong("database.mysql.connection-timeout", 30000L); }

    public String getRedisHost()       { return config.getString("database.redis.host", "localhost"); }
    public int    getRedisPort()       { return config.getInt("database.redis.port", 6379); }
    public String getRedisPassword()   { return config.getString("database.redis.password", ""); }
    public int    getRedisDatabase()   { return config.getInt("database.redis.database", 0); }
    public String getRedisKeyPrefix()  { return config.getString("database.redis.key-prefix", "nova"); }
    public int    getRedisPoolSize()   { return config.getInt("database.redis.pool-size", 10); }

    // ── Permissions ───────────────────────────────────────────

    public String getPermission(String key) {
        String override = config.getString("permissions." + key);
        if (override != null && !override.isBlank()) return override;
        return "novaeco." + key;
    }
}
