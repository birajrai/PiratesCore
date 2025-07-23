package xyz.dapirates.manager;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import net.Indyuce.bountyhunters.api.event.BountySetEvent;
import net.Indyuce.bountyhunters.api.event.BountyClaimEvent;

public class BountyHuntersListener implements Listener {
    private final PlayerStatsHandler statsHandler;

    public BountyHuntersListener(PlayerStatsHandler statsHandler) {
        this.statsHandler = statsHandler;
    }

    @EventHandler
    public void onBountySet(BountySetEvent event) {
        Player target = event.getTarget();
        if (target != null) {
            String playerName = target.getName();
            long bounty = event.getBounty();
            statsHandler.saveTopBountyAsync(playerName, bounty);
        }
    }

    @EventHandler
    public void onBountyClaim(BountyClaimEvent event) {
        Player hunter = event.getKiller();
        if (hunter != null) {
            String hunterName = hunter.getName();
            // Increment the hunter's top_hunters stat by 1
            statsHandler.loadStatAsync("top_hunters", hunterName).thenAccept(count -> {
                statsHandler.saveStatAsync("top_hunters", hunterName, count + 1);
            });
        }
    }
} 