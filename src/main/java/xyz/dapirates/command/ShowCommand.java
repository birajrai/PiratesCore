package xyz.dapirates.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
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
        Component rawNameComponent = item.displayName();
        String rawNameWithCodes = LegacyComponentSerializer.legacySection().serialize(rawNameComponent);
        // Remove color codes (e.g., §a) for bracket check
        String rawNamePlain = rawNameWithCodes.replaceAll("§[0-9a-fk-or]", "");
        Component itemComponent = rawNameComponent;
        
        // Add quantity if more than 1 item (inside the brackets)
        int quantity = item.getAmount();
        if (quantity > 1) {
            itemComponent = itemComponent.append(Component.text(" x" + quantity, NamedTextColor.GRAY));
        }
        
        // Only add brackets if not already present in plain text
        if (!(rawNamePlain.startsWith("[") && rawNamePlain.endsWith("]"))) {
            itemComponent = Component.text("[", NamedTextColor.WHITE)
                .append(itemComponent)
                .append(Component.text("]", NamedTextColor.WHITE));
        }
        
        // Add lore information if available
        if (item.hasItemMeta() && item.getItemMeta().hasLore()) {
            String loreInfo = item.getItemMeta().getLore().get(0); // Get first lore line
            if (loreInfo != null && !loreInfo.isEmpty()) {
                // Convert lore color codes to component
                Component loreComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(loreInfo);
                itemComponent = itemComponent.append(Component.text(" shows ", NamedTextColor.WHITE))
                    .append(loreComponent);
            }
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
        Component message = Component.text(player.getName() + " is holding ", NamedTextColor.WHITE)
                .append(itemComponent);

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