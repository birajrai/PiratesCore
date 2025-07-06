package xyz.dapirates.listener;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import xyz.dapirates.core.Core;
import xyz.dapirates.manager.WebhookManager;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.ArrayList;

public class ChatFilterListener implements Listener {
    private final Set<String> badWords = new HashSet<>();
    private final JavaPlugin plugin;
    private final Core core;
    private final WebhookManager webhookManager;

    private void loadBadWords() {
        badWords.clear();
        File file = new File(plugin.getDataFolder(), "BadWords.yml");
        if (!file.exists()) {
            plugin.saveResource("BadWords.yml", false);
        }
        if (file.exists()) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            List<String> words = config.getStringList("badwords");
            for (String word : words) {
                if (!word.isEmpty()) {
                    badWords.add(word.toLowerCase());
                }
            }
        }
    }

    public ChatFilterListener(Core core) {
        this.plugin = core;
        this.core = core;
        this.webhookManager = core.getWebhookManager();
        loadBadWords();
    }

    public void reload() {
        loadBadWords();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        String message = event.getMessage();
        String lowerMessage = message.toLowerCase();
        boolean found = false;
        String foundWord = null;
        for (String badWord : badWords) {
            if (badWord.isEmpty())
                continue;
            int index = lowerMessage.indexOf(badWord);
            while (index != -1) {
                String replacement = "*".repeat(badWord.length());
                message = message.substring(0, index) + replacement + message.substring(index + badWord.length());
                lowerMessage = message.toLowerCase();
                found = true;
                foundWord = badWord;
                index = lowerMessage.indexOf(badWord, index + replacement.length());
            }
        }
        if (found && foundWord != null) {
            // Send webhook embed
            String playerName = event.getPlayer().getName();
            String time = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());
            ArrayList<WebhookManager.Field> fields = new ArrayList<>();
            fields.add(new WebhookManager.Field("Player", playerName, true));
            fields.add(new WebhookManager.Field("Bad Word", foundWord, true));
            fields.add(new WebhookManager.Field("Time", time, false));
            webhookManager.sendEmbed(
                "badword",
                "🚨 Bad Word Detected",
                playerName + " used a bad word in chat!",
                fields,
                "ff0000", // Red color
                null,
                "Chat Filter"
            );
        }
        event.setMessage(message);
    }
}