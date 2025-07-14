package xyz.dapirates.utils;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

/**
 * Utility class for loading, saving, and managing YAML configuration files for plugins.
 */
public class ConfigUtils {
    /**
     * Loads a YAML configuration file, saving the default resource if it does not exist.
     * @param plugin The plugin instance
     * @param fileName The file name (e.g., "Settings.yml")
     * @return The loaded FileConfiguration
     */
    public static FileConfiguration loadConfig(JavaPlugin plugin, String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            plugin.saveResource(fileName, false);
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    /**
     * Saves a FileConfiguration to disk.
     * @param config The configuration to save
     * @param file The file to save to
     * @param plugin The plugin instance (for logging)
     */
    public static void saveConfig(FileConfiguration config, File file, JavaPlugin plugin) {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save " + file.getName() + ": " + e.getMessage());
        }
    }
} 