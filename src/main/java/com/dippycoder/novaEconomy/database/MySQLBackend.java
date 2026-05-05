package com.dippycoder.novaEconomy.database;

import com.dippycoder.novaEconomy.NovaEconomy;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.*;
import java.util.*;
import java.util.logging.Level;

public class MySQLBackend implements StorageBackend {

    private final NovaEconomy plugin;
    private HikariDataSource dataSource;

    public MySQLBackend(NovaEconomy plugin) {
        this.plugin = plugin;
    }

    @Override
    public void connect() {
        var cfg = plugin.getConfigManager();
        HikariConfig hCfg = new HikariConfig();
        hCfg.setPoolName("NovaEconomy-MySQL");
        hCfg.setJdbcUrl("jdbc:mysql://" + cfg.getMysqlHost() + ":" + cfg.getMysqlPort()
                + "/" + cfg.getMysqlDatabase());
        hCfg.setUsername(cfg.getMysqlUsername());
        hCfg.setPassword(cfg.getMysqlPassword());
        hCfg.setMaximumPoolSize(cfg.getMysqlPoolSize());
        hCfg.setConnectionTimeout(cfg.getMysqlConnectionTimeout());
        hCfg.addDataSourceProperty("createDatabaseIfNotExist", "true");
        hCfg.addDataSourceProperty("useSSL", "false");
        hCfg.addDataSourceProperty("allowPublicKeyRetrieval", "true");
        hCfg.addDataSourceProperty("serverTimezone", "UTC");
        hCfg.addDataSourceProperty("characterEncoding", "utf8");

        try {
            dataSource = new HikariDataSource(hCfg);
            try (Connection conn = dataSource.getConnection();
                 Statement st = conn.createStatement()) {
                st.execute("""
                        CREATE TABLE IF NOT EXISTS balances (
                            uuid     VARCHAR(36)  NOT NULL,
                            currency VARCHAR(64)  NOT NULL,
                            balance  DOUBLE       NOT NULL DEFAULT 0.0,
                            PRIMARY KEY (uuid, currency)
                        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4""");
                st.execute("""
                        CREATE TABLE IF NOT EXISTS currencies (
                            name         VARCHAR(64)  NOT NULL PRIMARY KEY,
                            display_name VARCHAR(255) NOT NULL,
                            symbol       VARCHAR(16)  NOT NULL,
                            singular     VARCHAR(255) NOT NULL,
                            plural       VARCHAR(255) NOT NULL,
                            default_bal  DOUBLE       NOT NULL DEFAULT 0.0,
                            decimals     INT          NOT NULL DEFAULT 2,
                            max_balance  DOUBLE       NOT NULL DEFAULT -1
                        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4""");
            }
            plugin.getLogger().info("Connected to MySQL database ("
                    + cfg.getMysqlHost() + ":" + cfg.getMysqlPort() + "/" + cfg.getMysqlDatabase() + ").");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to connect to MySQL: " + e.getMessage());
        }
    }

    @Override
    public void disconnect() {
        if (dataSource != null && !dataSource.isClosed()) dataSource.close();
    }

    @Override
    public double getBalance(String uuid, String currency, double defaultBalance) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
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
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO balances (uuid, currency, balance) VALUES (?,?,?) "
                     + "ON DUPLICATE KEY UPDATE balance = VALUES(balance)")) {
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
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
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
        try (Connection conn = dataSource.getConnection();
             Statement st = conn.createStatement();
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
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO currencies (name, display_name, symbol, singular, plural, default_bal, decimals, max_balance) "
                     + "VALUES (?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE "
                     + "display_name=VALUES(display_name), symbol=VALUES(symbol), "
                     + "singular=VALUES(singular), plural=VALUES(plural), "
                     + "default_bal=VALUES(default_bal), decimals=VALUES(decimals), max_balance=VALUES(max_balance)")) {
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
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM currencies WHERE name = ?")) {
            ps.setString(1, name);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to delete dynamic currency.", e);
        }
    }

    @Override
    public boolean isDynamicCurrency(String name) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM currencies WHERE name = ?")) {
            ps.setString(1, name);
            return ps.executeQuery().next();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to check dynamic currency.", e);
            return false;
        }
    }
}
