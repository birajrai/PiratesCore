package ovh.paulem.btm;

import org.bukkit.Bukkit;
import ovh.paulem.btm.compat.BTMPlaceholderCompat;
import ovh.paulem.btm.commands.CommandBTM;
import ovh.paulem.btm.config.ConfigBlacklist;
import ovh.paulem.btm.listeners.MendingUseListener;
import ovh.paulem.btm.listeners.PreventDestroyListener;
import ovh.paulem.btm.config.ConfigManager;
import ovh.paulem.btm.versioned.damage.DamageHandler;
import ovh.paulem.btm.versioned.damage.DamageLegacy;
import ovh.paulem.btm.managers.RepairManager;
import ovh.paulem.btm.versioned.damage.DamageNewer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import ovh.paulem.btm.utils.PluginUtils;
import ovh.paulem.btm.versioned.Versioning;
import ovh.paulem.btm.versioned.playerconfig.PlayerConfigHandler;
import ovh.paulem.btm.versioned.playerconfig.PlayerConfigLegacy;

public class BetterMending extends JavaPlugin {
    private static BetterMending instance;

    private PlayerConfigHandler playerConfig;
    private RepairManager repairManager;
    private DamageHandler damageHandler;
    private ConfigBlacklist configBlacklist;

    @Override
    public void onEnable() {
        instance = this;

        if(!Versioning.isPost9()) {
            getLogger().severe("You need to use a 1.9+ server! Mending isn't present in older versions!");
            setEnabled(false);
            return;
        }

        saveDefaultConfig();
        new ConfigManager(this).migrate();

        FileConfiguration config = getConfig();

        playerConfig = PlayerConfigHandler.of(this);
        damageHandler = Versioning.isPost17() ? new DamageNewer() : new DamageLegacy();
        repairManager = new RepairManager(this);
        configBlacklist = new ConfigBlacklist();

        getServer().getPluginManager().registerEvents(new MendingUseListener(), this);
        getServer().getPluginManager().registerEvents(new PreventDestroyListener(), this);

        final CommandBTM commandBTM = new CommandBTM(this, config.getInt("version", 0), playerConfig);
        getCommand("btm").setExecutor(commandBTM);
        getCommand("btm").setTabCompleter(commandBTM);

        if(Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null){
            boolean registered = new BTMPlaceholderCompat(this).register();
            if(!registered) {
                getLogger().warning("Unable to pass plugin to PlaceholderAPI! Better Mending placeholders may not work!");
            }
        }

        if(config.getBoolean("auto-repair", false)) {
            repairManager.initAutoRepair();
        }

        if(config.getBoolean("bstat", true)){
            // Metrics removed
        }

        PluginUtils.reloadConfig();

        getLogger().info("Enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Disabled! See you later!");
    }

    public static BetterMending getInstance() {
        return instance;
    }

    public static FileConfiguration getConf() {
        return getInstance().getConfig();
    }

    public static ConfigBlacklist getConfigBlacklist() {
        return getInstance().configBlacklist;
    }

    public static DamageHandler getDamageHandler() {
        return getInstance().damageHandler;
    }

    public static PlayerConfigHandler getPlayerConfig() {
        return getInstance().playerConfig;
    }

    public static RepairManager getRepairManager() {
        return getInstance().repairManager;
    }
}
