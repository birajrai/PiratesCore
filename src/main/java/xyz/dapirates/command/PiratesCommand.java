package xyz.dapirates.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import xyz.dapirates.core.Core;

public class PiratesCommand implements CommandExecutor {
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
        sender.sendMessage("§7Usage: /pirates reload");
        return true;
    }
}