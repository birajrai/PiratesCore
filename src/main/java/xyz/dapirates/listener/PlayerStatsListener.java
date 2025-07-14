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
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.entity.Boat;
import java.util.HashMap;

public class PlayerStatsListener implements Listener {
    private final Core plugin;
    private final PlayerStatsHandler statsHandler;

    // In-memory stat tracking
    private final HashMap<String, Integer> joinCounts = new HashMap<>();
    private final HashMap<String, Integer> boatCounts = new HashMap<>();
    private final HashMap<String, Integer> leaveBoatCounts = new HashMap<>();

    public PlayerStatsListener(Core plugin) {
        this.plugin = plugin;
        this.statsHandler = plugin.getPlayerStatsHandler();
    }

    // Save balance and balancetop to MySQL on player join
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String name = player.getName();
        // Increment join count
        int join = joinCounts.getOrDefault(name, 0) + 1;
        joinCounts.put(name, join);
        statsHandler.saveJoinCountAsync(name, join);
        Economy econ = plugin.getEconomy();
        if (econ != null) {
            double balance = econ.getBalance(player);
            statsHandler.saveBalanceAsync(name, balance);
            updateBalanceTop(econ);
        }
    }

    // Save playtime, balance, and leave count to MySQL on player quit
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        String name = player.getName();
        // Playtime in ticks; convert to seconds
        long playtimeTicks = player.getStatistic(Statistic.PLAY_ONE_MINUTE);
        long playtimeSeconds = playtimeTicks / 20L;
        statsHandler.savePlaytimeAsync(name, playtimeSeconds);
        // Save balance
        Economy econ = plugin.getEconomy();
        if (econ != null) {
            double balance = econ.getBalance(player);
            statsHandler.saveBalanceAsync(name, balance);
            updateBalanceTop(econ);
        }
    }

    // Track boat mount
    @EventHandler(priority = EventPriority.MONITOR)
    public void onVehicleEnter(VehicleEnterEvent event) {
        if (event.getEntered() instanceof Player && event.getVehicle() instanceof Boat) {
            Player player = (Player) event.getEntered();
            String name = player.getName();
            int boat = boatCounts.getOrDefault(name, 0) + 1;
            boatCounts.put(name, boat);
            statsHandler.saveBoatCountAsync(name, boat);
        }
    }

    // Track boat leave
    @EventHandler(priority = EventPriority.MONITOR)
    public void onVehicleExit(VehicleExitEvent event) {
        if (event.getExited() instanceof Player && event.getVehicle() instanceof Boat) {
            Player player = (Player) event.getExited();
            String name = player.getName();
            int leaveBoat = leaveBoatCounts.getOrDefault(name, 0) + 1;
            leaveBoatCounts.put(name, leaveBoat);
            statsHandler.saveLeaveBoatCountAsync(name, leaveBoat);
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

    // Update all top leaderboards (join, leave, boat, leaveboat)
    private void updateAllTops(List<Player> topPlayers) {
        statsHandler.updateGenericTopLeaderboard("player_join_top", topPlayers, joinCounts);
        statsHandler.updateGenericTopLeaderboard("player_boat_top", topPlayers, boatCounts);
        statsHandler.updateGenericTopLeaderboard("player_leave_boat_top", topPlayers, leaveBoatCounts);
    }

    // Update the balancetop leaderboard in the database for the top 10 players
    private void updateBalanceTop(Economy econ) {
        List<Player> onlinePlayers = Bukkit.getOnlinePlayers().stream().collect(Collectors.toList());
        onlinePlayers.sort(Comparator.comparingDouble(p -> econ.getBalance((OfflinePlayer)p)).reversed());
        int topN = Math.min(10, onlinePlayers.size());
        List<Player> topPlayers = onlinePlayers.subList(0, topN);
        statsHandler.updateTopBalanceLeaderboard(topPlayers, econ);
        updateAllTops(topPlayers);
    }
} 