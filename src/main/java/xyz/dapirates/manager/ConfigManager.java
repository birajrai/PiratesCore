package xyz.dapirates.manager;

import xyz.dapirates.core.Core;
import xyz.dapirates.utils.OreMiningConfig;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;

/**
 * Central manager for reloading all plugin configurations and listeners.
 */
public class ConfigManager {
    private final Core plugin;
    private final OreMiningConfig oreMiningConfig;
    private final WebhookManager webhookManager;
    private YamlConfiguration databaseConfig;
    private String dbHost;
    private String dbDatabase;
    private String dbUser;
    private String dbPassword;

    public ConfigManager(Core plugin, OreMiningConfig oreMiningConfig, WebhookManager webhookManager) {
        this.plugin = plugin;
        this.oreMiningConfig = oreMiningConfig;
        this.webhookManager = webhookManager;
    }

    public void loadDatabaseConfig() {
        try {
            File dbFile = new File(plugin.getDataFolder(), "Database.yml");
            if (!dbFile.exists()) {
                plugin.saveResource("Database.yml", false);
            }
            databaseConfig = YamlConfiguration.loadConfiguration(dbFile);
            dbHost = databaseConfig.getString("mysql.host", "localhost");
            dbDatabase = databaseConfig.getString("mysql.database", "piratescore");
            dbUser = databaseConfig.getString("mysql.user", "root");
            dbPassword = databaseConfig.getString("mysql.password", "password");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load Database.yml: " + e.getMessage());
            dbHost = "localhost";
            dbDatabase = "piratescore";
            dbUser = "root";
            dbPassword = "";
        }
    }

    /**
     * Reloads all configs and listeners in the plugin.
     */
    public void reloadAll() {
        oreMiningConfig.reloadConfig();
        webhookManager.reloadConfig();
        plugin.getFeatureManager().getChatFilterListener().reload();
        plugin.getFeatureManager().reloadSettings();
        plugin.getLogger().info("All configs reloaded.");
    }

    // Add methods to get MySQL config from Database.yml
    public String getDatabaseUrl() {
        if (dbHost == null || dbDatabase == null) loadDatabaseConfig();
        return "jdbc:mysql://" + dbHost + ":3306/" + dbDatabase + "?useSSL=false&autoReconnect=true";
    }
    public String getDatabaseUser() {
        if (dbUser == null) loadDatabaseConfig();
        return dbUser;
    }
    public String getDatabasePassword() {
        if (dbPassword == null) loadDatabaseConfig();
        return dbPassword;
    }
}