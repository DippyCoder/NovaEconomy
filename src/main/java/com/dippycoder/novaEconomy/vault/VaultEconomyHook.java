package com.dippycoder.novaEconomy.vault;

import com.dippycoder.novaEconomy.NovaEconomy;
import com.dippycoder.novaEconomy.manager.Currency;
import com.dippycoder.novaEconomy.util.AmountUtil;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.OfflinePlayer;

import java.util.List;
import java.util.UUID;

@SuppressWarnings("deprecation")
public class VaultEconomyHook implements Economy {

    private final NovaEconomy plugin;

    public VaultEconomyHook(NovaEconomy plugin) {
        this.plugin = plugin;
    }

    private Currency currency() {
        String name = plugin.getConfigManager().getVaultCurrency();
        return plugin.getCurrencyManager().get(name)
                .orElseGet(plugin.getCurrencyManager()::getDefault);
    }

    private UUID resolve(String playerName) {
        OfflinePlayer op = plugin.getServer().getOfflinePlayer(playerName);
        return op.getUniqueId();
    }

    // ── Economy interface ─────────────────────────────────────

    @Override public boolean isEnabled() { return true; }
    @Override public String getName() { return "NovaEconomy"; }
    @Override public boolean hasBankSupport() { return false; }

    @Override
    public int fractionalDigits() { return currency().decimals(); }

    @Override
    public String format(double amount) {
        Currency c = currency();
        String formatted = plugin.getConfigManager().isShortAmountFormatting()
                ? AmountUtil.formatShort(amount, c.decimals())
                : AmountUtil.formatFull(amount, c.decimals());
        return c.symbol() + formatted;
    }

    @Override public String currencyNamePlural() { return currency().plural(); }
    @Override public String currencyNameSingular() { return currency().singular(); }

    @Override public boolean hasAccount(String name) { return true; }
    @Override public boolean hasAccount(OfflinePlayer player) { return true; }
    @Override public boolean hasAccount(String name, String world) { return true; }
    @Override public boolean hasAccount(OfflinePlayer player, String world) { return true; }

    @Override
    public double getBalance(String name) {
        return plugin.getEconomyManager().getBalance(resolve(name), currency().name());
    }

    @Override
    public double getBalance(OfflinePlayer player) {
        return plugin.getEconomyManager().getBalance(player.getUniqueId(), currency().name());
    }

    @Override public double getBalance(String name, String world) { return getBalance(name); }
    @Override public double getBalance(OfflinePlayer player, String world) { return getBalance(player); }

    @Override public boolean has(String name, double amount) { return getBalance(name) >= amount; }
    @Override public boolean has(OfflinePlayer player, double amount) { return getBalance(player) >= amount; }
    @Override public boolean has(String name, String world, double amount) { return has(name, amount); }
    @Override public boolean has(OfflinePlayer player, String world, double amount) { return has(player, amount); }

    @Override
    public EconomyResponse withdrawPlayer(String name, double amount) {
        boolean ok = plugin.getEconomyManager().withdraw(resolve(name), currency().name(), amount);
        return ok
                ? new EconomyResponse(amount, getBalance(name), EconomyResponse.ResponseType.SUCCESS, null)
                : new EconomyResponse(0, getBalance(name), EconomyResponse.ResponseType.FAILURE, "Insufficient funds");
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, double amount) {
        return withdrawPlayer(player.getName() != null ? player.getName() : player.getUniqueId().toString(), amount);
    }

    @Override public EconomyResponse withdrawPlayer(String name, String world, double amount) { return withdrawPlayer(name, amount); }
    @Override public EconomyResponse withdrawPlayer(OfflinePlayer player, String world, double amount) { return withdrawPlayer(player, amount); }

    @Override
    public EconomyResponse depositPlayer(String name, double amount) {
        plugin.getEconomyManager().deposit(resolve(name), currency().name(), amount);
        return new EconomyResponse(amount, getBalance(name), EconomyResponse.ResponseType.SUCCESS, null);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, double amount) {
        return depositPlayer(player.getName() != null ? player.getName() : player.getUniqueId().toString(), amount);
    }

    @Override public EconomyResponse depositPlayer(String name, String world, double amount) { return depositPlayer(name, amount); }
    @Override public EconomyResponse depositPlayer(OfflinePlayer player, String world, double amount) { return depositPlayer(player, amount); }

    @Override public boolean createPlayerAccount(String name) { return true; }
    @Override public boolean createPlayerAccount(OfflinePlayer player) { return true; }
    @Override public boolean createPlayerAccount(String name, String world) { return true; }
    @Override public boolean createPlayerAccount(OfflinePlayer player, String world) { return true; }

    // ── Bank stubs (not supported) ────────────────────────────

    private static final EconomyResponse NOT_IMPL =
            new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "NovaEconomy does not support bank accounts.");

    @Override public EconomyResponse createBank(String name, String player) { return NOT_IMPL; }
    @Override public EconomyResponse createBank(String name, OfflinePlayer player) { return NOT_IMPL; }
    @Override public EconomyResponse deleteBank(String name) { return NOT_IMPL; }
    @Override public EconomyResponse bankBalance(String name) { return NOT_IMPL; }
    @Override public EconomyResponse bankHas(String name, double amount) { return NOT_IMPL; }
    @Override public EconomyResponse bankWithdraw(String name, double amount) { return NOT_IMPL; }
    @Override public EconomyResponse bankDeposit(String name, double amount) { return NOT_IMPL; }
    @Override public EconomyResponse isBankOwner(String name, String player) { return NOT_IMPL; }
    @Override public EconomyResponse isBankOwner(String name, OfflinePlayer player) { return NOT_IMPL; }
    @Override public EconomyResponse isBankMember(String name, String player) { return NOT_IMPL; }
    @Override public EconomyResponse isBankMember(String name, OfflinePlayer player) { return NOT_IMPL; }
    @Override public List<String> getBanks() { return List.of(); }
}
