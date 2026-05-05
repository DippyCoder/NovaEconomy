package com.dippycoder.novaEconomy.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.OptionalDouble;

public final class AmountUtil {

    private static final long K = 1_000L;
    private static final long M = 1_000_000L;
    private static final long B = 1_000_000_000L;
    private static final long T = 1_000_000_000_000L;

    private AmountUtil() {}

    /**
     * Parse a user-supplied amount string into a double.
     * Supports suffixes k/m/b/t (case-insensitive).
     * Returns empty if the string is not a valid positive number.
     */
    public static OptionalDouble parse(String input) {
        if (input == null || input.isBlank()) return OptionalDouble.empty();
        String s = input.trim().toLowerCase(Locale.ROOT);

        double multiplier = 1;
        if (s.endsWith("k")) { multiplier = K;  s = s.substring(0, s.length() - 1); }
        else if (s.endsWith("m")) { multiplier = M;  s = s.substring(0, s.length() - 1); }
        else if (s.endsWith("b")) { multiplier = B;  s = s.substring(0, s.length() - 1); }
        else if (s.endsWith("t")) { multiplier = T;  s = s.substring(0, s.length() - 1); }

        try {
            double value = Double.parseDouble(s) * multiplier;
            if (!Double.isFinite(value) || value < 0) return OptionalDouble.empty();
            return OptionalDouble.of(value);
        } catch (NumberFormatException e) {
            return OptionalDouble.empty();
        }
    }

    /**
     * Format a number in short form: 1500 → "1.5k", 1_500_000 → "1.5m".
     * Falls back to full format for values below 1 000.
     */
    public static String formatShort(double amount, int decimals) {
        if (amount >= T) return compact(amount / T, "t");
        if (amount >= B) return compact(amount / B, "b");
        if (amount >= M) return compact(amount / M, "m");
        if (amount >= K) return compact(amount / K, "k");
        return formatFull(amount, decimals);
    }

    /**
     * Format a number with thousands separators and a fixed number of decimal places.
     * Example: 1500.5 with decimals=2 → "1,500.50"
     */
    public static String formatFull(double amount, int decimals) {
        String pattern = "#,##0" + (decimals > 0 ? "." + "0".repeat(decimals) : "");
        DecimalFormat df = new DecimalFormat(pattern, DecimalFormatSymbols.getInstance(Locale.ROOT));
        return df.format(amount);
    }

    // compact: strip trailing ".0" from the short label
    private static String compact(double value, String suffix) {
        String s = String.format(Locale.ROOT, "%.1f", value);
        if (s.endsWith(".0")) s = s.substring(0, s.length() - 2);
        return s + suffix;
    }
}
