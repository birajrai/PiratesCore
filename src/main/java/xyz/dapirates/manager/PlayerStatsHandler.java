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
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(playtime);
                stmt.execute(kills);
                stmt.execute(deaths);
                stmt.execute(balance);
                stmt.execute(topbalance);
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
            for (Player player : Bukkit.getOnlinePlayers()) {
                String name = player.getName();
                long playtimeTicks = player.getStatistic(Statistic.PLAY_ONE_MINUTE);
                long playtimeSeconds = playtimeTicks / 20L;
                savePlaytimeAsync(name, playtimeSeconds);
                // For demo, kills and deaths are not tracked in memory, so skip or load/save as needed
                loadKillsAsync(name).thenAccept(kills -> saveKillsAsync(name, kills));
                loadDeathsAsync(name).thenAccept(deaths -> saveDeathsAsync(name, deaths));
                if (econ != null) {
                    double balance = econ.getBalance(player);
                    saveBalanceAsync(name, balance);
                }
            }
            // Update balancetop leaderboard
            if (econ != null) {
                List<org.bukkit.entity.Player> onlinePlayers = new java.util.ArrayList<>(Bukkit.getOnlinePlayers());
                onlinePlayers.sort(Comparator.comparingDouble(p -> econ.getBalance((org.bukkit.OfflinePlayer)p)).reversed());
                int topN = Math.min(10, onlinePlayers.size());
                List<org.bukkit.entity.Player> topPlayers = onlinePlayers.subList(0, topN);
                updateTopBalanceLeaderboard(topPlayers, econ);
            }
        });
    }
} 