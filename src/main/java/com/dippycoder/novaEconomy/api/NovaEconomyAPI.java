package com.dippycoder.novaEconomy.api;

import java.util.List;
import java.util.UUID;

/**
 * Public API for NovaEconomy.
 * Obtain an instance via {@link NovaEconomyProvider#get()}.
 */
public interface NovaEconomyAPI {

    /** Returns the name of the default currency (e.g. "COINS"). */
    String getDefaultCurrency();

    /** Returns all registered currency names. */
    List<String> getCurrencies();

    /** Returns true if a currency with the given name exists. */
    boolean hasCurrency(String currency);

    /** Returns the player's balance in the given currency. Creates a default balance entry if absent. */
    double getBalance(UUID uuid, String currency);

    /** Sets the player's balance unconditionally. */
    void setBalance(UUID uuid, String currency, double amount);

    /** Adds {@code amount} to the player's balance. */
    void deposit(UUID uuid, String currency, double amount);

    /**
     * Subtracts {@code amount} from the player's balance.
     *
     * @return true if successful, false if the player had insufficient funds.
     */
    boolean withdraw(UUID uuid, String currency, double amount);

    /** Returns true if the player has at least {@code amount} in the given currency. */
    boolean has(UUID uuid, String currency, double amount);

    /** Resets the player's balance to the currency's configured default. */
    void resetBalance(UUID uuid, String currency);
}
