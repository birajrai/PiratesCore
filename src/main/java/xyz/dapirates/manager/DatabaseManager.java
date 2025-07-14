package xyz.dapirates.manager;

import org.bukkit.Material;
import xyz.dapirates.core.Core;
import xyz.dapirates.service.OreMiningStats;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Handles all database operations for ore mining statistics and player data.
 * Uses H2 embedded database and caches player stats in memory.
 */
public class DatabaseManager {

    private final Core plugin;
    private final String dbUrl;
    private final Map<UUID, OreMiningStats> cache;
    private final boolean databaseAvailable;

    /**
     * Constructs a DatabaseManager for the plugin, initializing the H2 database.
     * @param plugin The main plugin instance
     */
    public DatabaseManager(Core plugin) {
        this.plugin = plugin;
        this.cache = new ConcurrentHashMap<>();

        File dbFile = new File(plugin.getDataFolder(), "oremining");
        this.dbUrl = "jdbc:h2:" + dbFile.getAbsolutePath() + ";DB_CLOSE_ON_EXIT=FALSE;DB_CLOSE_DELAY=-1";

        // Load H2 driver
        boolean dbAvailable = false;
        try {
            // Try the relocated H2 driver first (bundled with plugin)
            Class.forName("xyz.dapirates.libs.h2.Driver");
            initializeDatabase();
            dbAvailable = true;
        } catch (ClassNotFoundException e1) {
            try {
                // Fallback to original H2 driver (if available externally)
                Class.forName("org.h2.Driver");
                initializeDatabase();
                dbAvailable = true;
            } catch (ClassNotFoundException e2) {
                plugin.getLogger().severe("H2 database driver not found! Database features will be disabled.");
                plugin.getLogger().severe("Please ensure H2 database is properly included in the plugin.");
            } catch (Exception e3) {
                plugin.getLogger().severe("Failed to initialize database: " + e3.getMessage());
                plugin.getLogger().severe("Database features will be disabled.");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to initialize database: " + e.getMessage());
            plugin.getLogger().severe("Database features will be disabled.");
        }

        this.databaseAvailable = dbAvailable;
    }

    private void initializeDatabase() {
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            // Create tables if they don't exist
            createTables(conn);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize database", e);
        }
    }

