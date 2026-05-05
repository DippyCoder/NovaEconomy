package com.dippycoder.novaEconomy.vault;

import com.dippycoder.novaEconomy.NovaEconomy;
import com.dippycoder.novaEconomy.manager.Currency;
import com.dippycoder.novaEconomy.util.AmountUtil;
import net.milkbowl.vault2.economy.AccountPermission;
import net.milkbowl.vault2.economy.Economy;
import net.milkbowl.vault2.economy.EconomyResponse;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.*;

@SuppressWarnings("deprecation")
public class VaultUnlockedEconomyHook implements Economy {

    private final NovaEconomy plugin;

    public VaultUnlockedEconomyHook(NovaEconomy plugin) {
        this.plugin = plugin;
    }

    // ── Identity ──────────────────────────────────────────────

    @Override public boolean isEnabled() { return true; }
    @Override public @NotNull String getName() { return "NovaEconomy"; }
    @Override public boolean hasSharedAccountSupport() { return false; }
    @Override public boolean hasMultiCurrencySupport() { return true; }

    // ── Currency info ─────────────────────────────────────────

    @Override
    public int fractionalDigits(@NotNull String pluginName) {
        return defaultCurrency().decimals();
    }

    // Deprecated — no pluginName; delegate to the named overloads
    @Override
    public @NotNull String format(@NotNull BigDecimal amount) {
        return format("NovaEconomy", amount);
    }

    @Override
    public @NotNull String format(@NotNull String pluginName, @NotNull BigDecimal amount) {
        return format(pluginName, amount, plugin.getConfigManager().getDefaultCurrency());
    }

    @Override
    public @NotNull String format(@NotNull BigDecimal amount, @NotNull String currency) {
        return format("NovaEconomy", amount, currency);
    }

    @Override
    public @NotNull String format(@NotNull String pluginName, @NotNull BigDecimal amount, @NotNull String currency) {
        Currency c = resolveCurrency(currency);
        String formatted = plugin.getConfigManager().isShortAmountFormatting()
                ? AmountUtil.formatShort(amount.doubleValue(), c.decimals())
                : AmountUtil.formatFull(amount.doubleValue(), c.decimals());
        return c.symbol() + formatted;
    }

    @Override
    public boolean hasCurrency(@NotNull String currency) {
        return plugin.getCurrencyManager().exists(currency);
    }

    @Override
    public @NotNull String getDefaultCurrency(@NotNull String pluginName) {
        return plugin.getConfigManager().getDefaultCurrency();
    }

    @Override
    public @NotNull String defaultCurrencyNamePlural(@NotNull String pluginName) {
        return defaultCurrency().plural();
    }

    @Override
    public @NotNull String defaultCurrencyNameSingular(@NotNull String pluginName) {
        return defaultCurrency().singular();
    }

    @Override
    public @NotNull Collection<String> currencies() {
        return plugin.getCurrencyManager().getNames();
    }

    // ── Account management ────────────────────────────────────

    // Balances are created lazily on first access — all createAccount calls return true.
    @Override public boolean createAccount(@NotNull UUID accountID, @NotNull String name) { return true; }
    @Override public boolean createAccount(@NotNull UUID accountID, @NotNull String name, boolean player) { return true; }
    @Override public boolean createAccount(@NotNull UUID accountID, @NotNull String name, @NotNull String worldName) { return true; }
    @Override public boolean createAccount(@NotNull UUID accountID, @NotNull String name, @NotNull String worldName, boolean player) { return true; }

    @Override
    public @NotNull Map<UUID, String> getUUIDNameMap() {
        Map<UUID, String> map = new LinkedHashMap<>();
        for (OfflinePlayer op : plugin.getServer().getOnlinePlayers()) {
            map.put(op.getUniqueId(), op.getName());
        }
        return map;
    }

    @Override
    public @NotNull Optional<String> getAccountName(@NotNull UUID accountID) {
        return Optional.ofNullable(plugin.getServer().getOfflinePlayer(accountID).getName());
    }

    @Override public boolean hasAccount(@NotNull UUID accountID) { return true; }
    @Override public boolean hasAccount(@NotNull UUID accountID, @NotNull String worldName) { return true; }

    @Override public boolean renameAccount(@NotNull UUID accountID, @NotNull String name) { return false; }
    @Override public boolean renameAccount(@NotNull String pluginName, @NotNull UUID accountID, @NotNull String name) { return false; }
    @Override public boolean deleteAccount(@NotNull String pluginName, @NotNull UUID accountID) { return false; }

    @Override
    public boolean accountSupportsCurrency(@NotNull String pluginName, @NotNull UUID accountID, @NotNull String currency) {
        return plugin.getCurrencyManager().exists(currency);
    }

    @Override
    public boolean accountSupportsCurrency(@NotNull String pluginName, @NotNull UUID accountID,
                                           @NotNull String currency, @NotNull String world) {
        return plugin.getCurrencyManager().exists(currency);
    }

    // ── Balance ───────────────────────────────────────────────

    @Override
    public @NotNull BigDecimal getBalance(@NotNull String pluginName, @NotNull UUID accountID) {
        return bd(plugin.getEconomyManager().getBalance(accountID, plugin.getConfigManager().getDefaultCurrency()));
    }

    @Override
    public @NotNull BigDecimal getBalance(@NotNull String pluginName, @NotNull UUID accountID, @NotNull String world) {
        return getBalance(pluginName, accountID);
    }

    @Override
    public @NotNull BigDecimal getBalance(@NotNull String pluginName, @NotNull UUID accountID,
                                          @NotNull String world, @NotNull String currency) {
        return bd(plugin.getEconomyManager().getBalance(accountID, currency));
    }

