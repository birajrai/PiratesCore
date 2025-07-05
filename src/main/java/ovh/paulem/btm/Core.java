package ovh.paulem.btm;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import ovh.paulem.btm.features.BetterMending;

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