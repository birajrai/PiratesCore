package ovh.paulem.btm.versioned.playerconfig;

import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;
import ovh.paulem.btm.BetterMending;
import ovh.paulem.btm.listeners.ConfigMigrationListener;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerConfigNewer extends PlayerConfigHandler {
    private final NamespacedKey playerConfigKey = new NamespacedKey(BetterMending.getInstance(), "playerConfig");
    private static final PersistentDataType<Byte, Boolean> type = PersistentDataType.BOOLEAN;

    @Nullable
    private YamlConfiguration data = null;
    private final Map<UUID, Boolean> toMigrate;

    public PlayerConfigNewer() {
        this.toMigrate = new HashMap<>();
    }

    PlayerConfigNewer(Map<UUID, Boolean> toMigrate) {
        this.toMigrate = toMigrate;

        BetterMending.getInstance().getServer().getPluginManager().registerEvents(new ConfigMigrationListener(), BetterMending.getInstance());
    }

    public void migratePlayer(Player player) {
        if(toMigrate.isEmpty()) {
            return;
        }

        if(data == null) {
            data = YamlConfiguration.loadConfiguration(dataFile);
        }

        BetterMending.getInstance().getLogger().info(player.getUniqueId().toString());
        if(toMigrate.containsKey(player.getUniqueId())) {
            setPlayer(player, toMigrate.get(player.getUniqueId()));

            data.set(player.getUniqueId().toString(), null);
            try {
                data.save(dataFile);
            } catch (IOException e) {
                BetterMending.getInstance().getLogger().throwing(PlayerConfigNewer.class.getName(), "migratePlayer", e);
            }

            toMigrate.remove(player.getUniqueId());
        }

        if(toMigrate.isEmpty()) {
            PlayerConfigHandler.dataFile.delete();
            BetterMending.getInstance().getLogger().info("Migration complete!");
        }
    }

    @Override
    @Nullable
    public Boolean getPlayer(Player player) {
        return player.getPersistentDataContainer().get(playerConfigKey, type);
    }

    @Override
    public boolean setPlayer(Player player, boolean enabled) {
        player.getPersistentDataContainer().set(playerConfigKey, type, enabled);
        return enabled;
    }

    @Override
    public void reload() {}
}
