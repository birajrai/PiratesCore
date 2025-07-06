package xyz.dapirates.manager;

import xyz.dapirates.core.Core;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.ArrayList;
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
        this.configFile = new File(plugin.getDataFolder(), "Webhook.yml");
        this.httpClient = HttpClient.newHttpClient();
        loadConfig();
    }

    public void loadConfig() {
        if (!configFile.exists()) {
            plugin.saveResource("Webhook.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);
        webhooks = new HashMap<>();

        // Support old format: webhooks: { name: url }
        if (config.isConfigurationSection("webhooks")) {
            plugin.getLogger().warning(
                    "[PiratesAddons] Detected old Webhook.yml format (webhooks: section). Please migrate to the new flat format.");
            for (String key : config.getConfigurationSection("webhooks").getKeys(false)) {
                String url = config.getString("webhooks." + key);
                if (url != null && !url.isEmpty() && !url.contains("YOUR_WEBHOOK_URL_HERE")) {
                    webhooks.put(key, url);
                }
            }
        } else {
            // New format: top-level keys
            for (String key : config.getKeys(false)) {
                String url = config.getString(key);
                if (url != null && !url.isEmpty() && !url.contains("YOUR_WEBHOOK_URL_HERE")) {
                    webhooks.put(key, url);
                }
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
        if (url == null || url.isEmpty())
            return;
        CompletableFuture.runAsync(() -> {
            try {
                String json = "{\"content\":\"" + escapeJson(content) + "\"}";
                sendWebhook(url, json);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to send webhook", e);
            }
        });
    }

    public void sendEmbed(String webhookName, String title, String description, List<Field> fields, String color,
            String avatarUrl, String username) {
        String url = getWebhookUrl(webhookName);
        if (url == null || url.isEmpty())
            return;
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
                if (title != null)
                    json.append("\"title\":\"").append(escapeJson(title)).append("\",");
                if (description != null)
                    json.append("\"description\":\"").append(escapeJson(description)).append("\",");
                if (color != null)
                    json.append("\"color\":").append(hexToDecimal(color)).append(",");
                if (fields != null && !fields.isEmpty()) {
                    json.append("\"fields\":[");
                    for (int i = 0; i < fields.size(); i++) {
                        Field f = fields.get(i);
                        json.append("{\"name\":\"").append(escapeJson(f.name)).append("\",\"value\":\"")
                                .append(escapeJson(f.value)).append("\",\"inline\":").append(f.inline).append("}");
                        if (i < fields.size() - 1)
                            json.append(",");
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

    private void sendWebhook(String url, String json) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 204) {
                plugin.getLogger().warning(
                        "Discord webhook failed with status: " + response.statusCode() + " - " + response.body());
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