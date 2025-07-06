package xyz.dapirates.features;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ShowCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        Player player = (Player) sender;
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType().isAir()) {
            player.sendMessage("You are not holding any item.");
            return true;
        }
        // Get item name (display name or type)
        String itemName = item.hasItemMeta() && item.getItemMeta().hasDisplayName()
                ? item.getItemMeta().getDisplayName()
                : item.getType().name().replace('_', ' ').toLowerCase();
        Component itemComponent = Component.text(itemName, NamedTextColor.AQUA)
            .hoverEvent(item.asHoverEvent());
        Component message = Component.text(player.getName() + " shows you the item [", NamedTextColor.GREEN)
            .append(itemComponent)
            .append(Component.text("]", NamedTextColor.GREEN));
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(message);
        }
        return true;
    }
} 