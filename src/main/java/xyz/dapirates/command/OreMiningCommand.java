package xyz.dapirates.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import xyz.dapirates.Core;
import xyz.dapirates.features.OreMiningNotifier;
import xyz.dapirates.utils.OreMiningConfig;

import java.util.*;

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
            sender.sendMessage("§7Available commands: toggle, reload, whitelist");
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
            case "whitelist":
                if (!sender.hasPermission("pc.ores.notify.admin")) {
                    sender.sendMessage("§cYou don't have permission to use this command!");
                    return true;
                }
                handleWhitelistCommand(sender, args);
                break;
            default:
                sender.sendMessage("§cUnknown subcommand: " + subCommand);
                sender.sendMessage("§7Available commands: toggle, reload, whitelist");
                break;
        }

        return true;
    }

    private void handleWhitelistCommand(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /oremining whitelist <add|remove> <player>");
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
                notifier.addToWhitelist(targetPlayer);
                sender.sendMessage("§a[OreMining] §fAdded " + playerName + " to whitelist.");
                break;
            case "remove":
                notifier.removeFromWhitelist(targetPlayer);
                sender.sendMessage("§a[OreMining] §fRemoved " + playerName + " from whitelist.");
                break;
            default:
                sender.sendMessage("§cInvalid action: " + action);
                break;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("toggle");
            if (sender.hasPermission("pc.ores.notify.admin")) {
                subCommands = Arrays.asList("toggle", "reload", "whitelist");
            }

            String input = args[0].toLowerCase();
            for (String subCommand : subCommands) {
                if (subCommand.startsWith(input)) {
                    completions.add(subCommand);
                }
            }
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();

            if (subCommand.equals("whitelist") && sender.hasPermission("pc.ores.notify.admin")) {
                String input = args[1].toLowerCase();
                List<String> actions = Arrays.asList("add", "remove");
                for (String action : actions) {
                    if (action.startsWith(input)) {
                        completions.add(action);
                    }
                }
            }
        } else if (args.length == 3) {
            String subCommand = args[0].toLowerCase();

            if (subCommand.equals("whitelist") && sender.hasPermission("pc.ores.notify.admin")) {
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