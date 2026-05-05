package com.dippycoder.novaEconomy.api;

public final class NovaEconomyProvider {

    private static NovaEconomyAPI instance;

    private NovaEconomyProvider() {}

    public static NovaEconomyAPI get() {
        if (instance == null) throw new IllegalStateException("NovaEconomy is not loaded.");
        return instance;
    }

    public static void register(NovaEconomyAPI api) {
        if (instance != null) throw new IllegalStateException("NovaEconomyAPI is already registered.");
        instance = api;
    }

    public static void unregister() {
        instance = null;
    }
}
