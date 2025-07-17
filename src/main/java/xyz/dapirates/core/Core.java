package xyz.dapirates.core;

import org.bukkit.plugin.java.JavaPlugin;
import xyz.dapirates.listener.OreMiningListener;
import xyz.dapirates.manager.CommandManager;
import xyz.dapirates.manager.FeatureManager;
import xyz.dapirates.manager.DatabaseManager;
import xyz.dapirates.manager.MessageManager;
import xyz.dapirates.manager.WebhookManager;
import xyz.dapirates.manager.OreMiningWebhook;
import xyz.dapirates.manager.ConfigManager;
import xyz.dapirates.utils.OreMiningConfig;
import xyz.dapirates.manager.PlayerStatsHandler;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import xyz.dapirates.pirates.StatsTopGUI;

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
    private PlayerStatsHandler playerStatsHandler;
    private Economy economy;

    @Override
    public void onEnable() {
        // Setup Vault economy
        if (!setupEconomy()) {
            getLogger().severe("Vault with an economy plugin is required for balance tracking!");
        }
        // Initialize managers
        initializeManagers();

        // Register features (now includes ChatFilter)
        featureManager.registerFeatures();

        // Register commands
        commandManager.registerCommands(oreMiningListener);

        // Register player stats listener only if Stats feature is enabled
        if (featureManager.isFeatureEnabled("Stats")) {
            xyz.dapirates.listener.PlayerStatsListener statsListener = new xyz.dapirates.listener.PlayerStatsListener(this);
            // Inject StatsTopGUI
            xyz.dapirates.pirates.StatsTopGUI topGUI = (xyz.dapirates.pirates.StatsTopGUI) commandManager.getCommand("topgui");
            if (topGUI != null) {
                statsListener.setStatsTopGUI(topGUI);
            }
            getServer().getPluginManager().registerEvents(statsListener, this);
        }

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

        // Initialize player stats handler only if Stats feature is enabled
        if (featureManager.isFeatureEnabled("Stats")) {
            playerStatsHandler = new PlayerStatsHandler(this, configManager);
        } else {
            playerStatsHandler = null;
        }
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    public Economy getEconomy() {
        return economy;
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

    public FeatureManager getFeatureManager() {
        return featureManager;
    }

    public PlayerStatsHandler getPlayerStatsHandler() {
        return playerStatsHandler;
    }

    public void reloadPlugin() {
        try {
            // Unregister all listeners and commands if needed (not implemented here, but should be for full safety)
            // Re-initialize managers and listeners
            initializeManagers();
            // Re-register features
            featureManager.registerFeatures();
            // Re-register commands
            commandManager.registerCommands(oreMiningListener);
            // Register or unregister PlayerStatsListener according to the current 'Stats' toggle
            if (featureManager.isFeatureEnabled("Stats")) {
                getServer().getPluginManager().registerEvents(new xyz.dapirates.listener.PlayerStatsListener(this), this);
            }
            getLogger().info("Plugin reloaded and features re-evaluated according to config.");
        } catch (Exception e) {
            getLogger().severe("Error during plugin reload: " + e.getMessage());
            e.printStackTrace();
        }
    }
}