package xyz.dapirates;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.dapirates.features.BetterMending;
import xyz.dapirates.features.OreMiningNotifier;
import xyz.dapirates.command.ShowCommand;
import xyz.dapirates.command.OreMiningCommand;

public class Core extends JavaPlugin {
    
    private OreMiningNotifier oreMiningNotifier;
    
    @Override
    public void onEnable() {
        // Register existing features
        Bukkit.getPluginManager().registerEvents(new BetterMending(), this);
        this.getCommand("show").setExecutor(new ShowCommand());
        
        // Initialize and register ore mining feature
        oreMiningNotifier = new OreMiningNotifier(this);
        Bukkit.getPluginManager().registerEvents(oreMiningNotifier, this);
        
        // Register ore mining command
        OreMiningCommand oreMiningCommand = new OreMiningCommand(this, oreMiningNotifier);
        this.getCommand("oremining").setExecutor(oreMiningCommand);
        this.getCommand("oremining").setTabCompleter(oreMiningCommand);
        
        getLogger().info("Core plugin enabled, BetterMending and OreMining registered!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Core plugin disabled!");
    }
    
    public OreMiningNotifier getOreMiningNotifier() {
        return oreMiningNotifier;
    }
}