package xyz.dapirates.manager;

import xyz.dapirates.core.Core;
import xyz.dapirates.utils.OreMiningConfig;

public class ConfigManager {
    private final Core plugin;
    private final OreMiningConfig oreMiningConfig;
    private final WebhookManager webhookManager;

    public ConfigManager(Core plugin, OreMiningConfig oreMiningConfig, WebhookManager webhookManager) {
        this.plugin = plugin;
        this.oreMiningConfig = oreMiningConfig;
        this.webhookManager = webhookManager;
    }

    public void reloadAll() {
        oreMiningConfig.reloadConfig();
        webhookManager.reloadConfig();
        plugin.getChatFilterListener().reload();
        plugin.getFeatureManager().reloadSettings();
        plugin.getLogger().info("All configs reloaded.");
    }
}