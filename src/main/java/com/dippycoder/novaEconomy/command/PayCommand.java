package com.dippycoder.novaEconomy.command;

import com.dippycoder.novaEconomy.NovaEconomy;
import com.dippycoder.novaEconomy.manager.Currency;
import com.dippycoder.novaEconomy.util.AmountUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.OptionalDouble;

public class PayCommand extends BaseCommand {

    public PayCommand(NovaEconomy plugin) {
        super(plugin);
    }

    @Override
    protected void execute(CommandSender sender, Command command, String label, String[] args) {
        if (!requirePermission(sender, "cmd.pay")) return;
        if (!requirePlayer(sender)) return;

        if (args.length < 2) {
            msg.send(sender, "general.invalid-args", "usage", "/" + label + " <player> <amount> [currency]");
            return;
        }

        Player from = (Player) sender;
        Player to = findOnlinePlayer(sender, args[0]);
        if (to == null) return;

        // Self-pay check
        if (from.getUniqueId().equals(to.getUniqueId())) {
            boolean selfPayAllowed = plugin.getConfigManager().isPaySelfAllowed()
                    && from.hasPermission(plugin.getConfigManager().getPermission("pay.self"));
            if (!selfPayAllowed) {
                msg.send(sender, "pay.self-pay");
                return;
            }
        }

        OptionalDouble parsed = AmountUtil.parse(args[1]);
        if (parsed.isEmpty()) {
            msg.send(sender, "general.invalid-number", "value", args[1]);
            return;
        }

        double amount = parsed.getAsDouble();
        double minAmount = plugin.getConfigManager().getPayMinAmount();
        if (amount < minAmount) {
            Currency def = plugin.getCurrencyManager().getDefault();
            msg.send(sender, "pay.min-amount",
                    "amount", AmountUtil.formatFull(minAmount, def != null ? def.decimals() : 2));
            return;
        }

        String currencyName = args.length >= 3
                ? args[2].toUpperCase()
                : plugin.getConfigManager().getDefaultCurrency();

        if (!plugin.getCurrencyManager().exists(currencyName)) {
            msg.send(sender, "currency.not-found", "currency", currencyName);
            return;
        }

        Currency c = plugin.getCurrencyManager().get(currencyName).orElseThrow();

        // Funds check
        if (!plugin.getEconomyManager().has(from.getUniqueId(), c.name(), amount)) {
            msg.send(sender, "pay.insufficient-funds", "currency", c.displayName());
            return;
        }

        // Tax
        double tax = plugin.getConfigManager().getPayTax();
        boolean bypassTax = from.hasPermission(plugin.getConfigManager().getPermission("pay.bypass-tax"));
        double taxAmount = (bypassTax || tax <= 0) ? 0 : amount * (tax / 100.0);
        double received = amount - taxAmount;

        // Transfer
        plugin.getEconomyManager().withdraw(from.getUniqueId(), c.name(), amount);
        plugin.getEconomyManager().deposit(to.getUniqueId(), c.name(), received);

        String formattedSent     = formatAmount(amount, c);
        String formattedReceived = formatAmount(received, c);

        msg.send(from, "pay.sent", "player", to.getName(), "currency", c.displayName(), "amount", formattedSent);
        msg.send(to,   "pay.received", "player", from.getName(), "currency", c.displayName(), "amount", formattedReceived);

        if (taxAmount > 0) {
            msg.send(from, "pay.tax-info", "tax", formatAmount(taxAmount, c));
        }

        plugin.getDiscordWebhookManager().onPay(from.getName(), to.getName(), c.name(), received);
    }

    @Override
    protected List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) return filterPrefix(onlinePlayerNames(), args[0]);
        if (args.length == 2) return filterPrefix(List.of("100", "500", "1k", "10k", "100k"), args[1]);
        if (args.length == 3) return filterPrefix(plugin.getCurrencyManager().getNames(), args[2]);
        return List.of();
    }

    private String formatAmount(double amount, Currency c) {
        return plugin.getConfigManager().isShortAmountFormatting()
                ? AmountUtil.formatShort(amount, c.decimals())
                : AmountUtil.formatFull(amount, c.decimals());
    }
}
