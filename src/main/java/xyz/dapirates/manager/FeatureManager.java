package xyz.dapirates.manager;

import org.bukkit.plugin.java.JavaPlugin;
import xyz.dapirates.listener.BetterMending;
import xyz.dapirates.listener.OreMiningListener;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import xyz.dapirates.listener.ChatFilterListener;

public class FeatureManager {

    private final JavaPlugin plugin;
    private final Map<String, Object> features;
    private YamlConfiguration settingsConfig;
    private ChatFilterListener chatFilterListener;

    public FeatureManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.features = new HashMap<>();
    }

    private void loadSettings() {
        File settingsFile = new File(plugin.getDataFolder(), "Settings.yml");
        if (!settingsFile.exists()) {
            plugin.saveResource("Settings.yml", false);
        }
        settingsConfig = YamlConfiguration.loadConfiguration(settingsFile);
    }

    public boolean isFeatureEnabled(String featureName) {
        if (settingsConfig == null)
            loadSettings();
        return settingsConfig.getBoolean("features." + featureName, true);
    }

    public void registerFeatures() {
        loadSettings();
        // Log all features and their status
        if (settingsConfig != null && settingsConfig.isConfigurationSection("features")) {
            plugin.getLogger().info("[PiratesAddons] Feature status:");
            for (String feature : settingsConfig.getConfigurationSection("features").getKeys(false)) {
                boolean enabled = isFeatureEnabled(feature);
                plugin.getLogger().info("  - " + feature + ": " + (enabled ? "ENABLED" : "DISABLED"));
            }
        }
        // Register features using a more abstract approach
        if (isFeatureEnabled("BetterMending")) {
            registerFeature("bettermending", BetterMending::new);
        }
        // Register ore mining feature (already initialized in Core)
        if (isFeatureEnabled("OreMining")) {
            OreMiningListener oreMiningListener = ((xyz.dapirates.core.Core) plugin).getOreMiningListener();
            registerFeature("oremining", oreMiningListener);
        }
        // Register ChatFilter feature
        if (isFeatureEnabled("ChatFilter")) {
            chatFilterListener = new ChatFilterListener((xyz.dapirates.core.Core) plugin);
            registerFeature("chatfilter", chatFilterListener);
        }
    }

    private void registerFeature(String name, Object feature) {
        features.put(name, feature);
        plugin.getServer().getPluginManager().registerEvents((org.bukkit.event.Listener) feature, plugin);
    }

    private void registerFeature(String name, Supplier<Object> featureSupplier) {
        Object feature = featureSupplier.get();
        registerFeature(name, feature);
    }

    public <T> T getFeature(String name, Class<T> type) {
        Object feature = features.get(name);
        if (type.isInstance(feature)) {
            return type.cast(feature);
        }
        return null;
    }

    public OreMiningListener getOreMiningListener() {
        return getFeature("oremining", OreMiningListener.class);
    }

    public BetterMending getBetterMending() {
        return getFeature("bettermending", BetterMending.class);
    }

    public ChatFilterListener getChatFilterListener() {
        return getFeature("chatfilter", ChatFilterListener.class);
    }

    public void unregisterAll() {
        features.clear();
    }

    public void reloadSettings() {
        loadSettings();
    }
}