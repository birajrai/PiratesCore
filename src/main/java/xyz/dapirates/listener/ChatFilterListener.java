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
import xyz.dapirates.utils.ConfigUtils;

/**
 * Listens for player chat events and filters bad words, sending webhook notifications if needed.
 */
public class ChatFilterListener implements Listener {
    private final Set<String> badWords = new HashSet<>();
    private final JavaPlugin plugin;
    private final Core core;
    private final WebhookManager webhookManager;

    private static final int CHAT_HISTORY_SIZE = 5;
    private static final Deque<String> chatHistory = new LinkedList<>();

    /**
     * Loads the list of bad words from BadWords.yml.
     */
    private void loadBadWords() {
        badWords.clear();
        final org.bukkit.configuration.file.FileConfiguration config = ConfigUtils.loadConfig(plugin, "BadWords.yml");
        final List<String> words = config.getStringList("badwords");
        for (final String word : words) {
            if (!word.isEmpty()) {
                badWords.add(word.toLowerCase());
            }
        }
    }

    /**
     * Constructs a ChatFilterListener and loads bad words.
     */
    public ChatFilterListener(final Core core) {
        this.plugin = core;
        this.core = core;
        this.webhookManager = core.getWebhookManager();
        loadBadWords();
    }

    /**
     * Reloads the bad words list from config.
     */
    public void reload() {
        loadBadWords();
    }

    /**
     * Handles player chat events, filtering bad words and sending webhooks if needed.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerChat(final AsyncPlayerChatEvent event) {
        String message = event.getMessage();
        boolean found = false;
        String foundWord = null;
        for (final String badWord : badWords) {
            if (badWord.isEmpty())
                continue;
            // Regex to match the bad word as a whole word, at the start, or at the end of a word, but not in the middle
            final Pattern pattern = Pattern.compile(
                    "\\b" + Pattern.quote(badWord) + "\\b" + // exact word
                            "|\\b" + Pattern.quote(badWord) + "\\w+\\b" + // prefix
                            "|\\b\\w+" + Pattern.quote(badWord) + "\\b", // suffix
                    Pattern.CASE_INSENSITIVE);
            final Matcher matcher = pattern.matcher(message);
            final StringBuffer sb = new StringBuffer();
            while (matcher.find()) {
                final String matchedWord = matcher.group();
                final String replacement = "*".repeat(matchedWord.length());
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
            final String playerName = event.getPlayer().getName();
            final String time = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());
            final ArrayList<WebhookManager.Field> fields = new ArrayList<>();
            fields.add(new WebhookManager.Field("Player", playerName, true));
            fields.add(new WebhookManager.Field("Bad Word", foundWord, true));
            fields.add(new WebhookManager.Field("Time", time, true));
            fields.add(new WebhookManager.Field("Full Message", "```" + event.getMessage() + "```", false));
            // Add last 5 messages as code block
            final StringBuilder history = new StringBuilder();
            synchronized (chatHistory) {
                for (final String msg : chatHistory) {
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