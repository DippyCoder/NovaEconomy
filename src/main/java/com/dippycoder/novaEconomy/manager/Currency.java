package com.dippycoder.novaEconomy.manager;

public record Currency(
        String name,
        String displayName,
        String symbol,
        String singular,
        String plural,
        double defaultBalance,
        int decimals,
        double maxBalance,
        boolean dynamic
) {
    public boolean isMaxBalanceUnlimited() { return maxBalance < 0; }

    public boolean isWithinLimit(double amount) {
        return isMaxBalanceUnlimited() || amount <= maxBalance;
    }
}
