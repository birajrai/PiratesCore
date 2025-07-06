package xyz.dapirates.managers;

import xyz.dapirates.Core;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class WebhookManager {
    private final Core plugin;
    private final File configFile;
    private FileConfiguration config;
    private final HttpClient httpClient;
    private Map<String, String> webhooks;

    public WebhookManager(Core plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "webhook.yml");
        this.httpClient = HttpClient.newHttpClient();
        loadConfig();
    }

    public void loadConfig() {
        if (!configFile.exists()) {
            plugin.saveResource("webhook.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);
        webhooks = new HashMap<>();
        
        // Load all webhook URLs from the config
        for (String key : config.getKeys(false)) {
            String url = config.getString(key);
            if (url != null && !url.isEmpty() && !url.contains("YOUR_WEBHOOK_URL_HERE")) {
                webhooks.put(key, url);
            }
        }
    }

    public void reloadConfig() {
        loadConfig();
    }

    public String getWebhookUrl(String name) {
        if (name == null || name.isEmpty()) {
            // Return the first available webhook if no name specified
            return webhooks.values().stream().findFirst().orElse(null);
        }
        return webhooks.get(name);
    }

    public void sendSimple(String webhookName, String content) {
        String url = getWebhookUrl(webhookName);
        if (url == null || url.isEmpty()) return;
        CompletableFuture.runAsync(() -> {
            try {
                String json = "{\"content\":\"" + escapeJson(content) + "\"}";
                sendWebhook(url, json);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to send webhook", e);
            }
        });
    }

    public void sendEmbed(String webhookName, String title, String description, List<Field> fields, String color, String avatarUrl, String username) {
        String url = getWebhookUrl(webhookName);
        if (url == null || url.isEmpty()) return;
        CompletableFuture.runAsync(() -> {
            try {
                StringBuilder json = new StringBuilder();
                json.append("{");
                if (username != null && !username.isEmpty()) {
                    json.append("\"username\":\"").append(escapeJson(username)).append("\",");
                }
                if (avatarUrl != null && !avatarUrl.isEmpty()) {
                    json.append("\"avatar_url\":\"").append(escapeJson(avatarUrl)).append("\",");
                }
                json.append("\"embeds\":[{");
                if (title != null) json.append("\"title\":\"").append(escapeJson(title)).append("\",");
                if (description != null) json.append("\"description\":\"").append(escapeJson(description)).append("\",");
                if (color != null) json.append("\"color\":").append(hexToDecimal(color)).append(",");
                if (fields != null && !fields.isEmpty()) {
                    json.append("\"fields\":[");
                    for (int i = 0; i < fields.size(); i++) {
                        Field f = fields.get(i);
                        json.append("{\"name\":\"").append(escapeJson(f.name)).append("\",\"value\":\"").append(escapeJson(f.value)).append("\",\"inline\":").append(f.inline).append("}");
                        if (i < fields.size() - 1) json.append(",");
                    }
                    json.append("],");
                }
                json.append("\"timestamp\":\"").append(Instant.now().toString()).append("\"");
                json.append("}]}\n");
                sendWebhook(url, json.toString());
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to send webhook embed", e);
            }
        });
    }

    public void sendIndividualBlockNotification(org.bukkit.entity.Player player, org.bukkit.Material material, org.bukkit.Location location, boolean isTNT) {
        String webhookName = "oremining";
        String url = getWebhookUrl(webhookName);
        if (url == null || url.isEmpty()) return;

        CompletableFuture.runAsync(() -> {
            try {
                String title = isTNT ? "💥 TNT Mining Alert" : "⛏️ Ore Mining Alert";
                String description = String.format("**%s** found **%s**", 
                    player.getName(), 
                    getPrettyOreName(material));
                
                List<Field> fields = new ArrayList<>();
                fields.add(new Field("Player", player.getName(), true));
                fields.add(new Field("Ore", getPrettyOreName(material), true));
                fields.add(new Field("Method", isTNT ? "TNT" : "Mining", true));
                fields.add(new Field("Location", String.format("X: %d, Y: %d, Z: %d", 
                    location.getBlockX(), location.getBlockY(), location.getBlockZ()), false));
                fields.add(new Field("World", location.getWorld().getName(), false));
                
                String color = getOreColor(material);
                
                sendEmbed(webhookName, title, description, fields, color, null, "Ore Mining Notifier");
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to send individual block webhook", e);
            }
        });
    }

    public void sendBatchedSessionNotification(org.bukkit.entity.Player player, Map<org.bukkit.Material, Integer> minedBlocks, int totalBlocks, long sessionDuration) {
        String webhookName = "oremining";
        String url = getWebhookUrl(webhookName);
        if (url == null || url.isEmpty()) return;

        CompletableFuture.runAsync(() -> {
            try {
                String title = "📊 Mining Session Summary";
                String description = String.format("**%s** completed a mining session", player.getName());
                
                List<Field> fields = new ArrayList<>();
                fields.add(new Field("Player", player.getName(), true));
                fields.add(new Field("Total Blocks", String.valueOf(totalBlocks), true));
                fields.add(new Field("Duration", formatDuration(sessionDuration), true));
                
                // Add ore breakdown
                StringBuilder oreBreakdown = new StringBuilder();
                for (Map.Entry<org.bukkit.Material, Integer> entry : minedBlocks.entrySet()) {
                    String oreName = getPrettyOreName(entry.getKey());
                    int count = entry.getValue();
                    oreBreakdown.append(String.format("%s: **%d**\n", oreName, count));
                }
                
                if (oreBreakdown.length() > 0) {
                    fields.add(new Field("Ores Found", oreBreakdown.toString(), false));
                }
                
                String color = "00ff00"; // Green for session summary
                
                sendEmbed(webhookName, title, description, fields, color, null, "Ore Mining Notifier");
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to send batched session webhook", e);
            }
        });
    }

    private String getPrettyOreName(org.bukkit.Material material) {
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

    private String getOreColor(org.bukkit.Material material) {
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

    private void sendWebhook(String url, String json) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 204) {
                plugin.getLogger().warning("Discord webhook failed with status: " + response.statusCode() + " - " + response.body());
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to send Discord webhook", e);
        }
    }

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }

    private int hexToDecimal(String hex) {
        try {
            return Integer.parseInt(hex.replace("#", ""), 16);
        } catch (NumberFormatException e) {
            return 0x00ff00; // Default green
        }
    }

    public static class Field {
        public final String name;
        public final String value;
        public final boolean inline;
        public Field(String name, String value, boolean inline) {
            this.name = name;
            this.value = value;
            this.inline = inline;
        }
    }
} 