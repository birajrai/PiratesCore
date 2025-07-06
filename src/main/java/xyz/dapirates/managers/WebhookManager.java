package xyz.dapirates.managers;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import xyz.dapirates.Core;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class WebhookManager {

    private final Core plugin;
    private final File configFile;
    private FileConfiguration config;
    private final HttpClient httpClient;
    private final Map<String, Long> rateLimitMap;
    private boolean webhookEnabled;
    private String webhookUrl;
    private String username;
    private String avatarUrl;

    public WebhookManager(Core plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "webhook.yml");
        this.httpClient = HttpClient.newHttpClient();
        this.rateLimitMap = new ConcurrentHashMap<>();
        loadConfig();
    }

    private void loadConfig() {
        if (!configFile.exists()) {
            plugin.saveResource("webhook.yml", false);
        }

        config = YamlConfiguration.loadConfiguration(configFile);
        setupDefaults();
        loadSettings();
    }

    private void setupDefaults() {
        ConfigurationSection oremining = config.getConfigurationSection("oremining");
        if (oremining == null) {
            config.createSection("oremining");
            oremining = config.getConfigurationSection("oremining");
        }

        // Basic settings
        oremining.addDefault("enabled", false);
        oremining.addDefault("webhook-url", "https://discord.com/api/webhooks/YOUR_WEBHOOK_ID/YOUR_WEBHOOK_TOKEN");
        oremining.addDefault("username", "OreMining Bot");
        oremining.addDefault("avatar-url", "");

        // Embed settings
        ConfigurationSection embed = oremining.createSection("embed");
        embed.addDefault("enabled", true);
        embed.addDefault("title", "⛏️ Ore Mining Activity");
        embed.addDefault("color", "#00ff00");
        embed.addDefault("footer", "OreMining Notifications");
        embed.addDefault("footer-icon", "https://i.imgur.com/8tBXd6H.png");
        embed.addDefault("show-server-name", true);
        embed.addDefault("show-coordinates", true);
        embed.addDefault("show-timestamp", true);
        embed.addDefault("show-player-avatar", true);

        // Thumbnail settings
        ConfigurationSection thumbnail = embed.createSection("thumbnail");
        thumbnail.addDefault("enabled", true);
        thumbnail.addDefault("use-player-avatar", true);
        thumbnail.addDefault("custom-url", "");

        // Field settings
        ConfigurationSection fields = embed.createSection("fields");
        fields.addDefault("show-player", true);
        fields.addDefault("show-block", true);
        fields.addDefault("show-coordinates", true);
        fields.addDefault("show-world", true);
        fields.addDefault("show-duration", true);
        fields.addDefault("show-total", true);

        // Notification settings
        ConfigurationSection notifications = oremining.createSection("notifications");
        notifications.addDefault("individual-blocks", true);
        notifications.addDefault("batched-sessions", true);
        notifications.addDefault("tnt-mining", true);
        notifications.addDefault("min-blocks-for-batch", 1);
        notifications.addDefault("include-console", false);
        notifications.addDefault("rate-limit", 0);

        // Block colors
        ConfigurationSection colors = oremining.createSection("blocks.colors");
        colors.addDefault("diamond_ore", "#00ffff");
        colors.addDefault("deepslate_diamond_ore", "#00ffff");
        colors.addDefault("emerald_ore", "#00ff00");
        colors.addDefault("deepslate_emerald_ore", "#00ff00");
        colors.addDefault("gold_ore", "#ffd700");
        colors.addDefault("deepslate_gold_ore", "#ffd700");
        colors.addDefault("iron_ore", "#c0c0c0");
        colors.addDefault("deepslate_iron_ore", "#c0c0c0");
        colors.addDefault("copper_ore", "#cd7f32");
        colors.addDefault("deepslate_copper_ore", "#cd7f32");
        colors.addDefault("coal_ore", "#696969");
        colors.addDefault("deepslate_coal_ore", "#696969");
        colors.addDefault("redstone_ore", "#ff0000");
        colors.addDefault("deepslate_redstone_ore", "#ff0000");
        colors.addDefault("lapis_ore", "#0000ff");
        colors.addDefault("deepslate_lapis_ore", "#0000ff");
        colors.addDefault("ancient_debris", "#800080");
        colors.addDefault("nether_gold_ore", "#ffd700");
        colors.addDefault("nether_quartz_ore", "#ffffff");

        // Block emojis
        ConfigurationSection emojis = oremining.createSection("blocks.emojis");
        emojis.addDefault("diamond_ore", "💎");
        emojis.addDefault("deepslate_diamond_ore", "💎");
        emojis.addDefault("emerald_ore", "💚");
        emojis.addDefault("deepslate_emerald_ore", "💚");
        emojis.addDefault("gold_ore", "🟡");
        emojis.addDefault("deepslate_gold_ore", "🟡");
        emojis.addDefault("iron_ore", "⚙️");
        emojis.addDefault("deepslate_iron_ore", "⚙️");
        emojis.addDefault("copper_ore", "🟠");
        emojis.addDefault("deepslate_copper_ore", "🟠");
        emojis.addDefault("coal_ore", "⚫");
        emojis.addDefault("deepslate_coal_ore", "⚫");
        emojis.addDefault("redstone_ore", "🔴");
        emojis.addDefault("deepslate_redstone_ore", "🔴");
        emojis.addDefault("lapis_ore", "🔵");
        emojis.addDefault("deepslate_lapis_ore", "🔵");
        emojis.addDefault("ancient_debris", "🟣");
        emojis.addDefault("nether_gold_ore", "🟡");
        emojis.addDefault("nether_quartz_ore", "⚪");

        config.options().copyDefaults(true);
        saveConfig();
    }

    private void loadSettings() {
        ConfigurationSection oremining = config.getConfigurationSection("oremining");
        if (oremining == null) {
            webhookEnabled = false;
            return;
        }

        webhookEnabled = oremining.getBoolean("enabled", false);
        webhookUrl = oremining.getString("webhook-url", "");
        username = oremining.getString("username", "OreMining Bot");
        avatarUrl = oremining.getString("avatar-url", "");

        if (webhookEnabled && (webhookUrl.isEmpty() || webhookUrl.contains("YOUR_WEBHOOK"))) {
            plugin.getLogger().warning("Discord webhook is enabled but no valid webhook URL is configured!");
            webhookEnabled = false;
        }
    }

    public void reloadConfig() {
        loadConfig();
    }

    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save webhook.yml: " + e.getMessage());
        }
    }

    public boolean isWebhookEnabled() {
        return webhookEnabled;
    }

    public void sendIndividualBlockNotification(Player player, Material material, org.bukkit.Location location, boolean isTNT) {
        if (!webhookEnabled || !config.getBoolean("oremining.notifications.individual-blocks", true)) {
            return;
        }

        if (isTNT && !config.getBoolean("oremining.notifications.tnt-mining", true)) {
            return;
        }

        if (!checkRateLimit("individual")) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                String json = createIndividualBlockEmbed(player, material, location, isTNT);
                sendWebhook(json);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to send individual block webhook", e);
            }
        });
    }

    public void sendBatchedSessionNotification(Player player, Map<Material, Integer> minedBlocks, int totalBlocks, long sessionDuration) {
        if (!webhookEnabled || !config.getBoolean("oremining.notifications.batched-sessions", true)) {
            return;
        }

        if (totalBlocks < config.getInt("oremining.notifications.min-blocks-for-batch", 1)) {
            return;
        }

        if (!checkRateLimit("batched")) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                String json = createBatchedSessionEmbed(player, minedBlocks, totalBlocks, sessionDuration);
                sendWebhook(json);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to send batched session webhook", e);
            }
        });
    }

    private String createIndividualBlockEmbed(Player player, Material material, org.bukkit.Location location, boolean isTNT) {
        ConfigurationSection embedConfig = config.getConfigurationSection("oremining.embed");
        if (embedConfig == null || !embedConfig.getBoolean("enabled", true)) {
            return createPlainTextMessage(player, material, location, isTNT);
        }

        String materialName = material.name().toLowerCase();
        String color = config.getString("oremining.blocks.colors." + materialName, "#00ff00");
        String emoji = config.getString("oremining.blocks.emojis." + materialName, "⛏️");
        
        StringBuilder json = new StringBuilder();
        json.append("{");
        
        // Webhook username and avatar
        if (!username.isEmpty()) {
            json.append("\"username\":\"").append(escapeJson(username)).append("\",");
        }
        if (!avatarUrl.isEmpty()) {
            json.append("\"avatar_url\":\"").append(escapeJson(avatarUrl)).append("\",");
        }
        
        // Embed
        json.append("\"embeds\":[{");
        json.append("\"title\":\"").append(escapeJson(embedConfig.getString("title", "⛏️ Ore Mining Activity"))).append("\",");
        json.append("\"color\":").append(hexToDecimal(color)).append(",");
        json.append("\"description\":\"").append(escapeJson(emoji + " **" + player.getName() + "** found **" + getPrettyOreName(material) + "**")).append("\",");
        
        // Fields
        List<String> fields = new ArrayList<>();
        ConfigurationSection fieldsConfig = embedConfig.getConfigurationSection("fields");
        
        if (fieldsConfig != null) {
            if (fieldsConfig.getBoolean("show-player", true)) {
                fields.add("{\"name\":\"Player\",\"value\":\"" + escapeJson(player.getName()) + "\",\"inline\":true}");
            }
            if (fieldsConfig.getBoolean("show-block", true)) {
                fields.add("{\"name\":\"Block\",\"value\":\"" + escapeJson(getPrettyOreName(material)) + "\",\"inline\":true}");
            }
            if (fieldsConfig.getBoolean("show-coordinates", true) && embedConfig.getBoolean("show-coordinates", true)) {
                fields.add("{\"name\":\"Location\",\"value\":\"" + escapeJson("X: " + location.getBlockX() + ", Y: " + location.getBlockY() + ", Z: " + location.getBlockZ()) + "\",\"inline\":true}");
            }
            if (fieldsConfig.getBoolean("show-world", true)) {
                fields.add("{\"name\":\"World\",\"value\":\"" + escapeJson(location.getWorld().getName()) + "\",\"inline\":true}");
            }
        }
        
        if (!fields.isEmpty()) {
            json.append("\"fields\":[").append(String.join(",", fields)).append("],");
        }
        
        // Thumbnail
        ConfigurationSection thumbnailConfig = embedConfig.getConfigurationSection("thumbnail");
        if (thumbnailConfig != null && thumbnailConfig.getBoolean("enabled", true)) {
            if (thumbnailConfig.getBoolean("use-player-avatar", true)) {
                String playerAvatar = "https://crafatar.com/avatars/" + player.getUniqueId() + "?overlay";
                json.append("\"thumbnail\":{\"url\":\"").append(escapeJson(playerAvatar)).append("\"},");
            } else if (!thumbnailConfig.getString("custom-url", "").isEmpty()) {
                json.append("\"thumbnail\":{\"url\":\"").append(escapeJson(thumbnailConfig.getString("custom-url"))).append("\"},");
            }
        }
        
        // Footer
        String footer = embedConfig.getString("footer", "OreMining Notifications");
        String footerIcon = embedConfig.getString("footer-icon", "");
        json.append("\"footer\":{\"text\":\"").append(escapeJson(footer)).append("\"");
        if (!footerIcon.isEmpty()) {
            json.append(",\"icon_url\":\"").append(escapeJson(footerIcon)).append("\"");
        }
        json.append("}");
        
        // Timestamp
        if (embedConfig.getBoolean("show-timestamp", true)) {
            json.append(",\"timestamp\":\"").append(Instant.now().toString()).append("\"");
        }
        
        json.append("}]}");
        
        return json.toString();
    }

    private String createBatchedSessionEmbed(Player player, Map<Material, Integer> minedBlocks, int totalBlocks, long sessionDuration) {
        ConfigurationSection embedConfig = config.getConfigurationSection("oremining.embed");
        if (embedConfig == null || !embedConfig.getBoolean("enabled", true)) {
            return createPlainTextBatchedMessage(player, minedBlocks, totalBlocks, sessionDuration);
        }

        StringBuilder json = new StringBuilder();
        json.append("{");
        
        // Webhook username and avatar
        if (!username.isEmpty()) {
            json.append("\"username\":\"").append(escapeJson(username)).append("\",");
        }
        if (!avatarUrl.isEmpty()) {
            json.append("\"avatar_url\":\"").append(escapeJson(avatarUrl)).append("\",");
        }
        
        // Embed
        json.append("\"embeds\":[{");
        json.append("\"title\":\"").append(escapeJson(embedConfig.getString("title", "⛏️ Ore Mining Activity"))).append("\",");
        json.append("\"color\":16776960,"); // Yellow color for batched sessions
        json.append("\"description\":\"").append(escapeJson("⛏️ **" + player.getName() + "** completed a mining session!")).append("\",");
        
        // Fields
        List<String> fields = new ArrayList<>();
        ConfigurationSection fieldsConfig = embedConfig.getConfigurationSection("fields");
        
        if (fieldsConfig != null) {
            if (fieldsConfig.getBoolean("show-player", true)) {
                fields.add("{\"name\":\"Player\",\"value\":\"" + escapeJson(player.getName()) + "\",\"inline\":true}");
            }
            if (fieldsConfig.getBoolean("show-total", true)) {
                fields.add("{\"name\":\"Total Blocks\",\"value\":\"" + escapeJson(String.valueOf(totalBlocks)) + "\",\"inline\":true}");
            }
            if (fieldsConfig.getBoolean("show-duration", true)) {
                long seconds = sessionDuration / 1000;
                String duration = seconds < 60 ? seconds + "s" : (seconds / 60) + "m " + (seconds % 60) + "s";
                fields.add("{\"name\":\"Duration\",\"value\":\"" + escapeJson(duration) + "\",\"inline\":true}");
            }
        }
        
        // Block breakdown
        if (!minedBlocks.isEmpty()) {
            StringBuilder breakdown = new StringBuilder();
            for (Map.Entry<Material, Integer> entry : minedBlocks.entrySet()) {
                String emoji = config.getString("oremining.blocks.emojis." + entry.getKey().name().toLowerCase(), "⛏️");
                breakdown.append(emoji).append(" ").append(getPrettyOreName(entry.getKey())).append(": **").append(entry.getValue()).append("**\n");
            }
            fields.add("{\"name\":\"Blocks Mined\",\"value\":\"" + escapeJson(breakdown.toString().trim()) + "\",\"inline\":false}");
        }
        
        if (!fields.isEmpty()) {
            json.append("\"fields\":[").append(String.join(",", fields)).append("],");
        }
        
        // Thumbnail
        ConfigurationSection thumbnailConfig = embedConfig.getConfigurationSection("thumbnail");
        if (thumbnailConfig != null && thumbnailConfig.getBoolean("enabled", true)) {
            if (thumbnailConfig.getBoolean("use-player-avatar", true)) {
                String playerAvatar = "https://crafatar.com/avatars/" + player.getUniqueId() + "?overlay";
                json.append("\"thumbnail\":{\"url\":\"").append(escapeJson(playerAvatar)).append("\"},");
            } else if (!thumbnailConfig.getString("custom-url", "").isEmpty()) {
                json.append("\"thumbnail\":{\"url\":\"").append(escapeJson(thumbnailConfig.getString("custom-url"))).append("\"},");
            }
        }
        
        // Footer
        String footer = embedConfig.getString("footer", "OreMining Notifications");
        String footerIcon = embedConfig.getString("footer-icon", "");
        json.append("\"footer\":{\"text\":\"").append(escapeJson(footer)).append("\"");
        if (!footerIcon.isEmpty()) {
            json.append(",\"icon_url\":\"").append(escapeJson(footerIcon)).append("\"");
        }
        json.append("}");
        
        // Timestamp
        if (embedConfig.getBoolean("show-timestamp", true)) {
            json.append(",\"timestamp\":\"").append(Instant.now().toString()).append("\"");
        }
        
        json.append("}]}");
        
        return json.toString();
    }

    private String createPlainTextMessage(Player player, Material material, org.bukkit.Location location, boolean isTNT) {
        String emoji = config.getString("oremining.blocks.emojis." + material.name().toLowerCase(), "⛏️");
        String message = emoji + " **" + player.getName() + "** found **" + getPrettyOreName(material) + "**";
        if (isTNT) {
            message += " (TNT)";
        }
        message += " at X:" + location.getBlockX() + " Y:" + location.getBlockY() + " Z:" + location.getBlockZ() + " in " + location.getWorld().getName();
        
        return "{\"content\":\"" + escapeJson(message) + "\"}";
    }

    private String createPlainTextBatchedMessage(Player player, Map<Material, Integer> minedBlocks, int totalBlocks, long sessionDuration) {
        StringBuilder message = new StringBuilder();
        message.append("⛏️ **").append(player.getName()).append("** completed a mining session!\n");
        message.append("Total blocks: **").append(totalBlocks).append("**\n");
        
        long seconds = sessionDuration / 1000;
        String duration = seconds < 60 ? seconds + "s" : (seconds / 60) + "m " + (seconds % 60) + "s";
        message.append("Duration: **").append(duration).append("**\n\n");
        
        message.append("**Blocks Mined:**\n");
        for (Map.Entry<Material, Integer> entry : minedBlocks.entrySet()) {
            String emoji = config.getString("oremining.blocks.emojis." + entry.getKey().name().toLowerCase(), "⛏️");
            message.append(emoji).append(" ").append(getPrettyOreName(entry.getKey())).append(": **").append(entry.getValue()).append("**\n");
        }
        
        return "{\"content\":\"" + escapeJson(message.toString().trim()) + "\"}";
    }

    private void sendWebhook(String json) {
        if (webhookUrl.isEmpty()) {
            return;
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
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

    private boolean checkRateLimit(String type) {
        int rateLimit = config.getInt("oremining.notifications.rate-limit", 0);
        if (rateLimit <= 0) {
            return true;
        }

        long currentTime = System.currentTimeMillis();
        long oneMinuteAgo = currentTime - 60000;
        
        // Clean old entries
        rateLimitMap.entrySet().removeIf(entry -> entry.getValue() < oneMinuteAgo);
        
        // Check rate limit
        long count = rateLimitMap.values().stream()
                .filter(time -> time > oneMinuteAgo)
                .count();
        
        if (count >= rateLimit) {
            return false;
        }
        
        rateLimitMap.put(type + "_" + currentTime, currentTime);
        return true;
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
                return material.name().replace("_", " ");
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
} 