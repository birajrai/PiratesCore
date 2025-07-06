package xyz.dapirates.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import xyz.dapirates.core.Core;

public class PiratesCommand implements CommandExecutor {
    private final Core plugin;

    public PiratesCommand(Core plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("pirates.reload")) {
                sender.sendMessage("§cYou don't have permission to use this command!");
                return true;
            }
            plugin.getConfigManager().reloadAll();
            plugin.getChatFilterListener().reload();
            sender.sendMessage("§a[PiratesAddons] All configs reloaded!");
            return true;
        }
        sender.sendMessage("§7Usage: /pirates reload");
        return true;
    }
}