package ovh.paulem.btm.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import ovh.paulem.btm.BetterMending;
import ovh.paulem.btm.listeners.extendables.ManagersListener;
import ovh.paulem.btm.versioned.playerconfig.PlayerConfigNewer;

public class ConfigMigrationListener extends ManagersListener {
    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        if(BetterMending.getPlayerConfig() instanceof PlayerConfigNewer) {
            PlayerConfigNewer playerConfigNewer = (PlayerConfigNewer) BetterMending.getPlayerConfig();

            playerConfigNewer.migratePlayer(e.getPlayer());
        }
    }
}
