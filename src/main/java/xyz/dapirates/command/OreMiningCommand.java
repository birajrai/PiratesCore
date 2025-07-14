package xyz.dapirates.command;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import xyz.dapirates.core.Core;
import xyz.dapirates.service.OreMiningStats;
import xyz.dapirates.listener.OreMiningListener;
import xyz.dapirates.utils.OreMiningConfig;

import java.util.*;

public class OreMiningCommand implements CommandExecutor, TabCompleter {

    private final Core plugin;
    private final OreMiningListener notifier;
    private final OreMiningConfig config;

    public OreMiningCommand(final Core plugin, final OreMiningListener notifier) {
        this.plugin = plugin;
        this.notifier = notifier;
        this.config = new OreMiningConfig(plugin);
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§7Available commands: toggle, reload, stats, top, ignore");
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "toggle":
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§cThis command can only be used by players!");
                    return true;
                }
                if (!sender.hasPermission("pc.ores.notify")) {
                    sender.sendMessage("§cYou don't have permission to use this command!");
                    return true;
                }
                notifier.toggleNotifications((Player) sender);
                break;
            case "reload":
                if (!sender.hasPermission("pc.ores.notify.admin")) {
                    sender.sendMessage("§cYou don't have permission to use this command!");
                    return true;
                }
                config.reloadConfig();
                plugin.getWebhookManager().reloadConfig();
                sender.sendMessage("§a[OreMining] §fConfiguration and webhook reloaded!");
                break;
            case "stats":
                handleStatsCommand(sender, args);
                break;
            case "top":
                handleTopCommand(sender, args);
                break;
            case "ignore":
                if (!sender.hasPermission("pc.ores.notify.admin")) {
                    sender.sendMessage("§cYou don't have permission to use this command!");
                    return true;
                }
                if (args.length > 1 && args[1].equalsIgnoreCase("list")) {
                    handleIgnoreListCommand(sender);
                } else {
                    handleIgnoreCommand(sender, args);
                }
                break;
            default:
                sender.sendMessage("§cUnknown subcommand: " + subCommand);
                sender.sendMessage("§7Available commands: toggle, reload, stats, top, ignore");
                break;
        }

        return true;
    }

    private void handleStatsCommand(CommandSender sender, String[] args) {
        if (args.length > 1 && sender.hasPermission("pc.ores.notify.admin")) {
            // Show stats for specific player
            String playerName = args[1];
            Player targetPlayer = Bukkit.getPlayer(playerName);
            if (targetPlayer == null) {
                sender.sendMessage("§cPlayer not found: " + playerName);
                return;
            }
            showPlayerStats(sender, targetPlayer);
        } else if (sender instanceof Player) {
            // Show own stats
            showPlayerStats(sender, (Player) sender);
        } else {
            sender.sendMessage("§cYou must be a player to view your own stats!");
        }
    }

    private void showPlayerStats(CommandSender sender, Player player) {
        OreMiningStats stats = plugin.getDatabaseManager().getFromCache(player.getUniqueId());
        if (stats == null) {
            // Try to load from database
            try {
                stats = plugin.getDatabaseManager().loadPlayerStatsAsync(player.getUniqueId()).get();
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load player stats: " + e.getMessage());
            }
        }

        if (stats == null) {
            sender.sendMessage("§cNo statistics found for " + player.getName());
            return;
        }

        sender.sendMessage("§a=== Mining Statistics for " + player.getName() + " ===");
        sender.sendMessage("§fTotal blocks mined: §e" + stats.getTotalBlocks());

        Map<Material, Integer> blockCounts = stats.getBlockCounts();
        if (!blockCounts.isEmpty()) {
            sender.sendMessage("§fBlock breakdown:");
            blockCounts.entrySet().stream()
                    .sorted(Map.Entry.<Material, Integer>comparingByValue().reversed())
                    .forEach(entry -> {
                        String blockName = entry.getKey().name().replace("_", " ");
                        sender.sendMessage("§7  " + blockName + ": §e" + entry.getValue());
                    });
        }
    }

    private void handleTopCommand(CommandSender sender, String[] args) {
        int limit = 10;
        if (args.length > 1) {
            try {
                limit = Integer.parseInt(args[1]);
                if (limit <= 0 || limit > 100) {
                    sender.sendMessage("§cAmount must be between 1 and 100!");
                    return;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage("§cInvalid amount: " + args[1]);
                return;
            }
        }

        try {
            List<OreMiningStats> topPlayers = plugin.getDatabaseManager().getTopPlayersAsync(limit).get();

            sender.sendMessage("§a=== Top " + limit + " Miners ===");
            if (topPlayers.isEmpty()) {
                sender.sendMessage("§7No mining data available.");
                return;
            }

            for (int i = 0; i < topPlayers.size(); i++) {
                OreMiningStats stats = topPlayers.get(i);
                String playerName = Bukkit.getOfflinePlayer(stats.getPlayerId()).getName();
                if (playerName == null)
                    playerName = "Unknown";

                String rank = getRankColor(i + 1);
                sender.sendMessage(
                        rank + "#" + (i + 1) + " §f" + playerName + " §7- §e" + stats.getTotalBlocks() + " blocks");
            }
        } catch (Exception e) {
            sender.sendMessage("§cFailed to load top players: " + e.getMessage());
            plugin.getLogger().warning("Failed to get top players: " + e.getMessage());
        }
    }

    private String getRankColor(int rank) {
        switch (rank) {
            case 1:
                return "§6"; // Gold
            case 2:
                return "§7"; // Silver
            case 3:
                return "§c"; // Bronze
            default:
                return "§f"; // White
        }
    }

    private void handleIgnoreCommand(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /oremining ignore <add|remove> <player>");
            return;
        }

        String action = args[1].toLowerCase();
        String playerName = args[2];
        Player targetPlayer = Bukkit.getPlayer(playerName);

        if (targetPlayer == null) {
            sender.sendMessage("§cPlayer not found: " + playerName);
            return;
        }

        switch (action) {
            case "add":
                notifier.addToIgnore(targetPlayer);
                sender.sendMessage("§a[OreMining] §fAdded " + playerName + " to ignore list.");
                break;
            case "remove":
                notifier.removeFromIgnore(targetPlayer);
                sender.sendMessage("§a[OreMining] §fRemoved " + playerName + " from ignore list.");
                break;
            default:
                sender.sendMessage("§cInvalid action: " + action);
                break;
        }
    }

    private void handleIgnoreListCommand(CommandSender sender) {
        List<String> ignored = config.getIgnoredPlayers();
        if (ignored.isEmpty()) {
            sender.sendMessage("§a[OreMining] §fIgnore list is empty.");
            return;
        }
        sender.sendMessage("§a[OreMining] §fIgnored players:");
        for (String name : ignored) {
            sender.sendMessage("§7- " + name);
        }
    }

    @Override
    public List<String> onTabComplete(final CommandSender sender, final Command command, final String alias, final String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("toggle", "stats", "top");
            if (sender.hasPermission("pc.ores.notify.admin")) {
                subCommands = Arrays.asList("toggle", "reload", "stats", "top", "ignore");
            }
            String input = args[0].toLowerCase();
            for (String subCommand : subCommands) {
                if (subCommand.startsWith(input)) {
                    completions.add(subCommand);
                }
            }
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            if (subCommand.equals("ignore") && sender.hasPermission("pc.ores.notify.admin")) {
                String input = args[1].toLowerCase();
                List<String> actions = Arrays.asList("add", "remove", "list");
                for (String action : actions) {
                    if (action.startsWith(input)) {
                        completions.add(action);
                    }
                }
            }
        } else if (args.length == 3) {
            String subCommand = args[0].toLowerCase();
            if (subCommand.equals("ignore") && sender.hasPermission("pc.ores.notify.admin")) {
                String input = args[2].toLowerCase();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getName().toLowerCase().startsWith(input)) {
                        completions.add(player.getName());
                    }
                }
            }
        }

        return completions;
    }
}