package xyz.dapirates.listener;

import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import xyz.dapirates.core.Core;
import xyz.dapirates.manager.PlayerStatsHandler;
import net.milkbowl.vault.economy.Economy;

public class PlayerStatsListener implements Listener {
    private final Core plugin;
    private final PlayerStatsHandler statsHandler;

    public PlayerStatsListener(Core plugin) {
        this.plugin = plugin;
        this.statsHandler = plugin.getPlayerStatsHandler();
    }

    // Save playtime and balance to MySQL on player quit
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        // Playtime in ticks; convert to seconds
        long playtimeTicks = player.getStatistic(Statistic.PLAY_ONE_MINUTE);
        long playtimeSeconds = playtimeTicks / 20L;
        statsHandler.savePlaytimeAsync(player.getName(), playtimeSeconds);
        // Save balance
        Economy econ = plugin.getEconomy();
        if (econ != null) {
            double balance = econ.getBalance(player);
            statsHandler.saveBalanceAsync(player.getName(), balance);
        }
    }

    // Save balance and balancetop to MySQL on player join
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Economy econ = plugin.getEconomy();
        if (econ != null) {
            double balance = econ.getBalance(player);
            statsHandler.saveBalanceAsync(player.getName(), balance);
            // For demo, use balance as topbalance (real balancetop would require leaderboard logic)
            statsHandler.saveTopBalanceAsync(player.getName(), balance);
        }
    }

    // Save kills/deaths to MySQL on player death
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player deceased = event.getEntity();
        Player killer = deceased.getKiller();
        if (killer != null && !killer.equals(deceased)) {
            // Increment killer's kills (for demo, just increment by 1)
            statsHandler.loadKillsAsync(killer.getName()).thenAccept(kills -> {
                statsHandler.saveKillsAsync(killer.getName(), kills + 1);
            });
        }
        // Increment deceased's deaths
        statsHandler.loadDeathsAsync(deceased.getName()).thenAccept(deaths -> {
            statsHandler.saveDeathsAsync(deceased.getName(), deaths + 1);
        });
    }

    // Stub: Save CMI balance (call this from your CMI integration)
    public void saveBalance(Player player, double balance) {
        statsHandler.saveBalanceAsync(player.getName(), balance);
    }

    // Stub: Save CMI topbalance (call this from your CMI integration)
    public void saveTopBalance(Player player, double topbalance) {
        statsHandler.saveTopBalanceAsync(player.getName(), topbalance);
    }
} 