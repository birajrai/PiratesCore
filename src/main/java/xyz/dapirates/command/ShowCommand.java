package xyz.dapirates.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
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

        // Get the item's display name component (preserves exact formatting and color)
        Component itemComponent = item.displayName();

        // Add quantity if more than 1 item
        int quantity = item.getAmount();
        if (quantity > 1) {
            itemComponent = itemComponent.append(Component.text(" x" + quantity, NamedTextColor.GRAY));
        }

        // Add hover event to show item details
        itemComponent = itemComponent.hoverEvent(item.asHoverEvent());

        // Get player's prefix from LuckPerms
        Component playerPrefixComponent = Component.empty();
        try {
            LuckPerms luckPerms = LuckPermsProvider.get();
            User user = luckPerms.getUserManager().getUser(player.getUniqueId());
            if (user != null) {
                String prefix = user.getCachedData().getMetaData().getPrefix();
                if (prefix != null) {
                    // Convert color codes to component using LegacyComponentSerializer
                    playerPrefixComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(prefix);
                }
            }
        } catch (Exception e) {
            // LuckPerms not available, use default
        }

        // Create message with prefix and item
        Component message = Component.text(player.getName() + " shows you the item [", NamedTextColor.GREEN)
                .append(itemComponent)
                .append(Component.text("]", NamedTextColor.GREEN));

        // Insert prefix at the beginning
        if (!playerPrefixComponent.equals(Component.empty())) {
            message = playerPrefixComponent.append(message);
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(message);
        }
        return true;
    }
}