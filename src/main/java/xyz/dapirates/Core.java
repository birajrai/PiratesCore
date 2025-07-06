package xyz.dapirates;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.dapirates.features.BetterMending;
import xyz.dapirates.features.OreMiningNotifier;
import xyz.dapirates.command.ShowCommand;
import xyz.dapirates.command.OreMiningCommand;
import xyz.dapirates.managers.CommandManager;
import xyz.dapirates.managers.FeatureManager;
import xyz.dapirates.managers.DatabaseManager;
import xyz.dapirates.managers.MessageManager;
import xyz.dapirates.utils.OreMiningConfig;

public class Core extends JavaPlugin {
    
    private OreMiningNotifier oreMiningNotifier;
    private OreMiningConfig oreMiningConfig;
    private CommandManager commandManager;
    private FeatureManager featureManager;
    private DatabaseManager databaseManager;
    private MessageManager messageManager;
    
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
        if (databaseManager != null) {
            databaseManager.close();
        }
        getLogger().info("Core plugin disabled!");
    }
    
    private void initializeManagers() {
        // Initialize ore mining config
        oreMiningConfig = new OreMiningConfig(this);
        
        // Initialize managers first
        commandManager = new CommandManager(this);
        featureManager = new FeatureManager(this);
        databaseManager = new DatabaseManager(this);
        messageManager = new MessageManager(this);
        
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
}