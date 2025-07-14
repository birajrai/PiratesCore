package xyz.dapirates.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import xyz.dapirates.core.Core;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import net.milkbowl.vault.economy.Economy;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Arrays;
import java.util.Collections;
import org.bukkit.OfflinePlayer;

public class PiratesCommand implements CommandExecutor, TabCompleter {
    private final Core plugin;

    public PiratesCommand(final Core plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("pirates.reload")) {
                sender.sendMessage("§cYou don't have permission to use this command!");
                return true;
            }
            plugin.getConfigManager().reloadAll();
            sender.sendMessage("§a[PiratesAddons] All configs reloaded!");
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("updatesql")) {
            if (!sender.hasPermission("pirates.updatesql")) {
                sender.sendMessage("§cYou don't have permission to use this command!");
                return true;
            }
            try {
                plugin.getPlayerStatsHandler().reloadConfig(plugin.getConfigManager());
                // Also update balancetop leaderboard
                Economy econ = plugin.getEconomy();
                if (econ != null) {
                    List<Player> onlinePlayers = Bukkit.getOnlinePlayers().stream().collect(Collectors.toList());
                    onlinePlayers.sort(Comparator.comparingDouble(p -> econ.getBalance((OfflinePlayer)p)).reversed());
                    int topN = Math.min(10, onlinePlayers.size());
                    List<Player> topPlayers = onlinePlayers.subList(0, topN);
                    plugin.getPlayerStatsHandler().updateTopBalanceLeaderboard(topPlayers, econ);
                }
                sender.sendMessage("§a[PiratesAddons] SQL config reloaded and tables updated!");
            } catch (Exception e) {
                sender.sendMessage("§c[PiratesAddons] Failed to update SQL: " + e.getMessage());
            }
            return true;
        }
        sender.sendMessage("§7Usage: /pirates reload | /pirates updatesql");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("reload", "updatesql");
        }
        return Collections.emptyList();
    }
}