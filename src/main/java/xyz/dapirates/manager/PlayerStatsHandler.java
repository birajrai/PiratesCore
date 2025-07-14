package xyz.dapirates.manager;

import xyz.dapirates.core.Core;
import xyz.dapirates.manager.ConfigManager;

import java.sql.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.Statistic;
import net.milkbowl.vault.economy.Economy;
import java.util.Comparator;
import java.util.stream.Collectors;

/**
 * Handles player stats (playtime, kills, deaths, CMI balance, topbalance) in MySQL using username as the key.
 * All operations are async to prevent server lag.
 */
public class PlayerStatsHandler {
    private final Core plugin;
    private String mysqlUrl;
    private String mysqlUser;
    private String mysqlPassword;
    private boolean databaseAvailable;
    private final java.util.logging.Logger logger;

    public PlayerStatsHandler(Core plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        // Load MySQL config from ConfigManager
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
            String playtime = """
                CREATE TABLE IF NOT EXISTS player_playtime (
                    username VARCHAR(32) PRIMARY KEY,
                    playtime BIGINT NOT NULL
                )""";
            String kills = """
                CREATE TABLE IF NOT EXISTS player_kills (
                    username VARCHAR(32) PRIMARY KEY,
                    kills INT NOT NULL
                )""";
            String deaths = """
                CREATE TABLE IF NOT EXISTS player_deaths (
                    username VARCHAR(32) PRIMARY KEY,
                    deaths INT NOT NULL
                )""";
            String balance = """
                CREATE TABLE IF NOT EXISTS player_cmi_balance (
                    username VARCHAR(32) PRIMARY KEY,
                    balance DOUBLE NOT NULL
                )""";
            String topbalance = """
                CREATE TABLE IF NOT EXISTS player_cmi_topbalance (
                    id INT PRIMARY KEY,
                    username VARCHAR(32) NOT NULL,
                    topbalance DOUBLE NOT NULL
                )""";
            String joinTop = """
                CREATE TABLE IF NOT EXISTS player_join_top (
                    id INT PRIMARY KEY,
                    username VARCHAR(32) NOT NULL,
                    count INT NOT NULL
                )""";
            String boatTop = """
                CREATE TABLE IF NOT EXISTS player_boat_top (
                    id INT PRIMARY KEY,
                    username VARCHAR(32) NOT NULL,
                    count INT NOT NULL
                )""";
            String leaveBoatTop = """
                CREATE TABLE IF NOT EXISTS player_leave_boat_top (
                    id INT PRIMARY KEY,
                    username VARCHAR(32) NOT NULL,
                    count INT NOT NULL
                )""";
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(playtime);
                stmt.execute(kills);
                stmt.execute(deaths);
                stmt.execute(balance);
                stmt.execute(topbalance);
                stmt.execute(joinTop);
                stmt.execute(boatTop);
                stmt.execute(leaveBoatTop);
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

    // Async save methods
    public CompletableFuture<Void> savePlaytimeAsync(String username, long playtime) {
        return CompletableFuture.runAsync(() -> {
            String sql = "REPLACE INTO player_playtime (username, playtime) VALUES (?, ?)";
            logger.info("Executing SQL: " + sql + " [username=" + username + ", playtime=" + playtime + "]");
            try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username);
                ps.setLong(2, playtime);
                ps.executeUpdate();
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Failed to save playtime for " + username, e);
            }
        });
    }

    public CompletableFuture<Void> saveKillsAsync(String username, int kills) {
        return CompletableFuture.runAsync(() -> {
            String sql = "REPLACE INTO player_kills (username, kills) VALUES (?, ?)";
            logger.info("Executing SQL: " + sql + " [username=" + username + ", kills=" + kills + "]");
            try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username);
                ps.setInt(2, kills);
                ps.executeUpdate();
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Failed to save kills for " + username, e);
            }
        });
    }

    public CompletableFuture<Void> saveDeathsAsync(String username, int deaths) {
        return CompletableFuture.runAsync(() -> {
            String sql = "REPLACE INTO player_deaths (username, deaths) VALUES (?, ?)";
            logger.info("Executing SQL: " + sql + " [username=" + username + ", deaths=" + deaths + "]");
            try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username);
                ps.setInt(2, deaths);
                ps.executeUpdate();
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Failed to save deaths for " + username, e);
            }
        });
    }

    public CompletableFuture<Void> saveBalanceAsync(String username, double balance) {
        return CompletableFuture.runAsync(() -> {
            String sql = "REPLACE INTO player_cmi_balance (username, balance) VALUES (?, ?)";
            logger.info("Executing SQL: " + sql + " [username=" + username + ", balance=" + balance + "]");
            try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username);
                ps.setDouble(2, balance);
                ps.executeUpdate();
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Failed to save balance for " + username, e);
            }
        });
    }

    // Update the balancetop leaderboard in the database for the top 10 players
    public void updateTopBalanceLeaderboard(List<org.bukkit.entity.Player> topPlayers, net.milkbowl.vault.economy.Economy econ) {
        CompletableFuture.runAsync(() -> {
            String clearSql = "DELETE FROM player_cmi_topbalance";
            String insertSql = "INSERT INTO player_cmi_topbalance (id, username, topbalance) VALUES (?, ?, ?)";
            try (Connection conn = getConnection(); Statement clearStmt = conn.createStatement()) {
                clearStmt.executeUpdate(clearSql);
                try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                    for (int i = 0; i < topPlayers.size(); i++) {
                        org.bukkit.entity.Player p = topPlayers.get(i);
                        ps.setInt(1, i + 1); // id 1-10
                        ps.setString(2, p.getName());
                        ps.setDouble(3, econ.getBalance(p));
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Failed to update balancetop leaderboard", e);
            }
        });
    }

    // Deprecated: use updateTopBalanceLeaderboard for leaderboard
    public CompletableFuture<Void> saveTopBalanceAsync(String username, double topbalance) {
        return CompletableFuture.runAsync(() -> {
            String sql = "REPLACE INTO player_cmi_topbalance (id, username, topbalance) VALUES (?, ?, ?)";
            logger.info("Executing SQL: " + sql + " [username=" + username + ", topbalance=" + topbalance + "]");
            try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                // This method is deprecated for leaderboard, but keep id as 0 for legacy
                ps.setInt(1, 0);
                ps.setString(2, username);
                ps.setDouble(3, topbalance);
                ps.executeUpdate();
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Failed to save topbalance for " + username, e);
            }
        });
    }

    // Save join/leave count for a player
    public CompletableFuture<Void> saveJoinLeaveCountAsync(String username, int count) {
        return CompletableFuture.runAsync(() -> {
            String sql = "REPLACE INTO player_join_leave_top (id, username, count) VALUES (?, ?, ?)";
            logger.info("Executing SQL: " + sql + " [username=" + username + ", count=" + count + "]");
            try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                // id is 0 for direct save, leaderboard will set real id
                ps.setInt(1, 0);
                ps.setString(2, username);
                ps.setInt(3, count);
                ps.executeUpdate();
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Failed to save join/leave count for " + username, e);
            }
        });
    }

    // Update the join/leave top 10 leaderboard
    public void updateJoinLeaveTopLeaderboard(List<org.bukkit.entity.Player> topPlayers, java.util.Map<String, Integer> joinLeaveCounts) {
        CompletableFuture.runAsync(() -> {
            String clearSql = "DELETE FROM player_join_leave_top";
            String insertSql = "INSERT INTO player_join_leave_top (id, username, count) VALUES (?, ?, ?)";
            try (Connection conn = getConnection(); Statement clearStmt = conn.createStatement()) {
                clearStmt.executeUpdate(clearSql);
                try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                    for (int i = 0; i < topPlayers.size(); i++) {
                        org.bukkit.entity.Player p = topPlayers.get(i);
                        int count = joinLeaveCounts.getOrDefault(p.getName(), 0);
                        ps.setInt(1, i + 1); // id 1-10
                        ps.setString(2, p.getName());
                        ps.setInt(3, count);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Failed to update join/leave top leaderboard", e);
            }
        });
    }

    // Save join count for a player
    public CompletableFuture<Void> saveJoinCountAsync(String username, int count) {
        return saveGenericTopAsync("player_join_top", username, count);
    }
    // Save boat mount count for a player
    public CompletableFuture<Void> saveBoatCountAsync(String username, int count) {
        return saveGenericTopAsync("player_boat_top", username, count);
    }
    // Save boat leave count for a player
    public CompletableFuture<Void> saveLeaveBoatCountAsync(String username, int count) {
        return saveGenericTopAsync("player_leave_boat_top", username, count);
    }
    // Generic async save for top tables
    private CompletableFuture<Void> saveGenericTopAsync(String table, String username, int count) {
        return CompletableFuture.runAsync(() -> {
            String sql = "REPLACE INTO " + table + " (id, username, count) VALUES (?, ?, ?)";
            logger.info("Executing SQL: " + sql + " [username=" + username + ", count=" + count + "]");
            try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, 0);
                ps.setString(2, username);
                ps.setInt(3, count);
                ps.executeUpdate();
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Failed to save count for " + username + " in " + table, e);
            }
        });
    }
    // Update a generic top 10 leaderboard
    public void updateGenericTopLeaderboard(String table, List<org.bukkit.entity.Player> topPlayers, java.util.Map<String, Integer> counts) {
        CompletableFuture.runAsync(() -> {
            String clearSql = "DELETE FROM " + table;
            String insertSql = "INSERT INTO " + table + " (id, username, count) VALUES (?, ?, ?)";
            try (Connection conn = getConnection(); Statement clearStmt = conn.createStatement()) {
                clearStmt.executeUpdate(clearSql);
                try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                    for (int i = 0; i < topPlayers.size(); i++) {
                        org.bukkit.entity.Player p = topPlayers.get(i);
                        int count = counts.getOrDefault(p.getName(), 0);
                        ps.setInt(1, i + 1); // id 1-10
                        ps.setString(2, p.getName());
                        ps.setInt(3, count);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Failed to update leaderboard for " + table, e);
            }
        });
    }

    // Async load methods
    public CompletableFuture<Long> loadPlaytimeAsync(String username) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT playtime FROM player_playtime WHERE username = ?";
            logger.info("Executing SQL: " + sql + " [username=" + username + "]");
            try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getLong("playtime");
                }
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Failed to load playtime for " + username, e);
            }
            return 0L;
        });
    }

    public CompletableFuture<Integer> loadKillsAsync(String username) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT kills FROM player_kills WHERE username = ?";
            logger.info("Executing SQL: " + sql + " [username=" + username + "]");
            try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getInt("kills");
                }
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Failed to load kills for " + username, e);
            }
            return 0;
        });
    }

    public CompletableFuture<Integer> loadDeathsAsync(String username) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT deaths FROM player_deaths WHERE username = ?";
            logger.info("Executing SQL: " + sql + " [username=" + username + "]");
            try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getInt("deaths");
                }
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Failed to load deaths for " + username, e);
            }
            return 0;
        });
    }

    public CompletableFuture<Double> loadBalanceAsync(String username) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT balance FROM player_cmi_balance WHERE username = ?";
            logger.info("Executing SQL: " + sql + " [username=" + username + "]");
            try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getDouble("balance");
                }
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Failed to load balance for " + username, e);
            }
            return 0.0;
        });
    }

    public CompletableFuture<Double> loadTopBalanceAsync(String username) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT topbalance FROM player_cmi_topbalance WHERE username = ?";
            logger.info("Executing SQL: " + sql + " [username=" + username + "]");
            try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getDouble("topbalance");
                }
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Failed to load topbalance for " + username, e);
            }
            return 0.0;
        });
    }

    public boolean isDatabaseAvailable() {
        return databaseAvailable;
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

    // Save all stats for all online players and update leaderboard
    public void saveAllOnlinePlayerStats(Economy econ) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            java.util.Map<String, Integer> joinCounts = new java.util.HashMap<>();
            java.util.Map<String, Integer> boatCounts = new java.util.HashMap<>();
            java.util.Map<String, Integer> leaveBoatCounts = new java.util.HashMap<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                String name = player.getName();
                long playtimeTicks = player.getStatistic(Statistic.PLAY_ONE_MINUTE);
                long playtimeSeconds = playtimeTicks / 20L;
                savePlaytimeAsync(name, playtimeSeconds);
                loadKillsAsync(name).thenAccept(kills -> saveKillsAsync(name, kills));
                loadDeathsAsync(name).thenAccept(deaths -> saveDeathsAsync(name, deaths));
                if (econ != null) {
                    double balance = econ.getBalance(player);
                    saveBalanceAsync(name, balance);
                }
                // For demo, increment all counts by 1 each time
                int join = 1, boat = 1, leaveBoat = 1;
                saveJoinCountAsync(name, join);
                saveBoatCountAsync(name, boat);
                saveLeaveBoatCountAsync(name, leaveBoat);
                joinCounts.put(name, join);
                boatCounts.put(name, boat);
                leaveBoatCounts.put(name, leaveBoat);
            }
            // Update balancetop leaderboard
            if (econ != null) {
                List<org.bukkit.entity.Player> onlinePlayers = new java.util.ArrayList<>(Bukkit.getOnlinePlayers());
                onlinePlayers.sort(Comparator.comparingDouble(p -> econ.getBalance((org.bukkit.OfflinePlayer)p)).reversed());
                int topN = Math.min(10, onlinePlayers.size());
                List<org.bukkit.entity.Player> topPlayers = onlinePlayers.subList(0, topN);
                updateTopBalanceLeaderboard(topPlayers, econ);
                // Update all other top leaderboards (for demo, use same top 10 as balance)
                updateGenericTopLeaderboard("player_join_top", topPlayers, joinCounts);
                updateGenericTopLeaderboard("player_boat_top", topPlayers, boatCounts);
                updateGenericTopLeaderboard("player_leave_boat_top", topPlayers, leaveBoatCounts);
            }
        });
    }
} 