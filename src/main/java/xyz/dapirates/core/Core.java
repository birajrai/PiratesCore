package xyz.dapirates.core;

import org.bukkit.plugin.java.JavaPlugin;
import xyz.dapirates.listener.OreMiningListener;
import xyz.dapirates.listener.ChatFilterListener;
import xyz.dapirates.manager.CommandManager;
import xyz.dapirates.manager.FeatureManager;
import xyz.dapirates.manager.DatabaseManager;
import xyz.dapirates.manager.MessageManager;
import xyz.dapirates.manager.WebhookManager;
import xyz.dapirates.manager.OreMiningWebhook;
import xyz.dapirates.manager.ConfigManager;
import xyz.dapirates.utils.OreMiningConfig;

public class Core extends JavaPlugin {

    private OreMiningListener oreMiningListener;
    private OreMiningConfig oreMiningConfig;
    private CommandManager commandManager;
    private FeatureManager featureManager;
    private DatabaseManager databaseManager;
    private MessageManager messageManager;
    private WebhookManager webhookManager;
    private OreMiningWebhook oreMiningWebhook;
    private ConfigManager configManager;
    private ChatFilterListener chatFilterListener;

    @Override
    public void onEnable() {
        // Initialize managers
        initializeManagers();

        // Register features (now includes ChatFilter)
        featureManager.registerFeatures();

        // Register commands
        commandManager.registerCommands(oreMiningListener);

        getLogger().info("Core plugin enabled, BetterMending and OreMining registered!");
    }

    @Override
    public void onDisable() {
        // Flush all mining sessions to the database before shutdown
        if (oreMiningListener != null && databaseManager != null && databaseManager.isDatabaseAvailable()) {
            // Flush all active mining sessions to database asynchronously
            oreMiningListener.flushAllMiningSessionsToStats();

            // Give a small delay to allow async operations to complete
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        if (databaseManager != null) {
            databaseManager.close();
        }
        getLogger().info("Core plugin disabled!");
    }

    private void initializeManagers() {
        // Initialize ore mining config
        oreMiningConfig = new OreMiningConfig(this);

        // Initialize managers using a more abstract approach
        commandManager = new CommandManager(this);
        featureManager = new FeatureManager(this);
        databaseManager = new DatabaseManager(this);
        messageManager = new MessageManager(this);
        webhookManager = new WebhookManager(this);
        oreMiningWebhook = new OreMiningWebhook(this, webhookManager);
        configManager = new ConfigManager(this, oreMiningConfig, webhookManager);

        // Initialize ore mining feature after managers
        oreMiningListener = new OreMiningListener(this);
    }

    public OreMiningListener getOreMiningListener() {
        return oreMiningListener;
    }

    public OreMiningConfig getOreMiningConfig() {
        return oreMiningConfig;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public WebhookManager getWebhookManager() {
        return webhookManager;
    }

    public OreMiningWebhook getOreMiningWebhook() {
        return oreMiningWebhook;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public ChatFilterListener getChatFilterListener() {
        return featureManager.getChatFilterListener();
    }

    public FeatureManager getFeatureManager() {
        return featureManager;
    }
}