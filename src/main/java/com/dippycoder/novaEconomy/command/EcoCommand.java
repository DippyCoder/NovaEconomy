package com.dippycoder.novaEconomy.command;

import com.dippycoder.novaEconomy.NovaEconomy;
import com.dippycoder.novaEconomy.manager.Currency;
import com.dippycoder.novaEconomy.util.AmountUtil;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.OptionalDouble;

public class EcoCommand extends BaseCommand {

    private static final String PREFIX = "<dark_gray>[<gold>⚡</gold><dark_gray>]";
    private static final MiniMessage MM = MiniMessage.miniMessage();

    public EcoCommand(NovaEconomy plugin) {
        super(plugin);
    }

    @Override
    protected void execute(CommandSender sender, Command command, String label, String[] args) {
        if (!requirePermission(sender, "cmd.eco")) return;

        if (args.length == 0) {
            sendHelp(sender, label);
            return;
        }

        switch (args[0].toLowerCase()) {
            case "currency"         -> handleCurrency(sender, args);
            case "set"              -> handleSet(sender, args);
            case "give"             -> handleGive(sender, args);
            case "take"             -> handleTake(sender, args);
            case "reset"            -> handleReset(sender, args);
            case "top", "baltop"    -> handleTop(sender, args);
            default                 -> sendHelp(sender, label);
        }
    }

    // ── /eco currency ─────────────────────────────────────────

    private void handleCurrency(CommandSender sender, String[] args) {
        if (!requirePermission(sender, "cmd.eco.currency")) return;
        if (args.length < 2) {
            msg.send(sender, "general.invalid-args", "usage", "/eco currency <add|remove|list>");
            return;
        }
        switch (args[1].toLowerCase()) {
            case "add"    -> handleCurrencyAdd(sender, args);
            case "remove" -> handleCurrencyRemove(sender, args);
            case "list"   -> handleCurrencyList(sender);
            default       -> msg.send(sender, "general.invalid-args", "usage", "/eco currency <add|remove|list>");
        }
    }

    private void handleCurrencyAdd(CommandSender sender, String[] args) {
        if (args.length < 3) {
            msg.send(sender, "general.invalid-args", "usage", "/eco currency add <name>");
            return;
        }
        String name = args[2].toUpperCase();
        if (plugin.getCurrencyManager().exists(name)) {
            msg.send(sender, "currency.already-exists", "currency", name);
            return;
        }
        plugin.getCurrencyManager().addDynamic(name);
        msg.send(sender, "currency.added", "currency", name);
    }

    private void handleCurrencyRemove(CommandSender sender, String[] args) {
        if (args.length < 3) {
            msg.send(sender, "general.invalid-args", "usage", "/eco currency remove <name>");
            return;
        }
        String name = args[2].toUpperCase();
        if (!plugin.getCurrencyManager().exists(name)) {
            msg.send(sender, "currency.not-found", "currency", name);
            return;
        }
        if (plugin.getCurrencyManager().isConfigDefined(name)) {
            msg.send(sender, "currency.config-protected", "currency", name);
            return;
        }
        plugin.getCurrencyManager().removeDynamic(name);
        msg.send(sender, "currency.removed", "currency", name);
    }

    private void handleCurrencyList(CommandSender sender) {
        List<Currency> all = plugin.getCurrencyManager().getAll().stream().toList();
        if (all.isEmpty()) {
            msg.send(sender, "currency.list-empty");
            return;
        }
        msg.send(sender, "currency.list-header");
        for (Currency c : all) {
            msg.send(sender, "currency.list-entry", "name", c.name(), "display", c.displayName());
        }
    }

    // ── /eco set ──────────────────────────────────────────────

    private void handleSet(CommandSender sender, String[] args) {
        if (!requirePermission(sender, "cmd.eco.set")) return;
        if (args.length < 4) {
            msg.send(sender, "general.invalid-args", "usage", "/eco set <player> <currency> <amount>");
            return;
        }
        OfflinePlayer target = findOfflinePlayer(sender, args[1]);
        if (target == null) return;

        String currencyName = args[2].toUpperCase();
        if (!plugin.getCurrencyManager().exists(currencyName)) {
            msg.send(sender, "currency.not-found", "currency", currencyName);
            return;
        }

        OptionalDouble parsed = AmountUtil.parse(args[3]);
        if (parsed.isEmpty()) {
            msg.send(sender, "general.invalid-number", "value", args[3]);
            return;
        }

        Currency c = plugin.getCurrencyManager().get(currencyName).orElseThrow();
        double amount = parsed.getAsDouble();
        if (!c.isWithinLimit(amount)) {
            msg.send(sender, "eco.max-balance", "currency", c.displayName());
            return;
        }

        plugin.getEconomyManager().setBalance(target.getUniqueId(), currencyName, amount);
        String formatted = formatAmount(amount, c);
        msg.send(sender, "eco.set", "player", target.getName(), "currency", c.displayName(), "amount", formatted);
        plugin.getDiscordWebhookManager().onSet(target.getName(), c.name(), amount, sender.getName());
    }