    @Override
    public boolean has(@NotNull String pluginName, @NotNull UUID accountID, @NotNull BigDecimal amount) {
        return plugin.getEconomyManager().has(accountID, plugin.getConfigManager().getDefaultCurrency(), amount.doubleValue());
    }

    @Override
    public boolean has(@NotNull String pluginName, @NotNull UUID accountID,
                       @NotNull String worldName, @NotNull BigDecimal amount) {
        return has(pluginName, accountID, amount);
    }

    @Override
    public boolean has(@NotNull String pluginName, @NotNull UUID accountID,
                       @NotNull String worldName, @NotNull String currency, @NotNull BigDecimal amount) {
        return plugin.getEconomyManager().has(accountID, currency, amount.doubleValue());
    }

    // ── Withdraw ──────────────────────────────────────────────

    @Override
    public @NotNull EconomyResponse withdraw(@NotNull String pluginName, @NotNull UUID accountID,
                                             @NotNull BigDecimal amount) {
        return withdraw(pluginName, accountID, null, plugin.getConfigManager().getDefaultCurrency(), amount);
    }

    @Override
    public @NotNull EconomyResponse withdraw(@NotNull String pluginName, @NotNull UUID accountID,
                                             @NotNull String worldName, @NotNull BigDecimal amount) {
        return withdraw(pluginName, accountID, worldName, plugin.getConfigManager().getDefaultCurrency(), amount);
    }

    @Override
    public @NotNull EconomyResponse withdraw(@NotNull String pluginName, @NotNull UUID accountID,
                                             String worldName, @NotNull String currency, @NotNull BigDecimal amount) {
        if (!plugin.getCurrencyManager().exists(currency))
            return fail(accountID, plugin.getConfigManager().getDefaultCurrency(), "Unknown currency: " + currency);

        boolean ok = plugin.getEconomyManager().withdraw(accountID, currency, amount.doubleValue());
        BigDecimal balance = bd(plugin.getEconomyManager().getBalance(accountID, currency));
        return ok
                ? new EconomyResponse(amount, balance, EconomyResponse.ResponseType.SUCCESS, null)
                : new EconomyResponse(BigDecimal.ZERO, balance, EconomyResponse.ResponseType.FAILURE, "Insufficient funds");
    }

    // ── Deposit ───────────────────────────────────────────────

    @Override
    public @NotNull EconomyResponse deposit(@NotNull String pluginName, @NotNull UUID accountID,
                                            @NotNull BigDecimal amount) {
        return deposit(pluginName, accountID, null, plugin.getConfigManager().getDefaultCurrency(), amount);
    }

    @Override
    public @NotNull EconomyResponse deposit(@NotNull String pluginName, @NotNull UUID accountID,
                                            @NotNull String worldName, @NotNull BigDecimal amount) {
        return deposit(pluginName, accountID, worldName, plugin.getConfigManager().getDefaultCurrency(), amount);
    }

    @Override
    public @NotNull EconomyResponse deposit(@NotNull String pluginName, @NotNull UUID accountID,
                                            String worldName, @NotNull String currency, @NotNull BigDecimal amount) {
        if (!plugin.getCurrencyManager().exists(currency))
            return fail(accountID, plugin.getConfigManager().getDefaultCurrency(), "Unknown currency: " + currency);

        plugin.getEconomyManager().deposit(accountID, currency, amount.doubleValue());
        BigDecimal balance = bd(plugin.getEconomyManager().getBalance(accountID, currency));
        return new EconomyResponse(amount, balance, EconomyResponse.ResponseType.SUCCESS, null);
    }

    // ── Shared accounts (not supported) ──────────────────────

    @Override public boolean createSharedAccount(@NotNull String pluginName, @NotNull UUID accountID, @NotNull String name, @NotNull UUID owner) { return false; }
    @Override public boolean isAccountOwner(@NotNull String pluginName, @NotNull UUID accountID, @NotNull UUID uuid) { return false; }
    @Override public boolean setOwner(@NotNull String pluginName, @NotNull UUID accountID, @NotNull UUID uuid) { return false; }
    @Override public boolean isAccountMember(@NotNull String pluginName, @NotNull UUID accountID, @NotNull UUID uuid) { return false; }
    @Override public boolean addAccountMember(@NotNull String pluginName, @NotNull UUID accountID, @NotNull UUID uuid) { return false; }
    @Override public boolean addAccountMember(@NotNull String pluginName, @NotNull UUID accountID, @NotNull UUID uuid, @NotNull AccountPermission... perms) { return false; }
    @Override public boolean removeAccountMember(@NotNull String pluginName, @NotNull UUID accountID, @NotNull UUID uuid) { return false; }
    @Override public boolean hasAccountPermission(@NotNull String pluginName, @NotNull UUID accountID, @NotNull UUID uuid, @NotNull AccountPermission permission) { return false; }
    @Override public boolean updateAccountPermission(@NotNull String pluginName, @NotNull UUID accountID, @NotNull UUID uuid, @NotNull AccountPermission permission, boolean value) { return false; }

    // ── Helpers ───────────────────────────────────────────────

    private Currency defaultCurrency() {
        return plugin.getCurrencyManager().get(plugin.getConfigManager().getDefaultCurrency())
                .orElseGet(plugin.getCurrencyManager()::getDefault);
    }

    private Currency resolveCurrency(String name) {
        return plugin.getCurrencyManager().get(name).orElseGet(this::defaultCurrency);
    }

    private static BigDecimal bd(double value) {
        return BigDecimal.valueOf(value);
    }

    private EconomyResponse fail(UUID accountID, String currency, String message) {
        BigDecimal balance = bd(plugin.getEconomyManager().getBalance(accountID, currency));
        return new EconomyResponse(BigDecimal.ZERO, balance, EconomyResponse.ResponseType.FAILURE, message);
    }
}
