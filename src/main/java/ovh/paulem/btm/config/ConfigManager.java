package ovh.paulem.btm.config;

import ovh.paulem.btm.BetterMending;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;

public class ConfigManager {
    private final BetterMending plugin;

    public ConfigManager(BetterMending plugin){
        this.plugin = plugin;
    }

    public void migrate(){
        FileConfiguration config = plugin.getConfig();

        int detectedVersion = config.getInt("version", 0);
        new ConfigUpdater(plugin).checkUpdate(detectedVersion);
    }
}
