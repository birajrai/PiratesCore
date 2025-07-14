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
import xyz.dapirates.utils.ConfigUtils;

/**
 * Manages feature registration and toggling based on Settings.yml configuration.
 */
public class FeatureManager {

    private final JavaPlugin plugin;
    private final Map<String, Object> features;
    private YamlConfiguration settingsConfig;
    private ChatFilterListener chatFilterListener;

    public FeatureManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.features = new HashMap<>();
    }

    /**
     * Loads the Settings.yml configuration file.
     */
    private void loadSettings() {
        settingsConfig = (org.bukkit.configuration.file.YamlConfiguration) ConfigUtils.loadConfig(plugin, "Settings.yml");
    }

    /**
     * Checks if a feature is enabled in the config.
     * @param featureName The feature name
     * @return true if enabled, false otherwise
     */
    public boolean isFeatureEnabled(String featureName) {
        if (settingsConfig == null)
            loadSettings();
        return settingsConfig.getBoolean("features." + featureName, true);
    }

    /**
     * Registers all features based on config toggles.
     */
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

    /**
     * Registers a feature by name and instance.
     */
    private void registerFeature(String name, Object feature) {
        features.put(name, feature);
        plugin.getServer().getPluginManager().registerEvents((org.bukkit.event.Listener) feature, plugin);
    }

    /**
     * Registers a feature by name and supplier.
     */
    private void registerFeature(String name, Supplier<Object> featureSupplier) {
        Object feature = featureSupplier.get();
        registerFeature(name, feature);
    }

    /**
     * Gets a registered feature by name and type.
     */
    public <T> T getFeature(String name, Class<T> type) {
        Object feature = features.get(name);
        if (type.isInstance(feature)) {
            return type.cast(feature);
        }
        return null;
    }

    /**
     * Gets the registered OreMiningListener.
     */
    public OreMiningListener getOreMiningListener() {
        return getFeature("oremining", OreMiningListener.class);
    }

    /**
     * Gets the registered BetterMending listener.
     */
    public BetterMending getBetterMending() {
        return getFeature("bettermending", BetterMending.class);
    }

    /**
     * Gets the registered ChatFilterListener.
     */
    public ChatFilterListener getChatFilterListener() {
        return getFeature("chatfilter", ChatFilterListener.class);
    }

    /**
     * Unregisters all features.
     */
    public void unregisterAll() {
        features.clear();
    }

    /**
     * Reloads the Settings.yml configuration.
     */
    public void reloadSettings() {
        loadSettings();
    }
}