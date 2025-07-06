package xyz.dapirates.manager;

import org.bukkit.plugin.java.JavaPlugin;
import xyz.dapirates.listener.BetterMending;
import xyz.dapirates.listener.OreMiningListener;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class FeatureManager {

    private final JavaPlugin plugin;
    private final Map<String, Object> features;

    public FeatureManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.features = new HashMap<>();
    }

    public void registerFeatures() {
        // Register features using a more abstract approach
        registerFeature("bettermending", BetterMending::new);

        // Register ore mining feature (already initialized in Core)
        OreMiningListener oreMiningListener = ((xyz.dapirates.core.Core) plugin).getOreMiningListener();
        registerFeature("oremining", oreMiningListener);
    }

    private void registerFeature(String name, Object feature) {
        features.put(name, feature);
        plugin.getServer().getPluginManager().registerEvents((org.bukkit.event.Listener) feature, plugin);
    }

    private void registerFeature(String name, Supplier<Object> featureSupplier) {
        Object feature = featureSupplier.get();
        registerFeature(name, feature);
    }

    public <T> T getFeature(String name, Class<T> type) {
        Object feature = features.get(name);
        if (type.isInstance(feature)) {
            return type.cast(feature);
        }
        return null;
    }

    public OreMiningListener getOreMiningListener() {
        return getFeature("oremining", OreMiningListener.class);
    }

    public BetterMending getBetterMending() {
        return getFeature("bettermending", BetterMending.class);
    }

    public void unregisterAll() {
        features.clear();
    }
}