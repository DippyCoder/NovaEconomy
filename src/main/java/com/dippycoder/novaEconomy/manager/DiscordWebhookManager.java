package com.dippycoder.novaEconomy.manager;

import com.dippycoder.novaEconomy.NovaEconomy;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class DiscordWebhookManager {

    private final NovaEconomy plugin;
    private final HttpClient http = HttpClient.newHttpClient();

    private boolean enabled;
    private String webhookUrl;

    private boolean eventSet;
    private boolean eventGive;
    private boolean eventTake;
    private boolean eventReset;
    private boolean eventPay;

    private EmbedConfig embedSet;
    private EmbedConfig embedGive;
    private EmbedConfig embedTake;
    private EmbedConfig embedReset;
    private EmbedConfig embedPay;

    public DiscordWebhookManager(NovaEconomy plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "webhooks.yml");
        if (!file.exists()) plugin.saveResource("webhooks.yml", false);

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        enabled    = cfg.getBoolean("enabled", false);
        webhookUrl = cfg.getString("webhook-url", "");

        eventSet   = cfg.getBoolean("events.set",   true);
        eventGive  = cfg.getBoolean("events.give",  true);
        eventTake  = cfg.getBoolean("events.take",  true);
        eventReset = cfg.getBoolean("events.reset", true);
        eventPay   = cfg.getBoolean("events.pay",   true);

        embedSet   = loadEmbed(cfg, "set");
        embedGive  = loadEmbed(cfg, "give");
        embedTake  = loadEmbed(cfg, "take");
        embedReset = loadEmbed(cfg, "reset");
        embedPay   = loadEmbed(cfg, "pay");
    }

    // ── Event hooks ───────────────────────────────────────────

    public void onSet(String player, String currency, double amount, String actor) {
        if (!enabled || !eventSet) return;
        send(embedSet, Map.of("player", player, "currency", currency,
                "amount", String.valueOf(amount), "actor", actor));
    }

    public void onGive(String player, String currency, double amount, String actor) {
        if (!enabled || !eventGive) return;
        send(embedGive, Map.of("player", player, "currency", currency,
                "amount", String.valueOf(amount), "actor", actor));
    }

    public void onTake(String player, String currency, double amount, String actor) {
        if (!enabled || !eventTake) return;
        send(embedTake, Map.of("player", player, "currency", currency,
                "amount", String.valueOf(amount), "actor", actor));
    }

    public void onReset(String player, String currency, String actor) {
        if (!enabled || !eventReset) return;
        send(embedReset, Map.of("player", player, "currency", currency, "actor", actor));
    }

    public void onPay(String sender, String receiver, String currency, double amount) {
        if (!enabled || !eventPay) return;
        send(embedPay, Map.of("sender", sender, "receiver", receiver,
                "currency", currency, "amount", String.valueOf(amount)));
    }

    // ── Config loading ────────────────────────────────────────

    private record FieldTemplate(String name, String value, boolean inline) {}
    private record EmbedConfig(String title, int color, List<FieldTemplate> fields) {}

    private EmbedConfig loadEmbed(YamlConfiguration cfg, String key) {
        String path  = "embeds." + key;
        String title = cfg.getString(path + ".title", key);
        int color    = parseColor(cfg.getString(path + ".color", "FFFFFF"));

        List<FieldTemplate> fields = new ArrayList<>();
        List<?> raw = cfg.getList(path + ".fields", List.of());
        for (Object obj : raw) {
            if (!(obj instanceof Map<?, ?> m)) continue;
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) m;
            String name    = String.valueOf(map.getOrDefault("name",   ""));
            String value   = String.valueOf(map.getOrDefault("value",  ""));
            boolean inline = Boolean.parseBoolean(String.valueOf(map.getOrDefault("inline", "true")));
            fields.add(new FieldTemplate(name, value, inline));
        }
        return new EmbedConfig(title, color, fields);
    }

    private static int parseColor(String hex) {
        try { return (int) Long.parseLong(hex.replace("#", ""), 16); }
        catch (NumberFormatException e) { return 0xFFFFFF; }
    }

    // ── Sending ───────────────────────────────────────────────

    private void send(EmbedConfig cfg, Map<String, String> vars) {
        if (webhookUrl == null || webhookUrl.isBlank()) return;

        StringBuilder json = new StringBuilder();
        json.append("{\"embeds\":[{");
        json.append("\"title\":\"").append(escape(substitute(cfg.title(), vars))).append("\",");
        json.append("\"color\":").append(cfg.color()).append(",");
        json.append("\"timestamp\":\"").append(Instant.now()).append("\",");
        json.append("\"fields\":[");

        List<FieldTemplate> fields = cfg.fields();
        for (int i = 0; i < fields.size(); i++) {
            FieldTemplate f = fields.get(i);
            json.append("{\"name\":\"").append(escape(substitute(f.name(), vars))).append("\",");
            json.append("\"value\":\"").append(escape(substitute(f.value(), vars))).append("\",");
            json.append("\"inline\":").append(f.inline()).append("}");
            if (i < fields.size() - 1) json.append(",");
        }
        json.append("]}]}");

        String body = json.toString();
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(webhookUrl))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();
                http.send(request, HttpResponse.BodyHandlers.discarding());
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Discord webhook failed: " + e.getMessage());
            }
        });
    }

    private static String substitute(String template, Map<String, String> vars) {
        String result = template;
        for (Map.Entry<String, String> entry : vars.entrySet())
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        return result;
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "");
    }
}
