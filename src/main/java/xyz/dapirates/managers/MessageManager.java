package xyz.dapirates.managers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import xyz.dapirates.Core;

import java.util.List;
import java.util.Map;

public class MessageManager {

    private final Core plugin;
    private final boolean placeholderApiEnabled;

    public MessageManager(Core plugin) {
        this.plugin = plugin;
        this.placeholderApiEnabled = plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null;
    }

    public void sendMessage(Player player, String message, Material material, org.bukkit.Location location,
            boolean isTNT) {
        String processedMessage = processPlaceholders(player, message, material, location, isTNT);

        if (plugin.getOreMiningConfig().getMessageFormat().equalsIgnoreCase("json")) {
            sendJsonMessage(player, processedMessage, material, location);
        } else {
            player.sendMessage(processedMessage);
        }
    }

    public void sendJsonMessage(Player player, String message, Material material, org.bukkit.Location location) {
        Component component = createJsonComponent(message, material, location);
        player.sendMessage(component);
    }

    private Component createJsonComponent(String message, Material material, org.bukkit.Location location) {
        // Parse the message and create a rich JSON component
        TextComponent.Builder builder = Component.text();

        // Split message by color codes and create components
        String[] parts = message.split("§");
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (i == 0 && !part.startsWith("§")) {
                // First part without color code
                builder.append(Component.text(part));
                continue;
            }

            if (part.length() < 1)
                continue;

            char colorCode = part.charAt(0);
            String text = part.substring(1);

            NamedTextColor color = getColorFromCode(colorCode);
            TextComponent component = Component.text(text, color);

            // Add hover effect for coordinates
            if (text.contains("{x}") || text.contains("{y}") || text.contains("{z}")) {
                component = component.hoverEvent(HoverEvent.showText(
                        Component.text("Click to teleport!", NamedTextColor.GREEN))).clickEvent(ClickEvent.runCommand(
                                "/tp " + location.getBlockX() + " " + location.getBlockY() + " "
                                        + location.getBlockZ()));
            }

            // Add hover effect for block type
            if (text.contains(material.name().replace("_", " "))) {
                component = component.hoverEvent(HoverEvent.showText(
                        Component.text("Block: " + material.name(), NamedTextColor.YELLOW)));
            }

            builder.append(component);
        }

        return builder.build();
    }

    private NamedTextColor getColorFromCode(char code) {
        return switch (code) {
            case '0' -> NamedTextColor.BLACK;
            case '1' -> NamedTextColor.DARK_BLUE;
            case '2' -> NamedTextColor.DARK_GREEN;
            case '3' -> NamedTextColor.DARK_AQUA;
            case '4' -> NamedTextColor.DARK_RED;
            case '5' -> NamedTextColor.DARK_PURPLE;
            case '6' -> NamedTextColor.GOLD;
            case '7' -> NamedTextColor.GRAY;
            case '8' -> NamedTextColor.DARK_GRAY;
            case '9' -> NamedTextColor.BLUE;
            case 'a' -> NamedTextColor.GREEN;
            case 'b' -> NamedTextColor.AQUA;
            case 'c' -> NamedTextColor.RED;
            case 'd' -> NamedTextColor.LIGHT_PURPLE;
            case 'e' -> NamedTextColor.YELLOW;
            case 'f' -> NamedTextColor.WHITE;
            default -> NamedTextColor.WHITE;
        };
    }

    public String processPlaceholders(Player player, String message, Material material, org.bukkit.Location location,
            boolean isTNT) {
        // Replace basic placeholders
        String processed = message
                .replace("{player}", player.getName())
                .replace("{block}", material.name().replace("_", " "))
                .replace("{world}", location.getWorld().getName())
                .replace("{x}", String.valueOf(location.getBlockX()))
                .replace("{y}", String.valueOf(location.getBlockY()))
                .replace("{z}", String.valueOf(location.getBlockZ()));

        if (isTNT) {
            processed = processed.replace("found", "found (TNT)");
        }

        // Process PlaceholderAPI placeholders if available
        if (placeholderApiEnabled) {
            try {
                // Use reflection to avoid direct dependency
                Class<?> placeholderApiClass = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
                Object placeholderApi = placeholderApiClass.getMethod("setPlaceholders", Player.class, String.class)
                        .invoke(null, player, processed);
                processed = (String) placeholderApi;
            } catch (Exception e) {
                // PlaceholderAPI not available or error occurred
                plugin.getLogger().warning("Failed to process PlaceholderAPI placeholders: " + e.getMessage());
            }
        }

        return processed;
    }

    public void sendConsoleMessage(String message) {
        if (plugin.getOreMiningConfig().getMessageFormat().equalsIgnoreCase("json")) {
            // Convert JSON to legacy for console
            Component component = LegacyComponentSerializer.legacySection().deserialize(message);
            plugin.getServer().getConsoleSender().sendMessage(component);
        } else {
            plugin.getServer().getConsoleSender().sendMessage(message);
        }
    }

    public void broadcastMessage(List<Player> players, String message, Material material, org.bukkit.Location location,
            boolean isTNT) {
        for (Player player : players) {
            sendMessage(player, message, material, location, isTNT);
        }
    }

    public Component createClickableComponent(String text, String command, String hoverText) {
        return Component.text(text)
                .clickEvent(ClickEvent.runCommand(command))
                .hoverEvent(HoverEvent.showText(Component.text(hoverText)));
    }

    public Component createHoverComponent(String text, String hoverText) {
        return Component.text(text)
                .hoverEvent(HoverEvent.showText(Component.text(hoverText)));
    }

    public boolean isPlaceholderApiEnabled() {
        return placeholderApiEnabled;
    }
}