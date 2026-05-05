package com.dippycoder.novaEconomy.database;

import com.dippycoder.novaEconomy.NovaEconomy;
import redis.clients.jedis.*;
import redis.clients.jedis.resps.Tuple;

import java.util.*;
import java.util.logging.Level;

public class RedisBackend implements StorageBackend {

    private final NovaEconomy plugin;
    private JedisPool pool;
    private String prefix;

    public RedisBackend(NovaEconomy plugin) {
        this.plugin = plugin;
    }

    @Override
    public void connect() {
        var cfg = plugin.getConfigManager();
        prefix = cfg.getRedisKeyPrefix();

        JedisPoolConfig poolCfg = new JedisPoolConfig();
        poolCfg.setMaxTotal(cfg.getRedisPoolSize());
        poolCfg.setTestOnBorrow(true);

        String host = cfg.getRedisHost();
        int port = cfg.getRedisPort();
        String password = cfg.getRedisPassword();
        int db = cfg.getRedisDatabase();

        if (password.isBlank()) {
            pool = new JedisPool(poolCfg, host, port, Protocol.DEFAULT_TIMEOUT, null, db);
        } else {
            pool = new JedisPool(poolCfg, host, port, Protocol.DEFAULT_TIMEOUT, password, db);
        }

        // Validate connection
        try (Jedis j = pool.getResource()) {
            j.ping();
        }
        plugin.getLogger().info("Connected to Redis at " + host + ":" + port + " (db " + db + ").");
    }

    @Override
    public void disconnect() {
        if (pool != null && !pool.isClosed()) pool.close();
    }

    // ── Key helpers ───────────────────────────────────────────

    private String balKey(String uuid)       { return prefix + ":b:" + uuid; }
    private String btopKey(String currency)  { return prefix + ":bt:" + currency.toUpperCase(); }
    private String curKey(String name)       { return prefix + ":cur:" + name.toUpperCase(); }
    private String cursSetKey()              { return prefix + ":curs"; }

    // ── Balances ──────────────────────────────────────────────

    @Override
    public double getBalance(String uuid, String currency, double defaultBalance) {
        try (Jedis j = pool.getResource()) {
            String val = j.hget(balKey(uuid), currency.toUpperCase());
            return val == null ? defaultBalance : Double.parseDouble(val);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to get balance from Redis.", e);
            return defaultBalance;
        }
    }

    @Override
    public void setBalance(String uuid, String currency, double amount) {
        try (Jedis j = pool.getResource()) {
            String cur = currency.toUpperCase();
            Pipeline pipe = j.pipelined();
            pipe.hset(balKey(uuid), cur, String.valueOf(amount));
            pipe.zadd(btopKey(cur), amount, uuid);
            pipe.sync();
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to set balance in Redis.", e);
        }
    }

    @Override
    public List<Map.Entry<String, Double>> getTopBalances(String currency, int limit) {
        List<Map.Entry<String, Double>> result = new ArrayList<>();
        try (Jedis j = pool.getResource()) {
            List<Tuple> tuples = j.zrevrangeWithScores(btopKey(currency), 0, limit - 1);
            for (Tuple t : tuples) result.add(Map.entry(t.getElement(), t.getScore()));
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to get top balances from Redis.", e);
        }
        return result;
    }

    // ── Dynamic Currencies ────────────────────────────────────

    @Override
    public List<DynamicCurrencyRow> loadDynamicCurrencies() {
        List<DynamicCurrencyRow> list = new ArrayList<>();
        try (Jedis j = pool.getResource()) {
            Set<String> names = j.smembers(cursSetKey());
            for (String name : names) {
                Map<String, String> fields = j.hgetAll(curKey(name));
                if (fields.isEmpty()) continue;
                list.add(new DynamicCurrencyRow(
                        name,
                        fields.getOrDefault("display_name", name),
                        fields.getOrDefault("symbol", "$"),
                        fields.getOrDefault("singular", name),
                        fields.getOrDefault("plural", name + "s"),
                        parseDouble(fields.get("default_bal"), 0.0),
                        parseInt(fields.get("decimals"), 2),
                        parseDouble(fields.get("max_balance"), -1.0)
                ));
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to load dynamic currencies from Redis.", e);
        }
        return list;
    }

    @Override
    public void saveDynamicCurrency(String name, String displayName, String symbol,
                                    String singular, String plural,
                                    double defaultBalance, int decimals, double maxBalance) {
        try (Jedis j = pool.getResource()) {
            String key = name.toUpperCase();
            Map<String, String> fields = new HashMap<>();
            fields.put("display_name", displayName);
            fields.put("symbol", symbol);
            fields.put("singular", singular);
            fields.put("plural", plural);
            fields.put("default_bal", String.valueOf(defaultBalance));
            fields.put("decimals", String.valueOf(decimals));
            fields.put("max_balance", String.valueOf(maxBalance));

            Pipeline pipe = j.pipelined();
            pipe.hset(curKey(key), fields);
            pipe.sadd(cursSetKey(), key);
            pipe.sync();
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to save dynamic currency in Redis.", e);
        }
    }

    @Override
    public void deleteDynamicCurrency(String name) {
        try (Jedis j = pool.getResource()) {
            String key = name.toUpperCase();
            Pipeline pipe = j.pipelined();
            pipe.del(curKey(key));
            pipe.srem(cursSetKey(), key);
            pipe.sync();
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to delete dynamic currency from Redis.", e);
        }
    }

    @Override
    public boolean isDynamicCurrency(String name) {
        try (Jedis j = pool.getResource()) {
            return j.sismember(cursSetKey(), name.toUpperCase());
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to check dynamic currency in Redis.", e);
            return false;
        }
    }

    // ── Helpers ───────────────────────────────────────────────

    private double parseDouble(String s, double def) {
        if (s == null) return def;
        try { return Double.parseDouble(s); } catch (NumberFormatException e) { return def; }
    }

    private int parseInt(String s, int def) {
        if (s == null) return def;
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return def; }
    }
}
