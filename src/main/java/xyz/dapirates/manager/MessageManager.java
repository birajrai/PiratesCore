package xyz.dapirates.manager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import xyz.dapirates.core.Core;

import java.util.List;
import java.util.Map;

/**
 * Handles all player and console messaging, including placeholder processing and JSON formatting.
 */
public class MessageManager {

    private final Core plugin;
    private final boolean placeholderApiEnabled;
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    /**
     * Constructs a MessageManager for the given plugin.
     * @param plugin The main plugin instance
     */
    public MessageManager(Core plugin) {
        this.plugin = plugin;
        this.placeholderApiEnabled = plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null;
    }

    /**
     * Sends a formatted message to a player, using placeholders and JSON if configured.
     * @param player The player to send to
     * @param message The message template
     * @param material The block material (for placeholders)
     * @param location The location (for placeholders)
     * @param isTNT Whether TNT mining is involved
     */
    public void sendMessage(Player player, String message, Material material, org.bukkit.Location location,
            boolean isTNT) {
        String processedMessage = processPlaceholders(player, message, material, location, isTNT);

        if (plugin.getOreMiningConfig().getMessageFormat().equalsIgnoreCase("json")) {
            sendJsonMessage(player, processedMessage, material, location);
        } else {
            player.sendMessage(processedMessage);
        }
    }

    /**
     * Sends a JSON-formatted message to a player.
     * @param player The player to send to
     * @param message The message content
     * @param material The block material
     * @param location The location
     */
    public void sendJsonMessage(Player player, String message, Material material, org.bukkit.Location location) {
        Component component = createJsonComponent(message, material, location);
        player.sendMessage(component);
    }

    /**
     * Creates a JSON component from a message, adding color and hover/click events.
     */
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

    /**
     * Maps a Minecraft color code to a NamedTextColor.
     */
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

    /**
     * Processes placeholders in a message, including PlaceholderAPI if available.
     * @param player The player context
     * @param message The message template
     * @param material The block material
     * @param location The location
     * @param isTNT Whether TNT mining is involved
     * @return The processed message
     */
    public String processPlaceholders(Player player, String message, Material material, org.bukkit.Location location,
            boolean isTNT) {
        // Replace basic placeholders
        String processed = message
                .replace("{player}", player.getName())
                .replace("{block}", material != null ? material.name().replace("_", " ") : "BLOCK")
                .replace("{world}", location != null ? location.getWorld().getName() : "world")
                .replace("{x}", location != null ? String.valueOf(location.getBlockX()) : "x")
                .replace("{y}", location != null ? String.valueOf(location.getBlockY()) : "y")
                .replace("{z}", location != null ? String.valueOf(location.getBlockZ()) : "z");

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
                plugin.getLogger().warning("Failed to process PlaceholderAPI placeholders: " + e.getMessage());
            }
        }

        return processed;
    }

    /**
     * Sends a message to the console, using JSON if configured.
     * @param message The message to send
     */
    public void sendConsoleMessage(String message) {
        if (plugin.getOreMiningConfig().getMessageFormat().equalsIgnoreCase("json")) {
            // Convert JSON to plain text for console
            Component component = MINI_MESSAGE.deserialize(message);
            plugin.getServer().getConsoleSender().sendMessage(component);
        } else {
            plugin.getServer().getConsoleSender().sendMessage(message);
        }
    }

    /**
     * Broadcasts a message to a list of players, using placeholders and JSON if configured.
     * @param players The players to send to
     * @param message The message template
     * @param material The block material
     * @param location The location
     * @param isTNT Whether TNT mining is involved
     */
    public void broadcastMessage(List<Player> players, String message, Material material, org.bukkit.Location location,
            boolean isTNT) {
        for (Player player : players) {
            sendMessage(player, message, material, location, isTNT);
        }
    }

    /**
     * Creates a clickable chat component.
     */
    public Component createClickableComponent(String text, String command, String hoverText) {
        return Component.text(text)
                .clickEvent(ClickEvent.runCommand(command))
                .hoverEvent(HoverEvent.showText(Component.text(hoverText)));
    }

    /**
     * Creates a hoverable chat component.
     */
    public Component createHoverComponent(String text, String hoverText) {
        return Component.text(text)
                .hoverEvent(HoverEvent.showText(Component.text(hoverText)));
    }

    /**
     * @return true if PlaceholderAPI is enabled
     */
    public boolean isPlaceholderApiEnabled() {
        return placeholderApiEnabled;
    }
}