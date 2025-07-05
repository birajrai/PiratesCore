package ovh.paulem.btm.versioned.playerconfig;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import ovh.paulem.btm.BetterMending;
import ovh.paulem.btm.versioned.Versioning;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public abstract class PlayerConfigHandler {
    protected static File dataFile;

    Map<UUID, Boolean> migrate() {
        Map<UUID, Boolean> toMigrate = new HashMap<>();

        if(dataFile.exists()) {
            BetterMending.getInstance().getLogger().info("Migrating " + dataFile.getName() + " to PDC...");

            YamlConfiguration data = YamlConfiguration.loadConfiguration(dataFile);

            Set<String> keys = data.getKeys(false);
            if(keys.isEmpty()) {
                dataFile.delete();
                return toMigrate;
            }

            for (String key : keys) {
                if (data.isBoolean(key)) {
                    boolean value = data.getBoolean(key);
                    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(UUID.fromString(key));

                    if(!offlinePlayer.hasPlayedBefore()) continue;

                    toMigrate.put(offlinePlayer.getUniqueId(), value);
                }
            }
        }

        return toMigrate;
    }

    public static PlayerConfigHandler of(BetterMending plugin) {
        dataFile = new File(plugin.getDataFolder(), "data.yml");
        return new PlayerConfigNewer();
    }

    public abstract @Nullable Boolean getPlayer(Player player);

    public boolean getPlayerOrDefault(Player player, boolean defaultValue) {
        Boolean got = getPlayer(player);
        return got != null ? got : defaultValue;
    }

    public boolean getPlayerOrCreate(Player player, boolean enabled) {
        Boolean present = getPlayer(player);

        if(present == null) return setPlayer(player, enabled);
        else return present;
    }

    public abstract boolean setPlayer(Player player, boolean enabled);

    public abstract void reload();
}
