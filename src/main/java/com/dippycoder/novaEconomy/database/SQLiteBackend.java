package com.dippycoder.novaEconomy.database;

import com.dippycoder.novaEconomy.NovaEconomy;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.logging.Level;

public class SQLiteBackend implements StorageBackend {

    private final NovaEconomy plugin;
    private Connection conn;

    public SQLiteBackend(NovaEconomy plugin) {
        this.plugin = plugin;
    }

    @Override
    public void connect() {
        File dbFolder = new File(plugin.getDataFolder(), "db");
        dbFolder.mkdirs();
        try {
            conn = DriverManager.getConnection("jdbc:sqlite:" + new File(dbFolder, "economy.db").getAbsolutePath());
            try (Statement st = conn.createStatement()) {
                st.execute("""
                        CREATE TABLE IF NOT EXISTS balances (
                            uuid     TEXT NOT NULL,
                            currency TEXT NOT NULL,
                            balance  REAL NOT NULL DEFAULT 0.0,
                            PRIMARY KEY (uuid, currency)
                        )""");
                st.execute("""
                        CREATE TABLE IF NOT EXISTS currencies (
                            name         TEXT NOT NULL PRIMARY KEY,
                            display_name TEXT NOT NULL,
                            symbol       TEXT NOT NULL,
                            singular     TEXT NOT NULL,
                            plural       TEXT NOT NULL,
                            default_bal  REAL NOT NULL DEFAULT 0.0,
                            decimals     INTEGER NOT NULL DEFAULT 2,
                            max_balance  REAL NOT NULL DEFAULT -1
                        )""");
            }
            plugin.getLogger().info("Connected to SQLite database.");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to connect to SQLite: " + e.getMessage());
        }
    }

    @Override
    public void disconnect() {
        if (conn == null) return;
        try {
            if (!conn.isClosed()) conn.close();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error closing SQLite connection: " + e.getMessage());
        }
    }

    @Override
    public double getBalance(String uuid, String currency, double defaultBalance) {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT balance FROM balances WHERE uuid = ? AND currency = ?")) {
            ps.setString(1, uuid);
            ps.setString(2, currency);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getDouble("balance");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to get balance.", e);
        }
        return defaultBalance;
    }

    @Override
    public void setBalance(String uuid, String currency, double amount) {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT OR REPLACE INTO balances (uuid, currency, balance) VALUES (?, ?, ?)")) {
            ps.setString(1, uuid);
            ps.setString(2, currency);
            ps.setDouble(3, amount);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to set balance.", e);
        }
    }

    @Override
    public List<Map.Entry<String, Double>> getTopBalances(String currency, int limit) {
        List<Map.Entry<String, Double>> result = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT uuid, balance FROM balances WHERE currency = ? ORDER BY balance DESC LIMIT ?")) {
            ps.setString(1, currency);
            ps.setInt(2, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) result.add(Map.entry(rs.getString("uuid"), rs.getDouble("balance")));
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to get top balances.", e);
        }
        return result;
    }

    @Override
    public List<DynamicCurrencyRow> loadDynamicCurrencies() {
        List<DynamicCurrencyRow> list = new ArrayList<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT name, display_name, symbol, singular, plural, default_bal, decimals, max_balance FROM currencies")) {
            while (rs.next()) {
                list.add(new DynamicCurrencyRow(
                        rs.getString("name"),
                        rs.getString("display_name"),
                        rs.getString("symbol"),
                        rs.getString("singular"),
                        rs.getString("plural"),
                        rs.getDouble("default_bal"),
                        rs.getInt("decimals"),
                        rs.getDouble("max_balance")
                ));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to load dynamic currencies.", e);
        }
        return list;
    }

    @Override
    public void saveDynamicCurrency(String name, String displayName, String symbol,
                                    String singular, String plural,
                                    double defaultBalance, int decimals, double maxBalance) {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT OR REPLACE INTO currencies (name, display_name, symbol, singular, plural, default_bal, decimals, max_balance) VALUES (?,?,?,?,?,?,?,?)")) {
            ps.setString(1, name);
            ps.setString(2, displayName);
            ps.setString(3, symbol);
            ps.setString(4, singular);
            ps.setString(5, plural);
            ps.setDouble(6, defaultBalance);
            ps.setInt(7, decimals);
            ps.setDouble(8, maxBalance);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to save dynamic currency.", e);
        }
    }

    @Override
    public void deleteDynamicCurrency(String name) {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM currencies WHERE name = ?")) {
            ps.setString(1, name);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to delete dynamic currency.", e);
        }
    }

    @Override
    public boolean isDynamicCurrency(String name) {
        try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM currencies WHERE name = ?")) {
            ps.setString(1, name);
            return ps.executeQuery().next();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to check dynamic currency.", e);
            return false;
        }
    }
}
