package ovh.paulem.btm.versioned.playerconfig;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import ovh.paulem.btm.BetterMending;

import java.io.IOException;

public class PlayerConfigLegacy extends PlayerConfigHandler {
    protected final YamlConfiguration data;

    PlayerConfigLegacy() {
        this.data = YamlConfiguration.loadConfiguration(dataFile);
    }

    @Override
    @Nullable
    public Boolean getPlayer(Player player) {
        return (Boolean) this.data.get(player.getUniqueId().toString());
    }

    @Override
    public boolean setPlayer(Player player, boolean enabled) {
        this.data.set(player.getUniqueId().toString(), enabled);

        try {
            this.data.save(dataFile);
        } catch (IOException e) {
            BetterMending.getInstance().getLogger().throwing(PlayerConfigLegacy.class.getName(), "setPlayer", e);
        }

        return enabled;
    }

    @Override
    public void reload() {
        try {
            this.data.load(dataFile);
        } catch (Exception e) {
            BetterMending.getInstance().getLogger().throwing(PlayerConfigLegacy.class.getName(), "reload", e);
        }
    }
}
