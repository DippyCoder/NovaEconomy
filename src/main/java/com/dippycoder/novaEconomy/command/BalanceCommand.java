package com.dippycoder.novaEconomy.command;

import com.dippycoder.novaEconomy.NovaEconomy;
import com.dippycoder.novaEconomy.manager.Currency;
import com.dippycoder.novaEconomy.util.AmountUtil;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class BalanceCommand extends BaseCommand {

    public BalanceCommand(NovaEconomy plugin) {
        super(plugin);
    }

    @Override
    protected void execute(CommandSender sender, Command command, String label, String[] args) {
        if (!requirePermission(sender, "cmd.balance")) return;

        boolean checkingOther = args.length >= 1;

        if (checkingOther && !sender.hasPermission(plugin.getConfigManager().getPermission("cmd.balance.other"))) {
            msg.send(sender, "general.no-permission");
            return;
        }

        String currencyName = plugin.getConfigManager().getDefaultCurrency();
        if (args.length >= 2) currencyName = args[1].toUpperCase();

        if (!plugin.getCurrencyManager().exists(currencyName)) {
            msg.send(sender, "currency.not-found", "currency", currencyName);
            return;
        }

        Currency c = plugin.getCurrencyManager().get(currencyName).orElseThrow();

        if (!checkingOther) {
            if (!requirePlayer(sender)) return;
            Player player = (Player) sender;
            double balance = plugin.getEconomyManager().getBalance(player.getUniqueId(), c.name());
            msg.send(sender, "balance.own",
                    "currency", c.displayName(),
                    "amount", formatAmount(balance, c));
        } else {
            @SuppressWarnings("deprecation")
            OfflinePlayer target = plugin.getServer().getOfflinePlayer(args[0]);
            if (!target.hasPlayedBefore() && !target.isOnline()) {
                msg.send(sender, "general.player-not-found", "player", args[0]);
                return;
            }
            double balance = plugin.getEconomyManager().getBalance(target.getUniqueId(), c.name());
            msg.send(sender, "balance.other",
                    "player", target.getName() != null ? target.getName() : args[0],
                    "currency", c.displayName(),
                    "amount", formatAmount(balance, c));
        }
    }

    @Override
    protected List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) return filterPrefix(onlinePlayerNames(), args[0]);
        if (args.length == 2) return filterPrefix(plugin.getCurrencyManager().getNames(), args[1]);
        return List.of();
    }

    private String formatAmount(double amount, Currency c) {
        return plugin.getConfigManager().isShortAmountFormatting()
                ? AmountUtil.formatShort(amount, c.decimals())
                : AmountUtil.formatFull(amount, c.decimals());
    }
}