    // ── /eco give ─────────────────────────────────────────────

    private void handleGive(CommandSender sender, String[] args) {
        if (!requirePermission(sender, "cmd.eco.give")) return;
        if (args.length < 4) {
            msg.send(sender, "general.invalid-args", "usage", "/eco give <player> <currency> <amount>");
            return;
        }
        OfflinePlayer target = findOfflinePlayer(sender, args[1]);
        if (target == null) return;

        String currencyName = args[2].toUpperCase();
        if (!plugin.getCurrencyManager().exists(currencyName)) {
            msg.send(sender, "currency.not-found", "currency", currencyName);
            return;
        }

        OptionalDouble parsed = AmountUtil.parse(args[3]);
        if (parsed.isEmpty()) {
            msg.send(sender, "general.invalid-number", "value", args[3]);
            return;
        }

        Currency c = plugin.getCurrencyManager().get(currencyName).orElseThrow();
        double amount = parsed.getAsDouble();
        double current = plugin.getEconomyManager().getBalance(target.getUniqueId(), currencyName);

        if (!c.isWithinLimit(current + amount)) {
            msg.send(sender, "eco.max-balance", "currency", c.displayName());
            return;
        }

        plugin.getEconomyManager().deposit(target.getUniqueId(), currencyName, amount);
        String formatted = formatAmount(amount, c);
        msg.send(sender, "eco.give", "player", target.getName(), "currency", c.displayName(), "amount", formatted);
        plugin.getDiscordWebhookManager().onGive(target.getName(), c.name(), amount, sender.getName());
    }

    // ── /eco take ─────────────────────────────────────────────

    private void handleTake(CommandSender sender, String[] args) {
        if (!requirePermission(sender, "cmd.eco.take")) return;
        if (args.length < 4) {
            msg.send(sender, "general.invalid-args", "usage", "/eco take <player> <currency> <amount>");
            return;
        }
        OfflinePlayer target = findOfflinePlayer(sender, args[1]);
        if (target == null) return;

        String currencyName = args[2].toUpperCase();
        if (!plugin.getCurrencyManager().exists(currencyName)) {
            msg.send(sender, "currency.not-found", "currency", currencyName);
            return;
        }

        OptionalDouble parsed = AmountUtil.parse(args[3]);
        if (parsed.isEmpty()) {
            msg.send(sender, "general.invalid-number", "value", args[3]);
            return;
        }

        Currency c = plugin.getCurrencyManager().get(currencyName).orElseThrow();
        double amount = parsed.getAsDouble();
        plugin.getEconomyManager().withdraw(target.getUniqueId(), currencyName, amount);
        String formatted = formatAmount(amount, c);
        msg.send(sender, "eco.take", "player", target.getName(), "currency", c.displayName(), "amount", formatted);
        plugin.getDiscordWebhookManager().onTake(target.getName(), c.name(), amount, sender.getName());
    }

    // ── /eco reset ────────────────────────────────────────────

    private void handleReset(CommandSender sender, String[] args) {
        if (!requirePermission(sender, "cmd.eco.reset")) return;
        if (args.length < 3) {
            msg.send(sender, "general.invalid-args", "usage", "/eco reset <player> <currency>");
            return;
        }
        OfflinePlayer target = findOfflinePlayer(sender, args[1]);
        if (target == null) return;

        String currencyName = args[2].toUpperCase();
        if (!plugin.getCurrencyManager().exists(currencyName)) {
            msg.send(sender, "currency.not-found", "currency", currencyName);
            return;
        }

        Currency c = plugin.getCurrencyManager().get(currencyName).orElseThrow();
        plugin.getEconomyManager().resetBalance(target.getUniqueId(), currencyName);
        msg.send(sender, "eco.reset", "player", target.getName(), "currency", c.displayName());
        plugin.getDiscordWebhookManager().onReset(target.getName(), c.name(), sender.getName());
    }

    // ── /eco top ──────────────────────────────────────────────

