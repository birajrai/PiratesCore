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

        PlayerConfigHandler playerConfigHandler;

        boolean fileBased = plugin.getConfig().getBoolean("file-based", false);
        boolean dataFileExists = dataFile.exists();
        boolean hasPDC = Versioning.hasPDC();

        if (fileBased) { // If the file-based option is enabled, use the file-based system
            playerConfigHandler = new PlayerConfigLegacy();
        } else if (dataFileExists && hasPDC) { // If the data file exists and the server supports PDC, migrate the data file to PDC
            playerConfigHandler = new PlayerConfigNewer(new PlayerConfigLegacy().migrate());
        } else if (hasPDC) { // If the server supports PDC, use the PDC system
            playerConfigHandler = new PlayerConfigNewer();
        } else { // If the server doesn't support PDC, use the file-based system
            playerConfigHandler = new PlayerConfigLegacy();
        }

        return playerConfigHandler;
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
