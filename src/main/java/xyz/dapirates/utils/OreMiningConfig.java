package xyz.dapirates.utils;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import xyz.dapirates.core.Core;
import xyz.dapirates.utils.ConfigUtils;
import xyz.dapirates.manager.MessageManager;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Loads and manages the OreMining.yml configuration, including tracked blocks, alert thresholds, and message formats.
 */
public class OreMiningConfig {

    private final Core plugin;
    private final File configFile;
    private FileConfiguration config;
    private final MessageManager messageManager;

    /**
     * Constructs an OreMiningConfig for the plugin and loads the config file.
     * @param plugin The main plugin instance
     */
    public OreMiningConfig(Core plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "OreMining.yml");
        this.messageManager = plugin.getMessageManager();
        loadConfig();
    }

    private void loadConfig() {
        config = ConfigUtils.loadConfig(plugin, "OreMining.yml");
        setupDefaults();
    }

    private void setupDefaults() {
        // General settings
        config.addDefault("general.enabled", true);
        config.addDefault("general.console-notifications", true);
        config.addDefault("general.self-notifications", true);
        config.addDefault("general.ignore-enabled", false);
        config.addDefault("general.tnt-mining-enabled", true);
        config.addDefault("general.message-format", "text"); // "text" or "embed"

        // Height and light controls
        config.addDefault("controls.min-height", -64);
        config.addDefault("controls.max-height", 320);
        config.addDefault("controls.min-light-level", 0);
        config.addDefault("controls.max-light-level", 15);

        // Time-based alerts
        config.addDefault("alerts.time-based.enabled", true);
        config.addDefault("alerts.time-based.timeframe-minutes", 5);
        config.addDefault("alerts.time-based.threshold", 10);
        config.addDefault("alerts.time-based.message",
                "§c[OreMining] §f{player} has mined {count} blocks in the last {timeframe} minutes!");

        // Ignore list
        config.addDefault("ignore.players", Arrays.asList());

        // Default tracked blocks
        setupDefaultBlocks();

        config.options().copyDefaults(true);
        saveConfig();
    }

    private void setupDefaultBlocks() {
        // Common ores
        setupBlockDefaults(Material.DIAMOND_ORE, "§b[OreMining] §f{player} found §bDiamond Ore§f!",
                Sound.ENTITY_PLAYER_LEVELUP);
        setupBlockDefaults(Material.DEEPSLATE_DIAMOND_ORE, "§b[OreMining] §f{player} found §bDeepslate Diamond Ore§f!",
                Sound.ENTITY_PLAYER_LEVELUP);
        setupBlockDefaults(Material.EMERALD_ORE, "§a[OreMining] §f{player} found §aEmerald Ore§f!",
                Sound.ENTITY_PLAYER_LEVELUP);
        setupBlockDefaults(Material.DEEPSLATE_EMERALD_ORE, "§a[OreMining] §f{player} found §aDeepslate Emerald Ore§f!",
                Sound.ENTITY_PLAYER_LEVELUP);
        setupBlockDefaults(Material.GOLD_ORE, "§6[OreMining] §f{player} found §6Gold Ore§f!",
                Sound.BLOCK_NOTE_BLOCK_PLING);
        setupBlockDefaults(Material.DEEPSLATE_GOLD_ORE, "§6[OreMining] §f{player} found §6Deepslate Gold Ore§f!",
                Sound.BLOCK_NOTE_BLOCK_PLING);
        setupBlockDefaults(Material.IRON_ORE, "§7[OreMining] §f{player} found §7Iron Ore§f!",
                Sound.BLOCK_NOTE_BLOCK_PLING);
        setupBlockDefaults(Material.DEEPSLATE_IRON_ORE, "§7[OreMining] §f{player} found §7Deepslate Iron Ore§f!",
                Sound.BLOCK_NOTE_BLOCK_PLING);
        setupBlockDefaults(Material.COPPER_ORE, "§c[OreMining] §f{player} found §cCopper Ore§f!",
                Sound.BLOCK_NOTE_BLOCK_PLING);
        setupBlockDefaults(Material.DEEPSLATE_COPPER_ORE, "§c[OreMining] §f{player} found §cDeepslate Copper Ore§f!",
                Sound.BLOCK_NOTE_BLOCK_PLING);
        setupBlockDefaults(Material.COAL_ORE, "§8[OreMining] §f{player} found §8Coal Ore§f!",
                Sound.BLOCK_NOTE_BLOCK_PLING);
        setupBlockDefaults(Material.DEEPSLATE_COAL_ORE, "§8[OreMining] §f{player} found §8Deepslate Coal Ore§f!",
                Sound.BLOCK_NOTE_BLOCK_PLING);
        setupBlockDefaults(Material.REDSTONE_ORE, "§4[OreMining] §f{player} found §4Redstone Ore§f!",
                Sound.BLOCK_NOTE_BLOCK_PLING);
        setupBlockDefaults(Material.DEEPSLATE_REDSTONE_ORE,
                "§4[OreMining] §f{player} found §4Deepslate Redstone Ore§f!", Sound.BLOCK_NOTE_BLOCK_PLING);
        setupBlockDefaults(Material.LAPIS_ORE, "§9[OreMining] §f{player} found §9Lapis Ore§f!",
                Sound.BLOCK_NOTE_BLOCK_PLING);
        setupBlockDefaults(Material.DEEPSLATE_LAPIS_ORE, "§9[OreMining] §f{player} found §9Deepslate Lapis Ore§f!",
                Sound.BLOCK_NOTE_BLOCK_PLING);
        setupBlockDefaults(Material.ANCIENT_DEBRIS, "§5[OreMining] §f{player} found §5Ancient Debris§f!",
                Sound.ENTITY_ENDER_DRAGON_GROWL);
        setupBlockDefaults(Material.NETHER_GOLD_ORE, "§6[OreMining] §f{player} found §6Nether Gold Ore§f!",
                Sound.BLOCK_NOTE_BLOCK_PLING);
        setupBlockDefaults(Material.NETHER_QUARTZ_ORE, "§f[OreMining] §f{player} found §fNether Quartz Ore§f!",
                Sound.BLOCK_NOTE_BLOCK_PLING);
    }

    private void setupBlockDefaults(Material material, String message, Sound sound) {
        String path = "blocks." + material.getKey().getKey().toLowerCase();
        config.addDefault(path + ".enabled", true);
        config.addDefault(path + ".message", message);
        config.addDefault(path + ".sound", sound.toString());
        config.addDefault(path + ".show-coordinates", true);
        config.addDefault(path + ".commands", Arrays.asList());
    }

    public void saveConfig() {
        ConfigUtils.saveConfig(config, configFile, plugin);
    }

    /**
     * Reloads the OreMining.yml configuration file.
     */
    public void reloadConfig() {
        loadConfig();
    }

    /**
     * Checks if the ore mining system is enabled.
     */
    public boolean isEnabled() {
        return config.getBoolean("general.enabled", true);
    }

    public boolean isConsoleNotificationsEnabled() {
        return config.getBoolean("general.console-notifications", true);
    }

    public boolean isSelfNotificationsEnabled() {
        return config.getBoolean("general.self-notifications", true);
    }

    public boolean isIgnoreEnabled() {
        return config.getBoolean("general.ignore-enabled", false);
    }

    public boolean isTNTMiningEnabled() {
        return config.getBoolean("general.tnt-mining-enabled", true);
    }

    public String getMessageFormat() {
        return config.getString("general.message-format", "text");
    }

    // Delayed notification settings
    public boolean isDelayedNotificationsEnabled() {
        return config.getBoolean("general.delayed-notifications.enabled", true);
    }

    public int getDelayedNotificationDelay() {
        return config.getInt("general.delayed-notifications.delay-seconds", 30);
    }

    public boolean isBatchMessagesEnabled() {
        return config.getBoolean("general.delayed-notifications.batch-messages", true);
    }

    // Height and light controls
    public boolean isHeightAllowed(int y) {
        int minHeight = config.getInt("controls.min-height", -64);
        int maxHeight = config.getInt("controls.max-height", 320);
        return y >= minHeight && y <= maxHeight;
    }

    public boolean isLightLevelAllowed(int lightLevel) {
        int minLight = config.getInt("controls.min-light-level", 0);
        int maxLight = config.getInt("controls.max-light-level", 15);
        return lightLevel >= minLight && lightLevel <= maxLight;
    }

    // Time-based alerts
    public boolean isTimeBasedAlertsEnabled() {
        return config.getBoolean("alerts.time-based.enabled", true);
    }

    public long getTimeBasedAlertTimeframe() {
        return config.getLong("alerts.time-based.timeframe-minutes", 5) * 60 * 1000; // Convert to milliseconds
    }

    public int getTimeBasedAlertThreshold() {
        return config.getInt("alerts.time-based.threshold", 10);
    }

    public String getTimeBasedAlertMessage(Player player, int count) {
        String message = config.getString("alerts.time-based.message",
                "§c[OreMining] §f{player} has mined {count} blocks in the last {timeframe} minutes!");

        long timeframeMinutes = getTimeBasedAlertTimeframe() / (60 * 1000);
        message = message
                .replace("{player}", player.getName())
                .replace("{count}", String.valueOf(count))
                .replace("{timeframe}", String.valueOf(timeframeMinutes));
        // Use MessageManager for any further placeholder processing
        return messageManager.processPlaceholders(player, message, null, player.getLocation(), false);
    }

    // Ignore list
    public List<String> getIgnoredPlayers() {
        return config.getStringList("ignore.players");
    }

    public void addIgnoredPlayer(String playerName) {
        List<String> ignore = getIgnoredPlayers();
        if (!ignore.contains(playerName)) {
            ignore.add(playerName);
            config.set("ignore.players", ignore);
            saveConfig();
        }
    }

    public void removeIgnoredPlayer(String playerName) {
        List<String> ignore = getIgnoredPlayers();
        ignore.remove(playerName);
        config.set("ignore.players", ignore);
        saveConfig();
    }

    // Block tracking
    public boolean isBlockTracked(Material material) {
        String path = "blocks." + material.getKey().getKey().toLowerCase();
        return config.getBoolean(path + ".enabled", false);
    }

    /**
     * Returns the custom message for a block, with placeholders replaced.
     */
    public String getCustomMessage(Material material, Player player, org.bukkit.Location location, boolean isTNT) {
        String path = "blocks." + material.getKey().getKey().toLowerCase();
        String message = config.getString(path + ".message", "§a[OreMining] §f{player} found {block}!");

        boolean showCoords = config.getBoolean(path + ".show-coordinates", true);

        message = message
                .replace("{player}", player.getName())
                .replace("{block}", material.getKey().getKey().replace("_", " "))
                .replace("{world}", location.getWorld().getName());

        if (showCoords) {
            message = message
                    .replace("{x}", String.valueOf(location.getBlockX()))
                    .replace("{y}", String.valueOf(location.getBlockY()))
                    .replace("{z}", String.valueOf(location.getBlockZ()));
        } else {
            message = message
                    .replace("{x}", "***")
                    .replace("{y}", "***")
                    .replace("{z}", "***");
        }

        if (isTNT) {
            message = message.replace("found", "found (TNT)");
        }
        // Use MessageManager for any further placeholder processing
        return messageManager.processPlaceholders(player, message, material, location, isTNT);
    }

    /**
     * Returns the custom sound for a block.
     */
    public Sound getCustomSound(Material material) {
        String path = "blocks." + material.getKey().getKey().toLowerCase();
        String soundName = config.getString(path + ".sound", Sound.BLOCK_NOTE_BLOCK_PLING.toString());
        try {
            return Sound.valueOf(soundName);
        } catch (IllegalArgumentException e) {
            return Sound.BLOCK_NOTE_BLOCK_PLING;
        }
    }

    /**
     * Returns the custom commands for a block.
     */
    public List<String> getCustomCommands(Material material) {
        String path = "blocks." + material.getKey().getKey().toLowerCase();
        return config.getStringList(path + ".commands");
    }

    /**
     * Returns true if coordinates should be shown for a block.
     */
    public boolean isShowCoordinates(Material material) {
        String path = "blocks." + material.getKey().getKey().toLowerCase();
        return config.getBoolean(path + ".show-coordinates", true);
    }

    // Block management
    /**
     * Adds a block to the tracked blocks list with custom settings.
     */
    public void addTrackedBlock(Material material, String message, Sound sound, boolean showCoords) {
        String path = "blocks." + material.getKey().getKey().toLowerCase();
        config.set(path + ".enabled", true);
        config.set(path + ".message", message);
        config.set(path + ".sound", sound.toString());
        config.set(path + ".show-coordinates", showCoords);
        config.set(path + ".commands", Arrays.asList());
        saveConfig();
    }

    /**
     * Removes a block from the tracked blocks list.
     */
    public void removeTrackedBlock(Material material) {
        String path = "blocks." + material.getKey().getKey().toLowerCase();
        config.set(path, null);
        saveConfig();
    }

    /**
     * Returns the set of all tracked blocks.
     */
    public Set<Material> getTrackedBlocks() {
        ConfigurationSection blocksSection = config.getConfigurationSection("blocks");
        if (blocksSection == null) {
            return new HashSet<>();
        }

        return blocksSection.getKeys(false).stream()
                .filter(key -> config.getBoolean("blocks." + key + ".enabled", false))
                .map(Material::valueOf)
                .collect(Collectors.toSet());
    }
}