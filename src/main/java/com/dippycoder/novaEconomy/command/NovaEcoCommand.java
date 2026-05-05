package com.dippycoder.novaEconomy.command;

import com.dippycoder.novaEconomy.NovaEconomy;
import com.dippycoder.novaEconomy.config.ConfigManager.VaultMode;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NovaEcoCommand extends BaseCommand {

    private static final int PAGE_SIZE = 20;
    private static final String PREFIX = "<dark_gray>[<gold>⚡</gold><dark_gray>]";
    private static final String GITHUB_URL = "https://github.com/dippycoder/novaeconomy";
    private static final MiniMessage MM = MiniMessage.miniMessage();

    public NovaEcoCommand(NovaEconomy plugin) {
        super(plugin);
    }

    @Override
    protected void execute(CommandSender sender, Command command, String label, String[] args) {
        if (!requirePermission(sender, "cmd.novaeco")) return;

        String sub = args.length > 0 ? args[0].toLowerCase() : "version";
        switch (sub) {
            case "version"            -> showVersion(sender);
            case "reload"             -> doReload(sender);
            case "update"             -> plugin.getUpdateChecker().checkAsyncForSender(sender);
            case "check-for-updates"  -> toggleCheckForUpdates(sender);
            case "help"               -> showHelp(sender, parsePage(args));
            default                   -> showVersion(sender);
        }
    }

    // ── Subcommands ───────────────────────────────────────────

    @SuppressWarnings("deprecation")
    private void showVersion(CommandSender sender) {
        String version    = plugin.getDescription().getVersion();
        String mcVersion  = Bukkit.getMinecraftVersion();
        String authors    = String.join(", ", plugin.getDescription().getAuthors());
        boolean checking  = plugin.getConfigManager().isCheckForUpdates();
        boolean hasUpdate = plugin.getUpdateChecker().isUpdateAvailable();
        String latestVer  = plugin.getUpdateChecker().getLatestVersion();

        send(sender, PREFIX + " <white><bold>NovaEconomy</bold></white> <dark_gray>v<gold>" + version + "</gold>");
        send(sender, PREFIX + " <gray>Platform <dark_gray>» <white>Paper " + mcVersion
                + "</white>  <dark_gray>|  <gray>Branch <dark_gray>» <white>ver/" + mcVersion + "</white>");
        send(sender, PREFIX + " <gray>Developer <dark_gray>» <aqua>" + authors
                + "</aqua>  <dark_gray>|  "
                + "<gray>GitHub <dark_gray>» <aqua><click:open_url:'" + GITHUB_URL + "'><underlined>"
                + GITHUB_URL + "</underlined></click></aqua>");
        send(sender, PREFIX + " <gray>Update checking <dark_gray>» "
                + (checking ? "<green>enabled" : "<red>disabled")
                + (hasUpdate
                    ? "  <dark_gray>|  <yellow>⚠ Update available: <bold>v" + latestVer + "</bold>"
                    : ""));
    }

    private void toggleCheckForUpdates(CommandSender sender) {
        boolean next = !plugin.getConfigManager().isCheckForUpdates();
        plugin.getConfigManager().setCheckForUpdates(next);
        msg.send(sender, next ? "novaeco.check-enabled" : "novaeco.check-disabled");
    }

    private void doReload(CommandSender sender) {
        if (!requirePermission(sender, "cmd.novaeco.reload")) return;
        plugin.getConfigManager().reload();
        plugin.getMessageManager().reload();
        plugin.getCurrencyManager().reload();
        plugin.getDiscordWebhookManager().reload();
        msg.send(sender, "general.reload-done");
    }

    @SuppressWarnings("deprecation")
    private void showHelp(CommandSender sender, int page) {
        Map<String, Map<String, Object>> cmds = plugin.getDescription().getCommands();
        List<Map.Entry<String, Map<String, Object>>> sorted = new ArrayList<>(cmds.entrySet());
        sorted.sort(Map.Entry.comparingByKey());

        int total   = sorted.size();
        int maxPage = Math.max(1, (int) Math.ceil((double) total / PAGE_SIZE));
        page = Math.min(Math.max(page, 1), maxPage);

        int from = (page - 1) * PAGE_SIZE;
        int to   = Math.min(from + PAGE_SIZE, total);

        send(sender, PREFIX + " <gray>Commands <dark_gray>— <white>page " + page
                + " of " + maxPage + "</white> <dark_gray>(" + total + " total)");
        send(sender, "<dark_gray>" + "─".repeat(44));

        for (int i = from; i < to; i++) {
            var entry = sorted.get(i);
            Object usageObj = entry.getValue().get("usage");
            Object descObj  = entry.getValue().get("description");
            String usage = usageObj != null ? String.valueOf(usageObj) : "/" + entry.getKey();
            String desc  = descObj  != null ? String.valueOf(descObj)  : "";
            send(sender, "<gold>" + usage + "</gold> <dark_gray>— <gray>" + desc);
        }

        send(sender, "<dark_gray>" + "─".repeat(44));

        boolean hasPrev = page > 1;
        boolean hasNext = page < maxPage;
        if (hasPrev || hasNext) {
            Component nav = Component.empty();
            if (hasPrev) nav = nav.append(MM.deserialize(
                    "<aqua><click:run_command:'/novaeconomy help " + (page - 1) + "'>[← Prev]</click></aqua>"));
            if (hasPrev && hasNext) nav = nav.append(Component.text("  "));
            if (hasNext) nav = nav.append(MM.deserialize(
                    "<aqua><click:run_command:'/novaeconomy help " + (page + 1) + "'>[Next →]</click></aqua>"));
            sender.sendMessage(nav);
        }
    }

    // ── Helpers ───────────────────────────────────────────────

    private void send(CommandSender sender, String miniMessage) {
        sender.sendMessage(MM.deserialize(miniMessage));
    }

    private int parsePage(String[] args) {
        if (args.length < 2) return 1;
        try { return Integer.parseInt(args[1]); } catch (NumberFormatException e) { return 1; }
    }

    @Override
    @SuppressWarnings("deprecation")
    protected List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1)
            return filterPrefix(List.of("version", "reload", "update", "check-for-updates", "help"), args[0]);

        if (args.length == 2 && args[0].equalsIgnoreCase("help")) {
            int total   = plugin.getDescription().getCommands().size();
            int maxPage = Math.max(1, (int) Math.ceil((double) total / PAGE_SIZE));
            List<String> pages = new ArrayList<>();
            for (int i = 1; i <= maxPage; i++) pages.add(String.valueOf(i));
            return filterPrefix(pages, args[1]);
        }
        return List.of();
    }
}
