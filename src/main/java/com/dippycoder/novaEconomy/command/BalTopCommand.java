package com.dippycoder.novaEconomy.command;

import com.dippycoder.novaEconomy.NovaEconomy;
import com.dippycoder.novaEconomy.manager.Currency;
import com.dippycoder.novaEconomy.util.AmountUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BalTopCommand extends BaseCommand {

    private static final int PER_PAGE = 10;

    public BalTopCommand(NovaEconomy plugin) {
        super(plugin);
    }

    @Override
    protected void execute(CommandSender sender, Command command, String label, String[] args) {
        if (!requirePermission(sender, "cmd.baltop")) return;

        String currencyName = args.length >= 1
                ? args[0].toUpperCase()
                : plugin.getConfigManager().getDefaultCurrency();

        if (!plugin.getCurrencyManager().exists(currencyName)) {
            msg.send(sender, "currency.not-found", "currency", currencyName);
            return;
        }

        int page = 1;
        if (args.length >= 2) {
            try { page = Math.max(1, Integer.parseInt(args[1])); }
            catch (NumberFormatException ignored) {
                msg.send(sender, "baltop.invalid-page");
                return;
            }
        }

        Currency c = plugin.getCurrencyManager().get(currencyName).orElseThrow();
        List<Map.Entry<String, Double>> all = plugin.getEconomyManager().getTopBalances(currencyName, PER_PAGE * page);

        if (all.isEmpty()) {
            msg.send(sender, "baltop.empty", "currency", c.displayName());
            return;
        }

        int fromIdx = (page - 1) * PER_PAGE;
        if (fromIdx >= all.size()) {
            msg.send(sender, "baltop.invalid-page");
            return;
        }

        int totalPages = (int) Math.ceil(all.size() / (double) PER_PAGE);
        msg.send(sender, "baltop.header", "currency", c.displayName());

        List<Map.Entry<String, Double>> entries = all.subList(fromIdx, Math.min(fromIdx + PER_PAGE, all.size()));
        int rank = fromIdx + 1;
        for (Map.Entry<String, Double> entry : entries) {
            String name = resolvePlayerName(entry.getKey());
            String formatted = plugin.getConfigManager().isShortAmountFormatting()
                    ? AmountUtil.formatShort(entry.getValue(), c.decimals())
                    : AmountUtil.formatFull(entry.getValue(), c.decimals());
            msg.send(sender, "baltop.entry", "rank", rank, "player", name, "amount", formatted);
            rank++;
        }

        msg.send(sender, "baltop.footer", "page", page, "pages", totalPages);
    }

    @Override
    protected List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) return filterPrefix(plugin.getCurrencyManager().getNames(), args[0]);
        return List.of();
    }

    @SuppressWarnings("deprecation")
    private String resolvePlayerName(String uuid) {
        try {
            org.bukkit.OfflinePlayer op = plugin.getServer().getOfflinePlayer(UUID.fromString(uuid));
            String name = op.getName();
            return name != null ? name : uuid;
        } catch (Exception e) {
            return uuid;
        }
    }
}
