package com.dippycoder.novaEconomy;

import com.dippycoder.novaEconomy.api.NovaEconomyProvider;
import com.dippycoder.novaEconomy.command.*;
import com.dippycoder.novaEconomy.config.ConfigManager;
import com.dippycoder.novaEconomy.database.DatabaseManager;
import com.dippycoder.novaEconomy.manager.CurrencyManager;
import com.dippycoder.novaEconomy.manager.DiscordWebhookManager;
import com.dippycoder.novaEconomy.manager.EconomyManager;
import com.dippycoder.novaEconomy.manager.UpdateChecker;
import com.dippycoder.novaEconomy.message.MessageManager;
import com.dippycoder.novaEconomy.config.ConfigManager.VaultMode;
import com.dippycoder.novaEconomy.vault.VaultEconomyHook;
import com.dippycoder.novaEconomy.vault.VaultUnlockedEconomyHook;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public final class NovaEconomy extends JavaPlugin {

    private static NovaEconomy instance;

    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private CurrencyManager currencyManager;
    private EconomyManager economyManager;
    private MessageManager messageManager;
    private DiscordWebhookManager discordWebhookManager;
    private UpdateChecker updateChecker;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        configManager        = new ConfigManager(this);
        databaseManager      = new DatabaseManager(this);
        databaseManager.connect();
        currencyManager      = new CurrencyManager(this);
        economyManager       = new EconomyManager(this);
        messageManager       = new MessageManager(this);
        discordWebhookManager = new DiscordWebhookManager(this);

        NovaEconomyProvider.register(economyManager);

        updateChecker = new UpdateChecker(this);
        updateChecker.checkAsync();

        registerVault();
        registerCommands();

        getLogger().info("NovaEconomy v" + getDescription().getVersion() + " enabled.");
    }

    @Override
    public void onDisable() {
        databaseManager.disconnect();
        NovaEconomyProvider.unregister();
        getLogger().info("NovaEconomy disabled.");
    }

    // ── Vault ─────────────────────────────────────────────────

    private void registerVault() {
        VaultMode mode = configManager.getVaultMode();

        // VaultUnlocked 2.x registers itself under the plugin name "Vault" (no separate VaultUnlocked plugin).
        // Detect modern vault2 support by checking whether vault2.Economy is loadable from Vault's classloader.
        Plugin vaultPlugin = getServer().getPluginManager().getPlugin("Vault");
        boolean hasVault2 = false;
        if (vaultPlugin != null) {
            try {
                vaultPlugin.getClass().getClassLoader().loadClass("net.milkbowl.vault2.economy.Economy");
                hasVault2 = true;
            } catch (ClassNotFoundException ignored) {}
        }

        if (mode == VaultMode.VAULT || mode == VaultMode.BOTH) {
            if (vaultPlugin != null) {
                getServer().getServicesManager().register(
                        Economy.class,
                        new VaultEconomyHook(this),
                        this,
                        ServicePriority.Normal
                );
                getLogger().info("Registered Vault economy provider (currency: " + configManager.getVaultCurrency() + ").");
            } else {
                getLogger().warning("Vault not found — skipping Vault economy registration.");
            }
        }

        if (mode == VaultMode.VAULT_UNLOCKED || mode == VaultMode.BOTH) {
            if (vaultPlugin != null && hasVault2) {
                if (getServer().getServicesManager().getRegistration(Economy.class) == null) {
                    getServer().getServicesManager().register(
                            Economy.class,
                            new VaultEconomyHook(this),
                            this,
                            ServicePriority.Normal
                    );
                }
                try {
                    @SuppressWarnings("unchecked")
                    Class<net.milkbowl.vault2.economy.Economy> vault2Class =
                            (Class<net.milkbowl.vault2.economy.Economy>)
                            vaultPlugin.getClass().getClassLoader()
                                    .loadClass("net.milkbowl.vault2.economy.Economy");
                    getServer().getServicesManager().register(
                            vault2Class,
                            new VaultUnlockedEconomyHook(this),
                            this,
                            ServicePriority.Normal
                    );
                    getLogger().info("Registered VaultUnlocked economy provider (multi-currency).");
                } catch (ClassNotFoundException e) {
                    getLogger().severe("vault2 Economy class not found — skipping VaultUnlocked economy registration.");
                }
            } else if (vaultPlugin == null) {
                getLogger().warning("Vault not found — skipping VaultUnlocked economy registration.");
            } else {
                getLogger().warning("Installed Vault does not support vault2 — skipping VaultUnlocked economy registration.");
            }
        }
    }

    // ── Commands ──────────────────────────────────────────────

    private void registerCommands() {
        register("novaeconomy", new NovaEcoCommand(this));
        register("eco",         new EcoCommand(this));
        register("balance",     new BalanceCommand(this));
        register("baltop",      new BalTopCommand(this));
        register("pay",         new PayCommand(this));
    }

    private void register(String name, BaseCommand handler) {
        PluginCommand cmd = getCommand(name);
        if (cmd == null) {
            getLogger().warning("Command '" + name + "' not found in paper-plugin.yml.");
            return;
        }
        cmd.setExecutor(handler);
        cmd.setTabCompleter(handler);
    }

    // ── Getters ───────────────────────────────────────────────

    public static NovaEconomy getInstance() { return instance; }

    public ConfigManager getConfigManager()               { return configManager; }
    public DatabaseManager getDatabaseManager()           { return databaseManager; }
    public CurrencyManager getCurrencyManager()           { return currencyManager; }
    public EconomyManager getEconomyManager()             { return economyManager; }
    public MessageManager getMessageManager()             { return messageManager; }
    public DiscordWebhookManager getDiscordWebhookManager() { return discordWebhookManager; }
    public UpdateChecker getUpdateChecker()                 { return updateChecker; }
}
