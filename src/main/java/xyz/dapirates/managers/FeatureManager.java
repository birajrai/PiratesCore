package xyz.dapirates.managers;

import org.bukkit.plugin.java.JavaPlugin;
import xyz.dapirates.features.BetterMending;
import xyz.dapirates.features.OreMiningNotifier;

import java.util.HashMap;
import java.util.Map;

public class FeatureManager {
    
    private final JavaPlugin plugin;
    private final Map<String, Object> features;
    
    public FeatureManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.features = new HashMap<>();
    }
    
    public void registerFeatures() {
        // Register existing features
        registerFeature("bettermending", new BetterMending());
        
        // Register ore mining feature
        OreMiningNotifier oreMiningNotifier = new OreMiningNotifier((xyz.dapirates.Core) plugin);
        registerFeature("oremining", oreMiningNotifier);
    }
    
    private void registerFeature(String name, Object feature) {
        features.put(name, feature);
        plugin.getServer().getPluginManager().registerEvents((org.bukkit.event.Listener) feature, plugin);
    }
    
    public <T> T getFeature(String name, Class<T> type) {
        Object feature = features.get(name);
        if (type.isInstance(feature)) {
            return type.cast(feature);
        }
        return null;
    }
    
    public OreMiningNotifier getOreMiningNotifier() {
        return getFeature("oremining", OreMiningNotifier.class);
    }
    
    public BetterMending getBetterMending() {
        return getFeature("bettermending", BetterMending.class);
    }
    
    public void unregisterAll() {
        features.clear();
    }
} 