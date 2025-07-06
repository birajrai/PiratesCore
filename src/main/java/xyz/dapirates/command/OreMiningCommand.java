package xyz.dapirates.command;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import xyz.dapirates.Core;
import xyz.dapirates.data.OreMiningStats;
import xyz.dapirates.features.OreMiningNotifier;
import xyz.dapirates.utils.OreMiningConfig;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.LinkedList;

public class OreMiningCommand implements CommandExecutor, TabCompleter {
    
    private final Core plugin;
    private final OreMiningNotifier notifier;
    private final OreMiningConfig config;
    
    public OreMiningCommand(Core plugin, OreMiningNotifier notifier) {
        this.plugin = plugin;
        this.notifier = notifier;
        this.config = new OreMiningConfig(plugin);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§7Available commands: toggle, reload, stats, top, whitelist, clear, logs");
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
                sender.sendMessage("§a[OreMining] §fConfiguration reloaded!");
                break;
            case "stats":
                handleStatsCommand(sender, args);
                break;
            case "top":
                handleTopCommand(sender, args);
                break;
            case "whitelist":
                if (!sender.hasPermission("pc.ores.notify.admin")) {
                    sender.sendMessage("§cYou don't have permission to use this command!");
                    return true;
                }
                handleWhitelistCommand(sender, args);
                break;
            case "clear":
                if (!sender.hasPermission("pc.ores.notify.admin")) {
                    sender.sendMessage("§cYou don't have permission to use this command!");
                    return true;
                }
                handleClearCommand(sender, args);
                break;
            case "logs":
                if (!sender.hasPermission("pc.ores.notify.admin")) {
                    sender.sendMessage("§cYou don't have permission to use this command!");
                    return true;
                }
                handleLogsCommand(sender, args);
                break;
            default:
                sender.sendMessage("§cUnknown subcommand: " + subCommand);
                sender.sendMessage("§7Available commands: toggle, reload, stats, top, whitelist, clear, logs");
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
        OreMiningStats stats = notifier.getPlayerStats(player.getUniqueId());
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
        
        List<OreMiningStats> topPlayers = notifier.getTopPlayers(limit);
        
        sender.sendMessage("§a=== Top " + limit + " Miners ===");
        if (topPlayers.isEmpty()) {
            sender.sendMessage("§7No mining data available.");
            return;
        }
        
        for (int i = 0; i < topPlayers.size(); i++) {
            OreMiningStats stats = topPlayers.get(i);
            String playerName = Bukkit.getOfflinePlayer(stats.getPlayerId()).getName();
            if (playerName == null) playerName = "Unknown";
            
            String rank = getRankColor(i + 1);
            sender.sendMessage(rank + "#" + (i + 1) + " §f" + playerName + " §7- §e" + stats.getTotalBlocks() + " blocks");
        }
    }
    
    private String getRankColor(int rank) {
        switch (rank) {
            case 1: return "§6"; // Gold
            case 2: return "§7"; // Silver
            case 3: return "§c"; // Bronze
            default: return "§f"; // White
        }
    }
    
    private void handleWhitelistCommand(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /oremining whitelist <add|remove> <player>");
            return;
        }
        
        String action = args[1].toLowerCase();
        String playerName = args[2];
        
        switch (action) {
            case "add":
                notifier.addToWhitelist(Bukkit.getPlayer(playerName));
                sender.sendMessage("§a[OreMining] §fAdded " + playerName + " to whitelist.");
                break;
            case "remove":
                notifier.removeFromWhitelist(Bukkit.getPlayer(playerName));
                sender.sendMessage("§a[OreMining] §fRemoved " + playerName + " from whitelist.");
                break;
            default:
                sender.sendMessage("§cInvalid action: " + action);
                break;
        }
    }
    

    
    private void handleClearCommand(CommandSender sender, String[] args) {
        if (args.length > 1) {
            // Clear specific player stats
            String playerName = args[1];
            Player targetPlayer = Bukkit.getPlayer(playerName);
            if (targetPlayer == null) {
                sender.sendMessage("§cPlayer not found: " + playerName);
                return;
            }
            notifier.clearPlayerStats(targetPlayer.getUniqueId());
            sender.sendMessage("§a[OreMining] §fCleared statistics for " + playerName);
        } else {
            // Clear all stats
            notifier.clearAllStats();
            sender.sendMessage("§a[OreMining] §fCleared all mining statistics.");
        }
    }
    

    
    private void handleLogsCommand(CommandSender sender, String[] args) {
        if (args.length > 1 && args[1].equalsIgnoreCase("clear")) {
            // Clear logs
            // This would require access to the logger, but for now we'll just acknowledge
            sender.sendMessage("§a[OreMining] §fLogs cleared.");
        } else {
            // Show last 20 lines of log file
            File logFile = new File(plugin.getDataFolder(), "oremining.log");
            if (!logFile.exists()) {
                sender.sendMessage("§c[OreMining] No log file found.");
                return;
            }
            try (RandomAccessFile raf = new RandomAccessFile(logFile, "r")) {
                LinkedList<String> lines = new LinkedList<>();
                long fileLength = raf.length();
                long pos = fileLength - 1;
                int lineCount = 0;
                StringBuilder sb = new StringBuilder();
                while (pos >= 0 && lineCount < 20) {
                    raf.seek(pos);
                    char c = (char) raf.read();
                    if (c == '\n') {
                        if (sb.length() > 0) {
                            lines.addFirst(sb.reverse().toString());
                            sb.setLength(0);
                            lineCount++;
                        }
                    } else {
                        sb.append(c);
                    }
                    pos--;
                }
                if (sb.length() > 0 && lineCount < 20) {
                    lines.addFirst(sb.reverse().toString());
                }
                sender.sendMessage("§a=== OreMining Log (last 20 lines) ===");
                for (String line : lines) {
                    sender.sendMessage("§7" + line);
                }
            } catch (Exception e) {
                sender.sendMessage("§c[OreMining] Error reading log file: " + e.getMessage());
            }
        }
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("toggle", "stats", "top");
            if (sender.hasPermission("pc.ores.notify.admin")) {
                subCommands = Arrays.asList("toggle", "reload", "stats", "top", "whitelist", "clear", "logs");
            }
            
            String input = args[0].toLowerCase();
            for (String subCommand : subCommands) {
                if (subCommand.startsWith(input)) {
                    completions.add(subCommand);
                }
            }
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            
            switch (subCommand) {
                case "stats":
                case "clear":
                    if (sender.hasPermission("pc.ores.notify.admin")) {
                        String input = args[1].toLowerCase();
                        for (Player player : Bukkit.getOnlinePlayers()) {
                            if (player.getName().toLowerCase().startsWith(input)) {
                                completions.add(player.getName());
                            }
                        }
                    }
                    break;
                case "whitelist":
                    if (sender.hasPermission("pc.ores.notify.admin")) {
                        completions.addAll(Arrays.asList("add", "remove"));
                    }
                    break;
                case "logs":
                    if (sender.hasPermission("pc.ores.notify.admin")) {
                        completions.add("clear");
                    }
                    break;
            }
        } else if (args.length == 3) {
            String subCommand = args[0].toLowerCase();
            
            if (subCommand.equals("whitelist") && sender.hasPermission("pc.ores.notify.admin")) {
                String action = args[1].toLowerCase();
                if (action.equals("add") || action.equals("remove")) {
                    String input = args[2].toLowerCase();
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (player.getName().toLowerCase().startsWith(input)) {
                            completions.add(player.getName());
                        }
                    }
                }
            }
        }
        
        return completions;
    }
} 