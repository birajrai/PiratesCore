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
import org.bukkit.entity.Boat;
import java.util.HashMap;

public class PlayerStatsListener implements Listener {
    private final Core plugin;
    private final PlayerStatsHandler statsHandler;

    // In-memory stat tracking
    private final HashMap<String, Integer> joinCounts = new HashMap<>();
    private final HashMap<String, Integer> boatCounts = new HashMap<>();

    public PlayerStatsListener(Core plugin) {
        this.plugin = plugin;
        this.statsHandler = plugin.getPlayerStatsHandler();
    }

    private boolean isStatsEnabled() {
        return plugin.getFeatureManager().isFeatureEnabled("Stats");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!isStatsEnabled()) return;
        Player player = event.getPlayer();
        String name = player.getName();
        String uuid = player.getUniqueId().toString();
        // Upsert player info
        statsHandler.upsertPlayerAsync(name, uuid);
        // Increment join count (in-memory, for demo)
        int join = joinCounts.getOrDefault(name, 0) + 1;
        joinCounts.put(name, join);
        // Save join count
        statsHandler.saveStatAsync("joined", name, join);
        // Save balance
        Economy econ = plugin.getEconomy();
        if (econ != null) {
            double balance = econ.getBalance(player);
            statsHandler.saveStatAsync("balance", name, (long) balance);
            updateBalanceTop(econ);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (!isStatsEnabled()) return;
        Player player = event.getPlayer();
        String name = player.getName();
        String uuid = player.getUniqueId().toString();
        // Upsert player info
        statsHandler.upsertPlayerAsync(name, uuid);
        // Save playtime (in seconds)
        long playtimeTicks = player.getStatistic(Statistic.PLAY_ONE_MINUTE);
        long playtimeSeconds = playtimeTicks / 20L;
        statsHandler.saveStatAsync("playtime", name, playtimeSeconds);
        // Save balance
        Economy econ = plugin.getEconomy();
        if (econ != null) {
            double balance = econ.getBalance(player);
            statsHandler.saveStatAsync("balance", name, (long) balance);
            updateBalanceTop(econ);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onVehicleEnter(VehicleEnterEvent event) {
        if (!isStatsEnabled()) return;
        if (event.getEntered() instanceof Player && event.getVehicle() instanceof Boat) {
            Player player = (Player) event.getEntered();
            String name = player.getName();
            // Increment boat mount count (in-memory, for demo)
            int boat = boatCounts.getOrDefault(name, 0) + 1;
            boatCounts.put(name, boat);
            statsHandler.saveStatAsync("boat_mount", name, boat);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!isStatsEnabled()) return;
        Player deceased = event.getEntity();
        Player killer = deceased.getKiller();
        if (killer != null && !killer.equals(deceased)) {
            String killerName = killer.getName();
            // Increment killer's kills
            statsHandler.loadStatAsync("kills", killerName).thenAccept(kills -> {
                statsHandler.saveStatAsync("kills", killerName, kills + 1);
            });
        }
        // Increment deceased's deaths
        String deceasedName = deceased.getName();
        statsHandler.loadStatAsync("deaths", deceasedName).thenAccept(deaths -> {
            statsHandler.saveStatAsync("deaths", deceasedName, deaths + 1);
        });
    }

    // Add more event handlers for other stats as needed (blocks broken, mobs killed, etc.)

    // Update the balancetop leaderboard in the database for the top 10 players
    private void updateBalanceTop(Economy econ) {
        List<Player> onlinePlayers = Bukkit.getOnlinePlayers().stream().collect(Collectors.toList());
        onlinePlayers.sort(Comparator.comparingDouble(p -> econ.getBalance((OfflinePlayer)p)).reversed());
        int topN = Math.min(10, onlinePlayers.size());
        List<Player> topPlayers = onlinePlayers.subList(0, topN);
        // Save top balances
        for (Player p : topPlayers) {
            statsHandler.saveStatAsync("balance", p.getName(), (long) econ.getBalance(p));
        }
    }
} 