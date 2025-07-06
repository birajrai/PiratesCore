package xyz.dapirates.managers;

import xyz.dapirates.Core;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OreMiningWebhook {
    private final Core plugin;
    private final WebhookManager webhookManager;
    private static final String WEBHOOK_NAME = "oremining";

    public OreMiningWebhook(Core plugin, WebhookManager webhookManager) {
        this.plugin = plugin;
        this.webhookManager = webhookManager;
    }

    public void sendIndividualBlockNotification(Player player, Material material, Location location, boolean isTNT) {
        String title = isTNT ? "💥 TNT Mining Alert" : "⛏️ Ore Mining Alert";
        String description = String.format("**%s** found **%s**", 
            player.getName(), 
            getPrettyOreName(material));
        
        List<WebhookManager.Field> fields = new ArrayList<>();
        fields.add(new WebhookManager.Field("Player", player.getName(), true));
        fields.add(new WebhookManager.Field("Ore", getPrettyOreName(material), true));
        fields.add(new WebhookManager.Field("Method", isTNT ? "TNT" : "Mining", true));
        fields.add(new WebhookManager.Field("Location", String.format("X: %d, Y: %d, Z: %d", 
            location.getBlockX(), location.getBlockY(), location.getBlockZ()), false));
        fields.add(new WebhookManager.Field("World", location.getWorld().getName(), false));
        
        String color = getOreColor(material);
        
        webhookManager.sendEmbed(WEBHOOK_NAME, title, description, fields, color, null, "Ore Mining Notifier");
    }

    public void sendBatchedSessionNotification(Player player, Map<Material, Integer> minedBlocks, int totalBlocks, long sessionDuration) {
        String title = "📊 Mining Session Summary";
        String description = String.format("**%s** completed a mining session", player.getName());
        
        List<WebhookManager.Field> fields = new ArrayList<>();
        fields.add(new WebhookManager.Field("Player", player.getName(), true));
        fields.add(new WebhookManager.Field("Total Blocks", String.valueOf(totalBlocks), true));
        fields.add(new WebhookManager.Field("Duration", formatDuration(sessionDuration), true));
        
        // Add ore breakdown
        StringBuilder oreBreakdown = new StringBuilder();
        for (Map.Entry<Material, Integer> entry : minedBlocks.entrySet()) {
            String oreName = getPrettyOreName(entry.getKey());
            int count = entry.getValue();
            oreBreakdown.append(String.format("%s: **%d**\n", oreName, count));
        }
        
        if (oreBreakdown.length() > 0) {
            fields.add(new WebhookManager.Field("Ores Found", oreBreakdown.toString(), false));
        }
        
        String color = "00ff00"; // Green for session summary
        
        webhookManager.sendEmbed(WEBHOOK_NAME, title, description, fields, color, null, "Ore Mining Notifier");
    }

    private String getPrettyOreName(Material material) {
        switch (material) {
            case DIAMOND_ORE:
            case DEEPSLATE_DIAMOND_ORE:
                return "Diamond Ore";
            case EMERALD_ORE:
            case DEEPSLATE_EMERALD_ORE:
                return "Emerald Ore";
            case GOLD_ORE:
            case DEEPSLATE_GOLD_ORE:
            case NETHER_GOLD_ORE:
                return "Gold Ore";
            case IRON_ORE:
            case DEEPSLATE_IRON_ORE:
                return "Iron Ore";
            case COPPER_ORE:
            case DEEPSLATE_COPPER_ORE:
                return "Copper Ore";
            case COAL_ORE:
            case DEEPSLATE_COAL_ORE:
                return "Coal Ore";
            case REDSTONE_ORE:
            case DEEPSLATE_REDSTONE_ORE:
                return "Redstone Ore";
            case LAPIS_ORE:
            case DEEPSLATE_LAPIS_ORE:
                return "Lapis Ore";
            case ANCIENT_DEBRIS:
                return "Ancient Debris";
            case NETHER_QUARTZ_ORE:
                return "Nether Quartz Ore";
            default:
                return material.name().replace("_", " ").toLowerCase();
        }
    }

    private String getOreColor(Material material) {
        switch (material) {
            case DIAMOND_ORE:
            case DEEPSLATE_DIAMOND_ORE:
                return "00ffff"; // Cyan
            case EMERALD_ORE:
            case DEEPSLATE_EMERALD_ORE:
                return "00ff00"; // Green
            case GOLD_ORE:
            case DEEPSLATE_GOLD_ORE:
            case NETHER_GOLD_ORE:
                return "ffaa00"; // Gold
            case IRON_ORE:
            case DEEPSLATE_IRON_ORE:
                return "cccccc"; // Light gray
            case COPPER_ORE:
            case DEEPSLATE_COPPER_ORE:
                return "ff6b35"; // Orange
            case COAL_ORE:
            case DEEPSLATE_COAL_ORE:
                return "555555"; // Dark gray
            case REDSTONE_ORE:
            case DEEPSLATE_REDSTONE_ORE:
                return "ff0000"; // Red
            case LAPIS_ORE:
            case DEEPSLATE_LAPIS_ORE:
                return "0000ff"; // Blue
            case ANCIENT_DEBRIS:
                return "800080"; // Purple
            case NETHER_QUARTZ_ORE:
                return "ffffff"; // White
            default:
                return "00ff00"; // Default green
        }
    }

    private String formatDuration(long durationMs) {
        long seconds = durationMs / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        
        if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }
} 