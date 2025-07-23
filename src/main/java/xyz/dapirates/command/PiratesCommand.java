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
import net.Indyuce.bountyhunters.BountyHunters;
import net.Indyuce.bountyhunters.api.Bounty;
import net.Indyuce.bountyhunters.manager.BountyManager;

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
            if (!plugin.getFeatureManager().isFeatureEnabled("Stats")) {
                sender.sendMessage("§c[PiratesAddons] Stats feature is disabled in the config. Enable it to use this command.");
                return true;
            }
            try {
                plugin.getPlayerStatsHandler().reloadConfig(plugin.getConfigManager());
                Economy econ = plugin.getEconomy();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    String name = player.getName();
                    String uuid = player.getUniqueId().toString();
                    plugin.getPlayerStatsHandler().upsertPlayerAsync(name, uuid);
                    // Playtime in seconds
                    long playtimeTicks = player.getStatistic(org.bukkit.Statistic.PLAY_ONE_MINUTE);
                    long playtimeSeconds = playtimeTicks / 20L;
                    plugin.getPlayerStatsHandler().saveStatAsync("playtime", name, playtimeSeconds);
                    // Balance
                    if (econ != null) {
                        double balance = econ.getBalance(player);
                        plugin.getPlayerStatsHandler().saveStatAsync("balance", name, (long) balance);
                    }
                    // Joined (for demo, increment by 1)
                    plugin.getPlayerStatsHandler().loadStatAsync("joined", name).thenAccept(joined -> {
                        plugin.getPlayerStatsHandler().saveStatAsync("joined", name, joined + 1);
                    });
                    // Boat mount (for demo, not tracked here)
                    // Add more stats as needed
                }
                // --- BountyHunters: Update all current bounties ---
                if (plugin.getServer().getPluginManager().getPlugin("BountyHunters") != null) {
                    BountyManager bountyManager = BountyHunters.getInstance().getBountyManager();
                    for (Bounty bounty : bountyManager.getBounties()) {
                        OfflinePlayer target = bounty.getTarget();
                        double reward = bounty.getReward();
                        plugin.getPlayerStatsHandler().saveTopBountyAsync(target.getName(), (long) reward);
                    }
                }
                sender.sendMessage("§a[PiratesAddons] SQL config reloaded, all player stats and tables updated!");
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