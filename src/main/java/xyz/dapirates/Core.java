package xyz.dapirates;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.dapirates.features.BetterMending;

public class Core extends JavaPlugin {
    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(new BetterMending(), this);
        getLogger().info("Core plugin enabled, BetterMending registered!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Core plugin disabled!");
    }
} 