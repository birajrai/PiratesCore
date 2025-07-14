package xyz.dapirates.manager;

import xyz.dapirates.core.Core;
import xyz.dapirates.utils.OreMiningConfig;

/**
 * Central manager for reloading all plugin configurations and listeners.
 */
public class ConfigManager {
    private final Core plugin;
    private final OreMiningConfig oreMiningConfig;
    private final WebhookManager webhookManager;

    public ConfigManager(Core plugin, OreMiningConfig oreMiningConfig, WebhookManager webhookManager) {
        this.plugin = plugin;
        this.oreMiningConfig = oreMiningConfig;
        this.webhookManager = webhookManager;
    }

    /**
     * Reloads all configs and listeners in the plugin.
     */
    public void reloadAll() {
        oreMiningConfig.reloadConfig();
        webhookManager.reloadConfig();
        plugin.getFeatureManager().getChatFilterListener().reload();
        plugin.getFeatureManager().reloadSettings();
        plugin.getLogger().info("All configs reloaded.");
    }
}