package ovh.paulem.btm;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import ovh.paulem.btm.listeners.MendingUseListener;
import ovh.paulem.btm.versioned.damage.DamageHandler;
import ovh.paulem.btm.versioned.damage.DamageNewer;

public class BetterMending extends JavaPlugin {
    private static BetterMending instance;
    private DamageHandler damageHandler;

    @Override
    public void onEnable() {
        instance = this;

        String version = Bukkit.getVersion();
        if (!version.contains("1.21.7")) {
            getLogger().severe("This plugin only supports PaperMC 1.21.7. Detected version: " + version);
            setEnabled(false);
            return;
        }

        saveDefaultConfig();
        damageHandler = new DamageNewer();
        getServer().getPluginManager().registerEvents(new MendingUseListener(), this);
        getLogger().info("Enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Disabled! See you later!");
    }

    public static BetterMending getInstance() {
        return instance;
    }

    public static DamageHandler getDamageHandler() {
        return getInstance().damageHandler;
    }
}
