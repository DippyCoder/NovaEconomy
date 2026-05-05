package com.dippycoder.novaEconomy.command;

import com.dippycoder.novaEconomy.NovaEconomy;
import com.dippycoder.novaEconomy.message.MessageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public abstract class BaseCommand implements CommandExecutor, TabCompleter {

    protected final NovaEconomy plugin;
    protected final MessageManager msg;

    protected BaseCommand(NovaEconomy plugin) {
        this.plugin = plugin;
        this.msg = plugin.getMessageManager();
    }

    @Override
    public final boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                                   @NotNull String label, @NotNull String[] args) {
        execute(sender, command, label, args);
        return true;
    }

    protected abstract void execute(CommandSender sender, Command command, String label, String[] args);

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, @NotNull String[] args) {
        return tabComplete(sender, args);
    }

    protected List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }

    // ── Helpers ───────────────────────────────────────────────

    protected boolean requirePlayer(CommandSender sender) {
        if (!(sender instanceof Player)) {
            msg.send(sender, "general.player-only");
            return false;
        }
        return true;
    }

    protected boolean requirePermission(CommandSender sender, String permKey) {
        if (!sender.hasPermission(plugin.getConfigManager().getPermission(permKey))) {
            msg.send(sender, "general.no-permission");
            return false;
        }
        return true;
    }

    protected Player findOnlinePlayer(CommandSender sender, String name) {
        Player target = plugin.getServer().getPlayerExact(name);
        if (target == null) msg.send(sender, "general.player-not-found", "player", name);
        return target;
    }

    @SuppressWarnings("deprecation")
    protected org.bukkit.OfflinePlayer findOfflinePlayer(CommandSender sender, String name) {
        org.bukkit.OfflinePlayer op = plugin.getServer().getOfflinePlayer(name);
        if (!op.hasPlayedBefore() && !op.isOnline()) {
            msg.send(sender, "general.player-not-found", "player", name);
            return null;
        }
        return op;
    }

    protected List<String> filterPrefix(List<String> list, String prefix) {
        if (prefix.isEmpty()) return list;
        return list.stream()
                .filter(s -> s.toLowerCase().startsWith(prefix.toLowerCase()))
                .toList();
    }

    protected List<String> onlinePlayerNames() {
        return plugin.getServer().getOnlinePlayers().stream()
                .map(Player::getName)
                .toList();
    }
}
