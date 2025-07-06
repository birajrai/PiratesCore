package xyz.dapirates;

import org.bukkit.plugin.java.JavaPlugin;
import xyz.dapirates.features.OreMiningNotifier;
import xyz.dapirates.managers.CommandManager;
import xyz.dapirates.managers.FeatureManager;
import xyz.dapirates.managers.DatabaseManager;
import xyz.dapirates.managers.MessageManager;
import xyz.dapirates.managers.WebhookManager;
import xyz.dapirates.managers.OreMiningWebhook;
import xyz.dapirates.managers.ConfigManager;
import xyz.dapirates.utils.OreMiningConfig;

public class Core extends JavaPlugin {

    private OreMiningNotifier oreMiningNotifier;
    private OreMiningConfig oreMiningConfig;
    private CommandManager commandManager;
    private FeatureManager featureManager;
    private DatabaseManager databaseManager;
    private MessageManager messageManager;
    private WebhookManager webhookManager;
    private OreMiningWebhook oreMiningWebhook;
    private ConfigManager configManager;

    @Override
    public void onEnable() {
        // Initialize managers
        initializeManagers();

        // Register features
        featureManager.registerFeatures();

        // Register commands
        commandManager.registerCommands(oreMiningNotifier);

        getLogger().info("Core plugin enabled, BetterMending and OreMining registered!");
    }

    @Override
    public void onDisable() {
        // Flush all mining sessions to the database before shutdown
        if (oreMiningNotifier != null && databaseManager != null && databaseManager.isDatabaseAvailable()) {
            // Flush all active mining sessions to database asynchronously
            oreMiningNotifier.flushAllMiningSessionsToStats();

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
        oreMiningNotifier = new OreMiningNotifier(this);
    }

    public OreMiningNotifier getOreMiningNotifier() {
        return oreMiningNotifier;
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

}