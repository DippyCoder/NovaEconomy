package com.dippycoder.novaEconomy.manager;

import com.dippycoder.novaEconomy.NovaEconomy;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UpdateChecker {

    private static final String API_URL =
            "https://api.github.com/repos/dippycoder/novaeconomy/releases?per_page=30";
    private static final String RELEASES_URL =
            "https://github.com/dippycoder/novaeconomy/releases/latest";
    private static final String PREFIX = "<dark_gray>[<gold>⚡</gold><dark_gray>]";

    private final NovaEconomy plugin;
    private final String currentVersion;

    private volatile boolean updateAvailable = false;
    private volatile String latestVersion    = null;

    @SuppressWarnings("deprecation")
    public UpdateChecker(NovaEconomy plugin) {
        this.plugin = plugin;
        this.currentVersion = plugin.getDescription().getVersion();
    }

    public void checkAsync() {
        if (!plugin.getConfigManager().isCheckForUpdates()) return;
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin,
                () -> runCheck(Bukkit.getMinecraftVersion(), null));
    }

    public void checkAsyncForSender(CommandSender sender) {
        MiniMessage mm = MiniMessage.miniMessage();
        String mcVersion = Bukkit.getMinecraftVersion();
        sender.sendMessage(mm.deserialize(PREFIX + " <gray>Checking for updates on <white>ver/"
                + mcVersion + "</white><gray>..."));
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin,
                () -> runCheck(mcVersion, sender));
    }

    // ── Core ──────────────────────────────────────────────────

    private void runCheck(String mcVersion, CommandSender sender) {
        MiniMessage mm = MiniMessage.miniMessage();
        String branch = "ver/" + mcVersion;
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "NovaEconomy-UpdateChecker")
                    .build();
            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                String msg = "Update check returned HTTP " + response.statusCode();
                plugin.getLogger().warning(msg);
                if (sender != null) sender.sendMessage(mm.deserialize(PREFIX + " <red>" + msg));
                return;
            }

            String tag = findTagForBranch(response.body(), branch);
            if (tag == null) {
                String msg = "No release found for branch " + branch + ".";
                plugin.getLogger().info(msg);
                if (sender != null) sender.sendMessage(mm.deserialize(PREFIX + " <gray>" + msg));
                return;
            }

            String stripped = tag.startsWith("v") ? tag.substring(1) : tag;
            latestVersion   = stripped;
            updateAvailable = !currentVersion.equals(latestVersion);

            if (updateAvailable) {
                plugin.getLogger().warning("A new version of NovaEconomy is available"
                        + " (" + branch + "): " + latestVersion
                        + " (you have " + currentVersion + ")");
                plugin.getLogger().warning("Download: " + RELEASES_URL);
                if (sender != null) {
                    sender.sendMessage(mm.deserialize(
                            PREFIX + " <yellow>Update available <dark_gray>(" + branch + ")<yellow>: "
                            + "<green><bold>v" + latestVersion + "</bold></green>"
                            + " <dark_gray>» <gray>you have <white>v" + currentVersion + "</white>"
                            + "\n" + PREFIX + " <gray><click:open_url:'" + RELEASES_URL + "'>"
                            + "<aqua><underlined>Click to download</underlined></aqua></click>"));
                }
            } else {
                String msg = "NovaEconomy is up to date (v" + currentVersion + " on " + branch + ").";
                plugin.getLogger().info(msg);
                if (sender != null) {
                    sender.sendMessage(mm.deserialize(
                            PREFIX + " <green>You are on the latest version "
                            + "<bold>v" + currentVersion + "</bold></green>"
                            + " <dark_gray>(" + branch + ")<gray>."));
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Update check failed: " + e.getMessage());
            if (sender != null)
                sender.sendMessage(mm.deserialize(PREFIX + " <red>Update check failed: " + e.getMessage()));
        }
    }

    // ── Parsing ───────────────────────────────────────────────

    private String findTagForBranch(String json, String branch) {
        List<String> tags        = extractAll(json, "\"tag_name\"\\s*:\\s*\"([^\"]+)\"");
        List<String> commitishes = extractAll(json, "\"target_commitish\"\\s*:\\s*\"([^\"]+)\"");
        for (int i = 0; i < Math.min(tags.size(), commitishes.size()); i++) {
            if (commitishes.get(i).equals(branch)) return tags.get(i);
        }
        return null;
    }

    private List<String> extractAll(String text, String regex) {
        List<String> results = new ArrayList<>();
        Matcher m = Pattern.compile(regex).matcher(text);
        while (m.find()) results.add(m.group(1));
        return results;
    }

    // ── Player notification on join ───────────────────────────

    public void notifyPlayer(Player player) {
        if (!updateAvailable || latestVersion == null) return;
        String branch = "ver/" + Bukkit.getMinecraftVersion();
        Component message = MiniMessage.miniMessage().deserialize(
                PREFIX + " <yellow>NovaEconomy update available"
                + " <dark_gray>(" + branch + ")<yellow>: "
                + "<green><bold>v" + latestVersion + "</bold></green>"
                + " <dark_gray>» <gray>you have <white>v" + currentVersion + "</white>"
                + "\n" + PREFIX + " <gray><click:open_url:'" + RELEASES_URL + "'>"
                + "<aqua><underlined>Click to download</underlined></aqua></click>");
        player.sendMessage(message);
    }

    public boolean isUpdateAvailable() { return updateAvailable; }
    public String  getLatestVersion()  { return latestVersion; }
}
