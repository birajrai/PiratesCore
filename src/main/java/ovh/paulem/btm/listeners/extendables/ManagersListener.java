package ovh.paulem.btm.listeners.extendables;

import ovh.paulem.btm.BetterMending;
import ovh.paulem.btm.config.ConfigBlacklist;
import ovh.paulem.btm.versioned.damage.DamageHandler;
import ovh.paulem.btm.managers.CooldownManager;
import ovh.paulem.btm.managers.RepairManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.Listener;
import ovh.paulem.btm.versioned.playerconfig.PlayerConfigHandler;

import java.util.ArrayList;
import java.util.List;

public class ManagersListener implements Listener {
    private static final List<ManagersListener> MANAGERS_LISTENERS = new ArrayList<>();

    protected final PlayerConfigHandler playerConfig;
    protected final RepairManager repairManager;
    protected final DamageHandler damageHandler;
    protected final ConfigBlacklist configBlacklist;

    protected CooldownManager cooldownManager;

    public ManagersListener() {
        this.playerConfig = BetterMending.getPlayerConfig();
        this.repairManager = BetterMending.getRepairManager();
        this.damageHandler = BetterMending.getDamageHandler();
        this.configBlacklist = BetterMending.getConfigBlacklist();

        this.cooldownManager = new CooldownManager(BetterMending.getInstance().getConfig().getInt("cooldown.time", 0));

        MANAGERS_LISTENERS.add(this);
    }

    public static void reloadConfig(FileConfiguration config) {
        MANAGERS_LISTENERS.forEach(listener ->
                listener.cooldownManager = new CooldownManager(config.getInt("cooldown.time", 0))
        );
    }
}
