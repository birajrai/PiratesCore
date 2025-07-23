package xyz.dapirates.manager;

import xyz.dapirates.core.Core;
import xyz.dapirates.manager.ConfigManager;
import java.sql.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Handles player stats and leaderboards in MySQL using a unified table structure for each stat.
 * All operations are async to prevent server lag.
 */
public class PlayerStatsHandler {
    private final Core plugin;
    private String mysqlUrl;
    private String mysqlUser;
    private String mysqlPassword;
    private boolean databaseAvailable;
    private final java.util.logging.Logger logger;

    // List of all stat table names (without 'stat_' prefix)
    private static final List<String> STAT_NAMES = Arrays.asList(
        "kills", "deaths", "blocks_broken", "mob_kills", "joined", "playtime", "boat_mount",
        "mob_deaths", "balance", "fishing", "blocks_placed", "damage_dealt", "distance_swim",
        "fish_caught", "items_enchanted", "villager_trades", "music_disc_played", "distance_boat",
        "damage_taken", "top_bounties"
    );

    public PlayerStatsHandler(Core plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.mysqlUrl = configManager.getDatabaseUrl();
        this.mysqlUser = configManager.getDatabaseUser();
        this.mysqlPassword = configManager.getDatabasePassword();
        boolean dbAvailable = false;
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            logger.info("Connecting to MySQL: " + mysqlUrl + " as user " + mysqlUser);
            initializeTables();
            dbAvailable = true;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to initialize MySQL: " + e.getMessage(), e);
        }
        this.databaseAvailable = dbAvailable;
    }

    private void initializeTables() {
        try (Connection conn = getConnection()) {
            // Create players table
            String playersTable = """
                CREATE TABLE IF NOT EXISTS players (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    username VARCHAR(32) UNIQUE,
                    uuid VARCHAR(36),
                    first_join TIMESTAMP,
                    last_seen TIMESTAMP
                )""";
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(playersTable);
            }
            // Create stat tables
            for (String stat : STAT_NAMES) {
                String statTable = "CREATE TABLE IF NOT EXISTS stat_" + stat + " (" +
                        "id INT PRIMARY KEY AUTO_INCREMENT, " +
                        "username VARCHAR(32), " +
                        "value BIGINT, " +
                        "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP)";
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute(statTable);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create player stats tables", e);
        }
    }

    private Connection getConnection() throws SQLException {
        logger.info("Opening MySQL connection: " + mysqlUrl + " as user " + mysqlUser);
        Connection conn = DriverManager.getConnection(mysqlUrl, mysqlUser, mysqlPassword);
        logger.info("Connection opened.");
        return conn;
    }

    // --- PLAYERS TABLE ---
    public CompletableFuture<Void> upsertPlayerAsync(String username, String uuid) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT INTO players (username, uuid, first_join, last_seen) VALUES (?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) " +
                    "ON DUPLICATE KEY UPDATE last_seen = CURRENT_TIMESTAMP, uuid = VALUES(uuid)";
            try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username);
                ps.setString(2, uuid);
                ps.executeUpdate();
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Failed to upsert player " + username, e);
            }
        });
    }

    // --- GENERIC STAT TABLES ---
    public CompletableFuture<Void> saveStatAsync(String stat, String username, long value) {
        return CompletableFuture.runAsync(() -> {
            String table = "stat_" + stat;
            String sql = "INSERT INTO " + table + " (username, value, updated_at) VALUES (?, ?, CURRENT_TIMESTAMP) " +
                    "ON DUPLICATE KEY UPDATE value = VALUES(value), updated_at = CURRENT_TIMESTAMP";
            try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username);
                ps.setLong(2, value);
                ps.executeUpdate();
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Failed to save stat " + stat + " for " + username, e);
            }
        });
    }

    public CompletableFuture<Long> loadStatAsync(String stat, String username) {
        return CompletableFuture.supplyAsync(() -> {
            String table = "stat_" + stat;
            String sql = "SELECT value FROM " + table + " WHERE username = ?";
            try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getLong("value");
                }
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Failed to load stat " + stat + " for " + username, e);
            }
            return 0L;
        });
    }

    public CompletableFuture<List<StatEntry>> getTopStatAsync(String stat, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            String table = "stat_" + stat;
            String sql = "SELECT username, value FROM " + table + " ORDER BY value DESC, updated_at ASC LIMIT ?";
            List<StatEntry> result = new ArrayList<>();
            try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, limit);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        result.add(new StatEntry(rs.getString("username"), rs.getLong("value")));
                    }
                }
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Failed to fetch top stat for " + stat, e);
            }
            return result;
        });
    }

    // --- BOUNTYHUNTERS INTEGRATION ---
    // Save top bounties (call this from BountyHunters integration)
    public CompletableFuture<Void> saveTopBountyAsync(String username, long value) {
        return saveStatAsync("top_bounties", username, value);
    }
    public CompletableFuture<List<StatEntry>> getTopBountiesAsync(int limit) {
        return getTopStatAsync("top_bounties", limit);
    }

    public boolean isDatabaseAvailable() {
        return databaseAvailable;
    }

    // --- StatEntry class for leaderboard results ---
    public static class StatEntry {
        public final String username;
        public final long value;
        public StatEntry(String username, long value) {
            this.username = username;
            this.value = value;
        }
    }

    // Optionally, add a method to reload config if needed
    public void reloadConfig(ConfigManager configManager) {
        logger.info("Reloading MySQL config from ConfigManager...");
        this.mysqlUrl = configManager.getDatabaseUrl();
        this.mysqlUser = configManager.getDatabaseUser();
        this.mysqlPassword = configManager.getDatabasePassword();
        boolean dbAvailable = false;
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            logger.info("Connecting to MySQL: " + mysqlUrl + " as user " + mysqlUser);
            initializeTables();
            dbAvailable = true;
            logger.info("MySQL config reloaded and tables updated.");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to reload MySQL config: " + e.getMessage(), e);
        }
        this.databaseAvailable = dbAvailable;
    }
} 