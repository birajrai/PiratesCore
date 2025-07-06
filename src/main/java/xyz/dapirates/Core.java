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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

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
        // Flush all player stats and mining sessions to the database before shutdown
        if (oreMiningNotifier != null && databaseManager != null && databaseManager.isDatabaseAvailable()) {
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            // Flush all active mining sessions to stats
            oreMiningNotifier.flushAllMiningSessionsToStats();
            // Save stats for all players in memory
            for (UUID playerId : oreMiningNotifier.getAllPlayerIds()) {
                var stats = oreMiningNotifier.getPlayerStats(playerId);
                if (stats != null) {
                    futures.add(databaseManager.savePlayerStatsAsync(stats));
                }
            }
            // Also save stats for all online players (in case not in map)
            for (var player : getServer().getOnlinePlayers()) {
                var stats = oreMiningNotifier.getPlayerStats(player.getUniqueId());
                if (stats != null) {
                    futures.add(databaseManager.savePlayerStatsAsync(stats));
                }
            }
            // Wait for all saves to complete (with timeout)
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).orTimeout(5, java.util.concurrent.TimeUnit.SECONDS).exceptionally(e -> null).join();
        }
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

    // Add this method to expose all player IDs for flushing
    public List<UUID> getAllPlayerIds() {
        return new ArrayList<>(oreMiningNotifier.getAllPlayerIds());
    }
}