    private void handleTop(CommandSender sender, String[] args) {
        if (!requirePermission(sender, "cmd.eco.top")) return;

        String currencyName = args.length >= 2
                ? args[1].toUpperCase()
                : plugin.getConfigManager().getDefaultCurrency();

        if (!plugin.getCurrencyManager().exists(currencyName)) {
            msg.send(sender, "currency.not-found", "currency", currencyName);
            return;
        }

        Currency c = plugin.getCurrencyManager().get(currencyName).orElseThrow();
        int page = 1;
        if (args.length >= 3) {
            try { page = Math.max(1, Integer.parseInt(args[2])); }
            catch (NumberFormatException ignored) {
                msg.send(sender, "baltop.invalid-page");
                return;
            }
        }

        int perPage = 10;
        var top = plugin.getEconomyManager().getTopBalances(currencyName, perPage * page);
        int fromIdx = (page - 1) * perPage;
        if (fromIdx >= top.size() && !top.isEmpty()) {
            msg.send(sender, "baltop.invalid-page");
            return;
        }

        if (top.isEmpty()) {
            msg.send(sender, "baltop.empty", "currency", c.displayName());
            return;
        }

        int totalPages = (int) Math.ceil(top.size() / (double) perPage);
        msg.send(sender, "baltop.header", "currency", c.displayName());

        List<java.util.Map.Entry<String, Double>> page_entries = top.subList(fromIdx, Math.min(fromIdx + perPage, top.size()));
        int rank = fromIdx + 1;
        for (var entry : page_entries) {
            String name = resolvePlayerName(entry.getKey());
            String formatted = formatAmount(entry.getValue(), c);
            msg.send(sender, "baltop.entry", "rank", rank, "player", name, "amount", formatted);
            rank++;
        }
        msg.send(sender, "baltop.footer", "page", page, "pages", totalPages);
    }

    // ── Help ──────────────────────────────────────────────────

    private void sendHelp(CommandSender sender, String label) {
        send(sender, PREFIX + " <gray>Economy <dark_gray>— <white>/" + label + " help</white>");
        send(sender, "<dark_gray>" + "─".repeat(44));
        send(sender, "<gold>/" + label + " currency add <name></gold> <dark_gray>— <gray>Create a new currency");
        send(sender, "<gold>/" + label + " currency remove <name></gold> <dark_gray>— <gray>Remove a dynamic currency");
        send(sender, "<gold>/" + label + " currency list</gold> <dark_gray>— <gray>List all currencies");
        send(sender, "<gold>/" + label + " set <player> <currency> <amount></gold> <dark_gray>— <gray>Set a player's balance");
        send(sender, "<gold>/" + label + " give <player> <currency> <amount></gold> <dark_gray>— <gray>Give a player money");
        send(sender, "<gold>/" + label + " take <player> <currency> <amount></gold> <dark_gray>— <gray>Take money from a player");
        send(sender, "<gold>/" + label + " reset <player> <currency></gold> <dark_gray>— <gray>Reset a player's balance");
        send(sender, "<gold>/" + label + " top [currency] [page]</gold> <dark_gray>— <gray>View balance leaderboard");
        send(sender, "<dark_gray>" + "─".repeat(44));
    }

    private void send(CommandSender sender, String miniMessage) {
        sender.sendMessage(MM.deserialize(miniMessage));
    }

    // ── Tab completion ────────────────────────────────────────

    @Override
    protected List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return filterPrefix(List.of("currency", "set", "give", "take", "reset", "top"), args[0]);
        }

        return switch (args[0].toLowerCase()) {
            case "currency" -> {
                if (args.length == 2) yield filterPrefix(List.of("add", "remove", "list"), args[1]);
                if (args.length == 3 && args[1].equalsIgnoreCase("remove"))
                    yield filterPrefix(plugin.getCurrencyManager().getNames(), args[2]);
                yield List.of();
            }
            case "set", "give", "take" -> {
                if (args.length == 2) yield filterPrefix(onlinePlayerNames(), args[1]);
                if (args.length == 3) yield filterPrefix(plugin.getCurrencyManager().getNames(), args[2]);
                if (args.length == 4) yield filterPrefix(List.of("100", "1k", "10k", "100k", "1m"), args[3]);
                yield List.of();
            }
            case "reset" -> {
                if (args.length == 2) yield filterPrefix(onlinePlayerNames(), args[1]);
                if (args.length == 3) yield filterPrefix(plugin.getCurrencyManager().getNames(), args[2]);
                yield List.of();
            }
            case "top", "baltop" -> {
                if (args.length == 2) yield filterPrefix(plugin.getCurrencyManager().getNames(), args[1]);
                yield List.of();
            }
            default -> List.of();
        };
    }

    // ── Utilities ─────────────────────────────────────────────

    private String formatAmount(double amount, Currency c) {
        return plugin.getConfigManager().isShortAmountFormatting()
                ? AmountUtil.formatShort(amount, c.decimals())
                : AmountUtil.formatFull(amount, c.decimals());
    }

    @SuppressWarnings("deprecation")
    private String resolvePlayerName(String uuid) {
        try {
            org.bukkit.OfflinePlayer op = plugin.getServer().getOfflinePlayer(java.util.UUID.fromString(uuid));
            String name = op.getName();
            return name != null ? name : uuid;
        } catch (Exception e) {
            return uuid;
        }
    }
}
