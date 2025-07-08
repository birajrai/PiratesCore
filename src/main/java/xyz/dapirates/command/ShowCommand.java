package xyz.dapirates.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
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
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacySection();
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
            return true;
        }
        
        Player player = (Player) sender;
        ItemStack item = player.getInventory().getItemInMainHand();
        
        if (item == null || item.getType().isAir()) {
            player.sendMessage(Component.text("You are not holding any item.", NamedTextColor.RED));
            return true;
        }

        try {
            Component message = buildShowMessage(player, item);
            Bukkit.broadcast(message);
            return true;
        } catch (Exception e) {
            player.sendMessage(Component.text("An error occurred while showing your item.", NamedTextColor.RED));
            e.printStackTrace();
            return true;
        }
    }
    
    private Component buildShowMessage(Player player, ItemStack item) {
        // Get player prefix from LuckPerms
        Component playerPrefix = getPlayerPrefix(player);
        
        // Build the item component
        Component itemComponent = buildItemComponent(item);
        
        // Create the main message
        Component message = Component.text(player.getName() + " is holding ", NamedTextColor.WHITE)
                .append(itemComponent);
        
        // Add prefix if available
        if (!playerPrefix.equals(Component.empty())) {
            message = playerPrefix.append(Component.space()).append(message);
        }
        
        return message;
    }
    
    private Component getPlayerPrefix(Player player) {
        try {
            LuckPerms luckPerms = LuckPermsProvider.get();
            User user = luckPerms.getUserManager().getUser(player.getUniqueId());
            
            if (user != null) {
                String prefix = user.getCachedData().getMetaData().getPrefix();
                if (prefix != null && !prefix.trim().isEmpty()) {
                    // Handle both legacy color codes and MiniMessage format
                    if (prefix.contains("§") || prefix.contains("&")) {
                        // Support & color codes by replacing with §
                        String legacyPrefix = prefix.replace('&', '§');
                        return LEGACY_SERIALIZER.deserialize(legacyPrefix);
                    } else {
                        return MINI_MESSAGE.deserialize(prefix);
                    }
                }
            }
        } catch (Exception e) {
            // LuckPerms not available or error occurred, continue without prefix
        }
        
        return Component.empty();
    }
    
    private Component buildItemComponent(ItemStack item) {
        // Get the item's display name or default name
        Component itemName = item.hasItemMeta() && item.getItemMeta().hasDisplayName() 
            ? item.displayName() 
            : Component.translatable(item.translationKey());
        
        // Add quantity if more than 1
        if (item.getAmount() > 1) {
            itemName = itemName.append(Component.text(" x" + item.getAmount(), NamedTextColor.GRAY));
        }
        
        // Check if item name already has brackets
        String plainName = LEGACY_SERIALIZER.serialize(itemName);
        String cleanName = plainName.replaceAll("§[0-9a-fk-or]", "");
        
        // Add brackets if not already present
        if (!(cleanName.startsWith("[") && cleanName.endsWith("]"))) {
            itemName = Component.text("[", NamedTextColor.WHITE)
                    .append(itemName)
                    .append(Component.text("]", NamedTextColor.WHITE));
        }
        
        // Add lore information if available
        if (item.hasItemMeta() && item.getItemMeta().hasLore()) {
            var lore = item.getItemMeta().getLore();
            if (lore != null && !lore.isEmpty()) {
                String firstLoreLine = lore.get(0);
                if (firstLoreLine != null && !firstLoreLine.trim().isEmpty()) {
                    Component loreComponent = parseLoreLine(firstLoreLine);
                    itemName = itemName.append(Component.text(" shows ", NamedTextColor.WHITE))
                            .append(loreComponent);
                }
            }
        }
        
        // Add hover event to show item details
        return itemName.hoverEvent(item.asHoverEvent());
    }
    
    private Component parseLoreLine(String loreLine) {
        try {
            // Try MiniMessage first
            return MINI_MESSAGE.deserialize(loreLine);
        } catch (Exception e) {
            try {
                // Fallback to legacy color codes, support & by replacing with §
                String legacyLore = loreLine.replace('&', '§');
                return LEGACY_SERIALIZER.deserialize(legacyLore);
            } catch (Exception e2) {
                // If all else fails, return as plain text
                return Component.text(loreLine, NamedTextColor.GRAY);
            }
        }
    }
}