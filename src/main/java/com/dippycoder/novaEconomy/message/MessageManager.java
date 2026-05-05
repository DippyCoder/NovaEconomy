package com.dippycoder.novaEconomy.message;

import com.dippycoder.novaEconomy.NovaEconomy;
import com.dippycoder.novaEconomy.util.SmallCapsUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class MessageManager {

    private static final String[] PREFIX_KEYS = { "eco" };

    private final NovaEconomy plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();

    private final Map<String, FileConfiguration> localeCache = new ConcurrentHashMap<>();
    private FileConfiguration defaultMessages;
    private boolean papiLoaded;

    public MessageManager(NovaEconomy plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        localeCache.clear();
        papiLoaded = plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null;

        String defaultLang = plugin.getConfigManager().getLanguage();
        defaultMessages = loadLocale(defaultLang);
        localeCache.put(defaultLang.toLowerCase(), defaultMessages);

        File messagesDir = new File(plugin.getDataFolder(), "messages");
        if (messagesDir.isDirectory()) {
            File[] files = messagesDir.listFiles((d, n) -> n.endsWith(".yml"));
            if (files != null) {
                for (File f : files) {
                    String key = f.getName().replace(".yml", "").toLowerCase();
                    localeCache.computeIfAbsent(key, k -> YamlConfiguration.loadConfiguration(f));
                }
            }
        }
    }

    // ── Public API ────────────────────────────────────────────

    public void send(CommandSender sender, String key, Object... kvPairs) {
        Component component = get(sender, key, kvPairs);
        if (component != null) sender.sendMessage(component);
    }

    public Component get(CommandSender sender, String key, Object... kvPairs) {
        FileConfiguration messages = getMessagesFor(sender);
        String raw = messages.getString(key);
        if (raw == null) {
            plugin.getLogger().warning("Missing message key: " + key);
            return Component.text("(missing: " + key + ")");
        }

        if (papiLoaded && sender instanceof Player player) {
            raw = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, raw);
        }

        List<TagResolver> resolvers = buildResolvers(messages, kvPairs);
        Component component = mm.deserialize(raw, TagResolver.resolver(resolvers));

        if (shouldApplySmallCaps(sender)) {
            component = SmallCapsUtil.applyToComponent(component);
        }
        return component;
    }

    public Component parse(CommandSender sender, String raw, Object... kvPairs) {
        FileConfiguration messages = getMessagesFor(sender);
        if (papiLoaded && sender instanceof Player player) {
            raw = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, raw);
        }
        return mm.deserialize(raw, TagResolver.resolver(buildResolvers(messages, kvPairs)));
    }

    public String getRaw(String key) {
        return defaultMessages.getString(key, "");
    }

    // ── Locale resolution ─────────────────────────────────────

    private FileConfiguration getMessagesFor(CommandSender sender) {
        if (!(sender instanceof Player player)) return defaultMessages;

        Locale locale = player.locale();
        String lang    = locale.getLanguage().toLowerCase();
        String country = locale.getCountry().toLowerCase();

        if (!country.isEmpty()) {
            FileConfiguration fc = localeCache.computeIfAbsent(
                    lang + "_" + country, k -> tryLoadLocale(lang + "_" + country));
            if (fc != null) return fc;
        }

        FileConfiguration fc = localeCache.computeIfAbsent(lang, k -> tryLoadLocale(lang));
        return fc != null ? fc : defaultMessages;
    }

    private FileConfiguration tryLoadLocale(String key) {
        File f = new File(plugin.getDataFolder(), "messages/" + key + ".yml");
        if (f.exists()) return YamlConfiguration.loadConfiguration(f);
        InputStream is = plugin.getResource("messages/" + key + ".yml");
        if (is != null) {
            plugin.saveResource("messages/" + key + ".yml", false);
            return YamlConfiguration.loadConfiguration(
                    new InputStreamReader(is, StandardCharsets.UTF_8));
        }
        return null;
    }

    private FileConfiguration loadLocale(String lang) {
        File file = new File(plugin.getDataFolder(), "messages/" + lang + ".yml");
        if (!file.exists()) plugin.saveResource("messages/" + lang + ".yml", false);
        if (file.exists()) return YamlConfiguration.loadConfiguration(file);
        InputStream is = plugin.getResource("messages/en.yml");
        if (is == null) {
            plugin.getLogger().warning("No messages file found; using empty config.");
            return new YamlConfiguration();
        }
        return YamlConfiguration.loadConfiguration(
                new InputStreamReader(is, StandardCharsets.UTF_8));
    }

    // ── Tag resolvers ─────────────────────────────────────────

    private List<TagResolver> buildResolvers(FileConfiguration messages, Object[] kvPairs) {
        List<TagResolver> resolvers = new ArrayList<>();

        for (String prefix : PREFIX_KEYS) {
            String val = messages.getString("prefix." + prefix, "");
            resolvers.add(Placeholder.component("prefix_" + prefix, mm.deserialize(val)));
        }

        for (int i = 0; i + 1 < kvPairs.length; i += 2) {
            String tagKey = String.valueOf(kvPairs[i]);
            Object tagVal = kvPairs[i + 1];
            if (tagVal instanceof Component c) {
                resolvers.add(Placeholder.component(tagKey, c));
            } else {
                resolvers.add(Placeholder.unparsed(tagKey, String.valueOf(tagVal)));
            }
        }
        return resolvers;
    }

    private boolean shouldApplySmallCaps(CommandSender sender) {
        if (!plugin.getConfigManager().isSmallCapsEnabled()) return false;
        if (!plugin.getConfigManager().isSmallCapsPermissionRequired()) return true;
        return sender.hasPermission(plugin.getConfigManager().getPermission("chat.smallcaps"));
    }

    public void broadcastAll(String key, Object... kvPairs) {
        for (Player p : plugin.getServer().getOnlinePlayers()) send(p, key, kvPairs);
    }

    public boolean isPapiLoaded() { return papiLoaded; }
}
