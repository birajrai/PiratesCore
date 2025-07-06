package xyz.dapirates.features;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import xyz.dapirates.Core;
import xyz.dapirates.data.OreMiningData;
import xyz.dapirates.data.OreMiningStats;
import xyz.dapirates.data.MiningSession;
import xyz.dapirates.utils.OreMiningConfig;
import xyz.dapirates.managers.DatabaseManager;
import xyz.dapirates.managers.MessageManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;

public class OreMiningNotifier implements Listener {

    private final Core plugin;
    private final OreMiningConfig config;
    private final DatabaseManager databaseManager;
    private final MessageManager messageManager;
    private final Map<UUID, OreMiningStats> playerStats;
    private final Map<UUID, Long> lastMiningTime;
    private final Map<UUID, MiningSession> miningSessions;
    private final Set<UUID> whitelistedPlayers;
    private final Set<UUID> toggledOffPlayers;
    private final Map<UUID, Long> lastAlertTime = new ConcurrentHashMap<>();

    public OreMiningNotifier(Core plugin) {
        this.plugin = plugin;
        this.config = new OreMiningConfig(plugin);
        this.databaseManager = plugin.getDatabaseManager();
        this.messageManager = plugin.getMessageManager();
        this.playerStats = new ConcurrentHashMap<>();
        this.lastMiningTime = new ConcurrentHashMap<>();
        this.miningSessions = new ConcurrentHashMap<>();
        this.whitelistedPlayers = new HashSet<>();
        this.toggledOffPlayers = new HashSet<>();

        // Load whitelisted players from config
        loadWhitelistedPlayers();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Material material = block.getType();

        // Check if player has permission
        if (!player.hasPermission("pc.ores.notify")) {
            return;
        }

        // Check if player has toggled off notifications
        if (toggledOffPlayers.contains(player.getUniqueId())) {
            return;
        }

        // Check if block is configured for notifications
        if (!config.isBlockTracked(material)) {
            return;
        }

        // Check height restrictions
        if (!config.isHeightAllowed(block.getY())) {
            return;
        }

        // Check light level restrictions
        if (!config.isLightLevelAllowed(block.getLightLevel())) {
            return;
        }

        // Update player stats
        updatePlayerStats(player, material, block.getLocation());

        // Handle delayed notifications
        if (config.isDelayedNotificationsEnabled()) {
            handleDelayedNotification(player, material, block.getLocation());
        } else {
            // Send immediate notifications
            sendOreMiningNotification(player, material, block.getLocation());
        }

        // Execute custom commands
        executeCustomCommands(player, material, block.getLocation());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (!config.isTNTMiningEnabled()) {
            return;
        }

        for (Block block : event.blockList()) {
            Material material = block.getType();

            if (!config.isBlockTracked(material)) {
                continue;
            }

            // Find the nearest player for TNT mining attribution
            Player nearestPlayer = findNearestPlayer(block.getLocation());
            if (nearestPlayer != null && nearestPlayer.hasPermission("pc.ores.notify")) {
                updatePlayerStats(nearestPlayer, material, block.getLocation());
                sendOreMiningNotification(nearestPlayer, material, block.getLocation(), true);
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Initialize player stats if not exists
        if (!playerStats.containsKey(player.getUniqueId())) {
            playerStats.put(player.getUniqueId(), new OreMiningStats(player.getUniqueId()));
        }

        // Send welcome message if player has permission
        if (player.hasPermission("pc.ores.notify")) {
            sendWelcomeMessage(player);
        }
    }

    private void updatePlayerStats(Player player, Material material, Location location) {
        UUID playerId = player.getUniqueId();
        OreMiningStats stats = playerStats.computeIfAbsent(playerId, OreMiningStats::new);

        // Update block count
        stats.addBlock(material);

        // Update time-based stats
        long currentTime = System.currentTimeMillis();
        lastMiningTime.put(playerId, currentTime);

        // Save to database asynchronously if available
        if (databaseManager != null) {
            databaseManager.savePlayerStatsAsync(stats).thenRun(() -> {
                databaseManager.updateCache(playerId, stats);
            });

            // Add mining entry to database asynchronously
            databaseManager.addMiningEntryAsync(playerId, material, location.getWorld().getName(),
                    location.getBlockX(), location.getBlockY(), location.getBlockZ(), false);
        }

        // Check for time-based alerts
        checkTimeBasedAlerts(player, stats, currentTime);
    }

    private void sendOreMiningNotification(Player miner, Material material, Location location) {
        sendOreMiningNotification(miner, material, location, false);
    }

    private void sendOreMiningNotification(Player miner, Material material, Location location, boolean isTNT) {
        String message = config.getCustomMessage(material, miner, location, isTNT);
        Sound sound = config.getCustomSound(material);

        // Send to console if enabled
        if (config.isConsoleNotificationsEnabled()) {
            messageManager.sendConsoleMessage(message);
        }

        // Collect players to notify
        List<Player> playersToNotify = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("pc.ores.notify") && !toggledOffPlayers.contains(player.getUniqueId())) {
                // Check whitelist if enabled
                if (config.isWhitelistEnabled() && !whitelistedPlayers.contains(player.getUniqueId())) {
                    continue;
                }

                // Don't send to self if self-notifications are disabled
                if (player.equals(miner) && !config.isSelfNotificationsEnabled()) {
                    continue;
                }

                playersToNotify.add(player);
            }
        }

        // Send messages asynchronously
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            messageManager.broadcastMessage(playersToNotify, message, material, location, isTNT);

            // Play sounds on main thread
            Bukkit.getScheduler().runTask(plugin, () -> {
                for (Player player : playersToNotify) {
                    if (sound != null) {
                        player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
                    }
                }
            });
        });
    }

    private void executeCustomCommands(Player player, Material material, Location location) {
        List<String> commands = config.getCustomCommands(material);

        for (String command : commands) {
            String processedCommand = command
                    .replace("{player}", player.getName())
                    .replace("{block}", material.name())
                    .replace("{x}", String.valueOf(location.getBlockX()))
                    .replace("{y}", String.valueOf(location.getBlockY()))
                    .replace("{z}", String.valueOf(location.getBlockZ()))
                    .replace("{world}", location.getWorld().getName());

            if (processedCommand.startsWith("/")) {
                processedCommand = processedCommand.substring(1);
            }

            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processedCommand);
        }
    }

    private String getPrettyOreName(org.bukkit.Material material) {
        switch (material) {
            case DIAMOND_ORE:
            case DEEPSLATE_DIAMOND_ORE:
                return "§bDiamond Ore";
            case EMERALD_ORE:
            case DEEPSLATE_EMERALD_ORE:
                return "§aEmerald Ore";
            case GOLD_ORE:
            case DEEPSLATE_GOLD_ORE:
            case NETHER_GOLD_ORE:
                return "§6Gold Ore";
            case IRON_ORE:
            case DEEPSLATE_IRON_ORE:
                return "§7Iron Ore";
            case COPPER_ORE:
            case DEEPSLATE_COPPER_ORE:
                return "§cCopper Ore";
            case COAL_ORE:
            case DEEPSLATE_COAL_ORE:
                return "§8Coal Ore";
            case REDSTONE_ORE:
            case DEEPSLATE_REDSTONE_ORE:
                return "§4Redstone Ore";
            case LAPIS_ORE:
            case DEEPSLATE_LAPIS_ORE:
                return "§9Lapis Ore";
            case ANCIENT_DEBRIS:
                return "§5Ancient Debris";
            case NETHER_QUARTZ_ORE:
                return "§fNether Quartz Ore";
            default:
                return "§f" + material.name().replace("_", " ").toLowerCase();
        }
    }

    private void checkTimeBasedAlerts(Player player, OreMiningStats stats, long currentTime) {
        int blocksInTimeframe = stats.getBlocksInTimeframe(currentTime, config.getTimeBasedAlertTimeframe());
        int threshold = config.getTimeBasedAlertThreshold();
        long cooldown = config.getTimeBasedAlertTimeframe(); // Use the same as the timeframe for cooldown
        UUID playerId = player.getUniqueId();
        long lastAlert = lastAlertTime.getOrDefault(playerId, 0L);
        if (blocksInTimeframe >= threshold && (currentTime - lastAlert > cooldown)) {
            // Build breakdown per ore type
            long cutoffTime = currentTime - config.getTimeBasedAlertTimeframe();
            var history = stats.getMiningHistory(cutoffTime);
            Map<org.bukkit.Material, Integer> oreCounts = new java.util.HashMap<>();
            for (var entry : history) {
                oreCounts.merge(entry.getMaterial(), 1, Integer::sum);
            }
            StringBuilder breakdown = new StringBuilder();
            for (var e : oreCounts.entrySet()) {
                if (breakdown.length() > 0)
                    breakdown.append("§7, ");
                breakdown.append(getPrettyOreName(e.getKey())).append(": §e").append(e.getValue());
            }
            String alertMessage = "§c[OreMining] §f" + player.getName() + " has mined: " + breakdown;
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (onlinePlayer.hasPermission("pc.ores.notify.admin")) {
                    onlinePlayer.sendMessage(alertMessage);
                }
            }
            if (config.isConsoleNotificationsEnabled()) {
                Bukkit.getConsoleSender().sendMessage(alertMessage);
            }
            lastAlertTime.put(playerId, currentTime);
        }
    }

    private Player findNearestPlayer(Location location) {
        Player nearestPlayer = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Player player : Bukkit.getOnlinePlayers()) {
            double distance = player.getLocation().distance(location);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestPlayer = player;
            }
        }

        return nearestPlayer;
    }

    private void handleDelayedNotification(Player player, Material material, org.bukkit.Location location) {
        UUID playerId = player.getUniqueId();

        // Get or create mining session
        MiningSession session = miningSessions.computeIfAbsent(playerId, k -> new MiningSession(player));
        session.addBlock(material);

        // Schedule delayed notification if not already scheduled
        if (!session.isNotificationScheduled()) {
            session.setNotificationScheduled(true);

            int delaySeconds = config.getDelayedNotificationDelay();
            Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
                sendDelayedNotification(player, session);
                miningSessions.remove(playerId);
            }, delaySeconds * 20L); // Convert seconds to ticks
        }
    }

    private void sendDelayedNotification(Player player, MiningSession session) {
        if (!session.hasMinedBlocks()) {
            return;
        }

        Map<Material, Integer> minedBlocks = session.getMinedBlocks();
        int totalBlocks = session.getTotalBlocks();
        long sessionDuration = session.getSessionDuration();

        // Create batched message
        String message = createBatchedMessage(player, minedBlocks, totalBlocks, sessionDuration);

        // Send to console if enabled
        if (config.isConsoleNotificationsEnabled()) {
            messageManager.sendConsoleMessage(message);
        }

        // Collect players to notify
        List<Player> playersToNotify = new ArrayList<>();
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer.hasPermission("pc.ores.notify")
                    && !toggledOffPlayers.contains(onlinePlayer.getUniqueId())) {
                // Check whitelist if enabled
                if (config.isWhitelistEnabled() && !whitelistedPlayers.contains(onlinePlayer.getUniqueId())) {
                    continue;
                }

                // Don't send to self if self-notifications are disabled
                if (onlinePlayer.equals(player) && !config.isSelfNotificationsEnabled()) {
                    continue;
                }

                playersToNotify.add(onlinePlayer);
            }
        }

        // Send batched message
        if (config.isBatchMessagesEnabled()) {
            messageManager.broadcastMessage(playersToNotify, message, null, null, false);
        } else {
            // Send individual messages for each block type
            for (Map.Entry<Material, Integer> entry : minedBlocks.entrySet()) {
                Material material = entry.getKey();
                int count = entry.getValue();
                String individualMessage = createIndividualMessage(player, material, count, sessionDuration);
                messageManager.broadcastMessage(playersToNotify, individualMessage, material, null, false);
            }
        }
    }

    private String createBatchedMessage(Player player, Map<Material, Integer> minedBlocks, int totalBlocks,
            long sessionDuration) {
        StringBuilder message = new StringBuilder();
        message.append("§a[OreMining] §f").append(player.getName()).append(" mined ");

        if (config.isBatchMessagesEnabled()) {
            // Create a summary message
            List<String> blockEntries = new ArrayList<>();
            for (Map.Entry<Material, Integer> entry : minedBlocks.entrySet()) {
                String blockName = entry.getKey().name().replace("_", " ");
                int count = entry.getValue();
                blockEntries.add("§e" + count + "x §f" + blockName);
            }

            message.append(String.join("§7, ", blockEntries));
            message.append(" §7(").append(totalBlocks).append(" total)");
        } else {
            // Just show total count
            message.append("§e").append(totalBlocks).append(" blocks");
        }

        // Add time information
        long seconds = sessionDuration / 1000;
        if (seconds < 60) {
            message.append(" §7in ").append(seconds).append("s");
        } else {
            long minutes = seconds / 60;
            long remainingSeconds = seconds % 60;
            message.append(" §7in ").append(minutes).append("m ").append(remainingSeconds).append("s");
        }

        return message.toString();
    }

    private String createIndividualMessage(Player player, Material material, int count, long sessionDuration) {
        String blockName = material.name().replace("_", " ");
        String message = "§a[OreMining] §f" + player.getName() + " mined §e" + count + "x §f" + blockName;

        // Add time information
        long seconds = sessionDuration / 1000;
        if (seconds < 60) {
            message += " §7in " + seconds + "s";
        } else {
            long minutes = seconds / 60;
            long remainingSeconds = seconds % 60;
            message += " §7in " + minutes + "m " + remainingSeconds + "s";
        }

        return message;
    }

    private void sendWelcomeMessage(Player player) {
        player.sendMessage("§a[OreMining] §fWelcome! You have ore mining notifications enabled.");
        player.sendMessage("§7Available commands: toggle, reload, stats, top, whitelist, clear, logs");
    }

    private void loadWhitelistedPlayers() {
        List<String> whitelistedNames = config.getWhitelistedPlayers();
        for (String name : whitelistedNames) {
            // Try to get UUID from name (this is a simplified approach)
            // In a real implementation, you might want to use a UUID cache or database
            Player player = Bukkit.getPlayer(name);
            if (player != null) {
                whitelistedPlayers.add(player.getUniqueId());
            }
        }
    }

    // Public methods for commands
    public void toggleNotifications(Player player) {
        UUID playerId = player.getUniqueId();
        if (toggledOffPlayers.contains(playerId)) {
            toggledOffPlayers.remove(playerId);
            player.sendMessage("§a[OreMining] §fNotifications enabled.");
        } else {
            toggledOffPlayers.add(playerId);
            player.sendMessage("§a[OreMining] §fNotifications disabled.");
        }
    }

    public void addToWhitelist(Player player) {
        whitelistedPlayers.add(player.getUniqueId());
        config.addWhitelistedPlayer(player.getName());
        player.sendMessage("§a[OreMining] §fAdded to whitelist.");
    }

    public void removeFromWhitelist(Player player) {
        whitelistedPlayers.remove(player.getUniqueId());
        config.removeWhitelistedPlayer(player.getName());
        player.sendMessage("§a[OreMining] §fRemoved from whitelist.");
    }

    public CompletableFuture<List<OreMiningStats>> getTopPlayersAsync(int limit) {
        if (databaseManager == null || !databaseManager.isDatabaseAvailable()) {
            // Fallback to in-memory stats
            return CompletableFuture.completedFuture(
                    playerStats.values().stream()
                            .sorted(Comparator.comparingInt(OreMiningStats::getTotalBlocks).reversed())
                            .limit(limit)
                            .toList());
        }
        return databaseManager.getTopPlayersAsync(limit);
    }

    public List<OreMiningStats> getTopPlayers(int limit) {
        try {
            return getTopPlayersAsync(limit).get();
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get top players: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public OreMiningStats getPlayerStats(UUID playerId) {
        // Try cache first
        OreMiningStats stats = playerStats.get(playerId);
        if (stats != null) {
            return stats;
        }

        // Try database if available
        if (databaseManager != null && databaseManager.isDatabaseAvailable()) {
            try {
                stats = databaseManager.loadPlayerStatsAsync(playerId).get();
                if (stats != null) {
                    playerStats.put(playerId, stats);
                    return stats;
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load player stats from database: " + e.getMessage());
            }
        }

        return null;
    }

    public Map<Material, Integer> getPlayerBlockStats(UUID playerId) {
        OreMiningStats stats = playerStats.get(playerId);
        return stats != null ? stats.getBlockCounts() : new HashMap<>();
    }

    public void clearPlayerStats(UUID playerId) {
        playerStats.remove(playerId);
        lastMiningTime.remove(playerId);
        if (databaseManager != null) {
            databaseManager.clearPlayerStatsAsync(playerId);
        }
    }

    public void clearAllStats() {
        playerStats.clear();
        lastMiningTime.clear();
        if (databaseManager != null) {
            databaseManager.clearAllStatsAsync();
        }
    }

    public boolean isPlayerWhitelisted(UUID playerId) {
        return whitelistedPlayers.contains(playerId);
    }

    public boolean isPlayerToggledOff(UUID playerId) {
        return toggledOffPlayers.contains(playerId);
    }

    public Set<UUID> getAllPlayerIds() {
        return playerStats.keySet();
    }

    public void flushAllMiningSessionsToStats() {
        for (Map.Entry<UUID, MiningSession> entry : miningSessions.entrySet()) {
            UUID playerId = entry.getKey();
            MiningSession session = entry.getValue();
            if (session != null && session.hasMinedBlocks()) {
                OreMiningStats stats = playerStats.computeIfAbsent(playerId, OreMiningStats::new);
                for (Map.Entry<org.bukkit.Material, Integer> blockEntry : session.getMinedBlocks().entrySet()) {
                    for (int i = 0; i < blockEntry.getValue(); i++) {
                        stats.addBlock(blockEntry.getKey());
                    }
                }
            }
        }
        miningSessions.clear();
    }
}