    private void createTables(Connection conn) throws SQLException {
        // Player stats table
        String createPlayerStats = """
                CREATE TABLE IF NOT EXISTS player_stats (
                    player_uuid VARCHAR(36) PRIMARY KEY,
                    player_name VARCHAR(16) NOT NULL,
                    total_blocks INT DEFAULT 0,
                    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """;

        // Block counts table
        String createBlockCounts = """
                CREATE TABLE IF NOT EXISTS block_counts (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    player_uuid VARCHAR(36) NOT NULL,
                    material VARCHAR(50) NOT NULL,
                    count INT DEFAULT 0,
                    FOREIGN KEY (player_uuid) REFERENCES player_stats(player_uuid) ON DELETE CASCADE
                )
                """;

        // Mining history table
        String createMiningHistory = """
                CREATE TABLE IF NOT EXISTS mining_history (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    player_uuid VARCHAR(36) NOT NULL,
                    material VARCHAR(50) NOT NULL,
                    world VARCHAR(50) NOT NULL,
                    x INT NOT NULL,
                    y INT NOT NULL,
                    z INT NOT NULL,
                    is_tnt BOOLEAN DEFAULT FALSE,
                    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (player_uuid) REFERENCES player_stats(player_uuid) ON DELETE CASCADE
                )
                """;

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(createPlayerStats);
            stmt.execute(createBlockCounts);
            stmt.execute(createMiningHistory);
        }
    }

    // Async methods for database operations
    public CompletableFuture<Void> savePlayerStatsAsync(OreMiningStats stats) {
        if (!databaseAvailable) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.runAsync(() -> {
            try (Connection conn = DriverManager.getConnection(dbUrl)) {
                savePlayerStats(conn, stats);
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save player stats", e);
            }
        });
    }

    /**
     * Loads player stats from the database asynchronously.
     * @param playerId The player's UUID
     * @return CompletableFuture for OreMiningStats
     */
    public CompletableFuture<OreMiningStats> loadPlayerStatsAsync(UUID playerId) {
        if (!databaseAvailable) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = DriverManager.getConnection(dbUrl)) {
                return loadPlayerStats(conn, playerId);
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load player stats", e);
                return null;
            }
        });
    }

    public CompletableFuture<List<OreMiningStats>> getTopPlayersAsync(int limit) {
        if (!databaseAvailable) {
            return CompletableFuture.completedFuture(new ArrayList<>());
        }

        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = DriverManager.getConnection(dbUrl)) {
                return getTopPlayers(conn, limit);
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to get top players", e);
                return new ArrayList<>();
            }
        });
    }

    public CompletableFuture<Void> addMiningEntryAsync(UUID playerId, Material material, String world, int x, int y,
            int z, boolean isTNT) {
        if (!databaseAvailable) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.runAsync(() -> {
            try (Connection conn = DriverManager.getConnection(dbUrl)) {
                addMiningEntry(conn, playerId, material, world, x, y, z, isTNT);
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to add mining entry", e);
            }
        });
    }

    public CompletableFuture<Void> clearPlayerStatsAsync(UUID playerId) {
        if (!databaseAvailable) {
            cache.remove(playerId);
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.runAsync(() -> {
            try (Connection conn = DriverManager.getConnection(dbUrl)) {
                clearPlayerStats(conn, playerId);
                cache.remove(playerId);
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to clear player stats", e);
            }
        });
    }

    public CompletableFuture<Void> clearAllStatsAsync() {
        if (!databaseAvailable) {
            cache.clear();
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.runAsync(() -> {
            try (Connection conn = DriverManager.getConnection(dbUrl)) {
                clearAllStats(conn);
                cache.clear();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to clear all stats", e);
            }
        });
    }

    /**
     * Saves a mining session to the database asynchronously.
     * @param playerId The player's UUID
     * @param minedBlocks The blocks mined in the session
     */
    public CompletableFuture<Void> saveOreMiningSessionAsync(UUID playerId, Map<Material, Integer> minedBlocks) {
        if (!databaseAvailable) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.runAsync(() -> {
            try (Connection conn = DriverManager.getConnection(dbUrl)) {
                saveOreMiningSession(conn, playerId, minedBlocks);
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save ore mining session", e);
            }
        });
    }

    // Synchronous database operations
    private void savePlayerStats(Connection conn, OreMiningStats stats) throws SQLException {
        // Insert or update player stats
        String upsertPlayer = """
                MERGE INTO player_stats (player_uuid, player_name, total_blocks, last_updated)
                VALUES (?, ?, ?, CURRENT_TIMESTAMP)
                """;

        try (PreparedStatement pstmt = conn.prepareStatement(upsertPlayer)) {
            pstmt.setString(1, stats.getPlayerId().toString());
            pstmt.setString(2, getPlayerName(stats.getPlayerId()));
            pstmt.setInt(3, stats.getTotalBlocks());
            pstmt.executeUpdate();
        }

        // Update block counts
        String deleteBlockCounts = "DELETE FROM block_counts WHERE player_uuid = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(deleteBlockCounts)) {
            pstmt.setString(1, stats.getPlayerId().toString());
            pstmt.executeUpdate();
        }

        String insertBlockCount = "INSERT INTO block_counts (player_uuid, material, count) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(insertBlockCount)) {
            for (Map.Entry<Material, Integer> entry : stats.getBlockCounts().entrySet()) {
                pstmt.setString(1, stats.getPlayerId().toString());
                pstmt.setString(2, entry.getKey().name());
                pstmt.setInt(3, entry.getValue());
                pstmt.executeUpdate();
            }
        }
    }

    private OreMiningStats loadPlayerStats(Connection conn, UUID playerId) throws SQLException {
        // Check cache first
        if (cache.containsKey(playerId)) {
            return cache.get(playerId);
        }

        OreMiningStats stats = new OreMiningStats(playerId);

        // Load block counts
        String loadBlockCounts = "SELECT material, count FROM block_counts WHERE player_uuid = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(loadBlockCounts)) {
            pstmt.setString(1, playerId.toString());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Material material = Material.valueOf(rs.getString("material"));
                    int count = rs.getInt("count");
                    for (int i = 0; i < count; i++) {
                        stats.addBlock(material);
                    }
                }
            }
        }

        // Cache the stats
        cache.put(playerId, stats);
        return stats;
    }

    private List<OreMiningStats> getTopPlayers(Connection conn, int limit) throws SQLException {
        List<OreMiningStats> topPlayers = new ArrayList<>();

        String query = """
                SELECT player_uuid, total_blocks
                FROM player_stats
                ORDER BY total_blocks DESC
                LIMIT ?
                """;

        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, limit);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    UUID playerId = UUID.fromString(rs.getString("player_uuid"));
                    OreMiningStats stats = loadPlayerStats(conn, playerId);
                    topPlayers.add(stats);
                }
            }
        }

        return topPlayers;
    }

    private void addMiningEntry(Connection conn, UUID playerId, Material material, String world, int x, int y, int z,
            boolean isTNT) throws SQLException {
        // Ensure player exists in player_stats
        OreMiningStats dummyStats = new OreMiningStats(playerId);
        savePlayerStats(conn, dummyStats);
        String insertHistory = """
                INSERT INTO mining_history (player_uuid, material, world, x, y, z, is_tnt)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement pstmt = conn.prepareStatement(insertHistory)) {
            pstmt.setString(1, playerId.toString());
            pstmt.setString(2, material.name());
            pstmt.setString(3, world);
            pstmt.setInt(4, x);
            pstmt.setInt(5, y);
            pstmt.setInt(6, z);
            pstmt.setBoolean(7, isTNT);
            pstmt.executeUpdate();
        }
    }

    private void clearPlayerStats(Connection conn, UUID playerId) throws SQLException {
        String deletePlayer = "DELETE FROM player_stats WHERE player_uuid = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(deletePlayer)) {
            pstmt.setString(1, playerId.toString());
            pstmt.executeUpdate();
        }
    }

    private void clearAllStats(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM mining_history");
            stmt.execute("DELETE FROM block_counts");
            stmt.execute("DELETE FROM player_stats");
        }
    }

    private void saveOreMiningSession(Connection conn, UUID playerId, Map<Material, Integer> minedBlocks)
            throws SQLException {
        // Create or update player stats entry
        String upsertPlayer = """
                MERGE INTO player_stats (player_uuid, player_name, total_blocks, last_updated)
                VALUES (?, ?, ?, CURRENT_TIMESTAMP)
                """;

        int totalBlocks = minedBlocks.values().stream().mapToInt(Integer::intValue).sum();

        try (PreparedStatement pstmt = conn.prepareStatement(upsertPlayer)) {
            pstmt.setString(1, playerId.toString());
            pstmt.setString(2, getPlayerName(playerId));
            pstmt.setInt(3, totalBlocks);
            pstmt.executeUpdate();
        }

        // Update block counts for this session
        String deleteBlockCounts = "DELETE FROM block_counts WHERE player_uuid = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(deleteBlockCounts)) {
            pstmt.setString(1, playerId.toString());
            pstmt.executeUpdate();
        }

        String insertBlockCount = "INSERT INTO block_counts (player_uuid, material, count) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(insertBlockCount)) {
            for (Map.Entry<Material, Integer> entry : minedBlocks.entrySet()) {
                pstmt.setString(1, playerId.toString());
                pstmt.setString(2, entry.getKey().name());
                pstmt.setInt(3, entry.getValue());
                pstmt.executeUpdate();
            }
        }
    }

    private String getPlayerName(UUID playerId) {
        // Try to get from online players first
        var player = plugin.getServer().getPlayer(playerId);
        if (player != null) {
            return player.getName();
        }

        // Try to get from offline players
        var offlinePlayer = plugin.getServer().getOfflinePlayer(playerId);
        if (offlinePlayer.getName() != null) {
            return offlinePlayer.getName();
        }

        return "Unknown";
    }

    /**
     * Closes the database connection and clears the cache.
     */
    public void close() {
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            // H2 will close automatically when the last connection is closed
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to close database connection", e);
        }
    }

    // Cache management
    public void updateCache(UUID playerId, OreMiningStats stats) {
        cache.put(playerId, stats);
    }

    public OreMiningStats getFromCache(UUID playerId) {
        return cache.get(playerId);
    }

    public void removeFromCache(UUID playerId) {
        cache.remove(playerId);
    }

    /**
     * Checks if the database is available and initialized.
     * @return true if available, false otherwise
     */
    public boolean isDatabaseAvailable() {
        return databaseAvailable;
    }
}