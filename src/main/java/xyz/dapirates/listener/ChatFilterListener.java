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
import java.util.Deque;
import java.util.LinkedList;

public class ChatFilterListener implements Listener {
    private final Set<String> badWords = new HashSet<>();
    private final JavaPlugin plugin;
    private final Core core;
    private final WebhookManager webhookManager;

    private static final int CHAT_HISTORY_SIZE = 5;
    private static final Deque<String> chatHistory = new LinkedList<>();

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
        boolean found = false;
        String foundWord = null;
        for (String badWord : badWords) {
            if (badWord.isEmpty())
                continue;
            // Regex to match the bad word as a whole word, at the start, or at the end of a
            // word, but not in the middle
            Pattern pattern = Pattern.compile(
                    "\\b" + Pattern.quote(badWord) + "\\b" + // exact word
                            "|\\b" + Pattern.quote(badWord) + "\\w+\\b" + // prefix
                            "|\\b\\w+" + Pattern.quote(badWord) + "\\b", // suffix
                    Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(message);
            StringBuffer sb = new StringBuffer();
            while (matcher.find()) {
                String matchedWord = matcher.group();
                String replacement = "*".repeat(matchedWord.length());
                matcher.appendReplacement(sb, replacement);
                found = true;
                foundWord = badWord;
            }
            matcher.appendTail(sb);
            message = sb.toString();
        }
        // Add the current message to chat history
        synchronized (chatHistory) {
            if (chatHistory.size() >= CHAT_HISTORY_SIZE) {
                chatHistory.removeFirst();
            }
            chatHistory.addLast(event.getPlayer().getName() + ": " + event.getMessage());
        }
        if (found && foundWord != null) {
            // Send webhook embed
            String playerName = event.getPlayer().getName();
            String time = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());
            ArrayList<WebhookManager.Field> fields = new ArrayList<>();
            fields.add(new WebhookManager.Field("Player", playerName, true));
            fields.add(new WebhookManager.Field("Bad Word", foundWord, true));
            fields.add(new WebhookManager.Field("Time", time, true));
            fields.add(new WebhookManager.Field("Full Message", "```" + event.getMessage() + "```", false));
            // Add last 5 messages as code block
            StringBuilder history = new StringBuilder();
            synchronized (chatHistory) {
                for (String msg : chatHistory) {
                    history.append(msg).append("\n");
                }
            }
            fields.add(new WebhookManager.Field("Last 5 Messages", "```" + history.toString().trim() + "```", false));
            webhookManager.sendEmbed(
                    "badword",
                    "🚨 Bad Word Detected",
                    "Player **" + playerName + "** used a prohibited word in chat!",
                    fields,
                    "ff0000", // Red color
                    null,
                    "Chat Filter Bot | PiratesAddons");
        }
        event.setMessage(message);
    }
}