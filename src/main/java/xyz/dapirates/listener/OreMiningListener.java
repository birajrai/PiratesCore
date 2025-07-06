package xyz.dapirates.listener;

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
import xyz.dapirates.core.Core;
import xyz.dapirates.service.MiningSession;
import xyz.dapirates.utils.OreMiningConfig;
import xyz.dapirates.manager.DatabaseManager;
import xyz.dapirates.manager.MessageManager;
import xyz.dapirates.manager.OreMiningWebhook;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;

public class OreMiningListener implements Listener {

    private final Core plugin;
    private final OreMiningConfig config;
    private final DatabaseManager databaseManager;
    private final MessageManager messageManager;
    private final OreMiningWebhook oreMiningWebhook;
    private final Map<UUID, MiningSession> miningSessions;
    private final Set<UUID> ignoredPlayers;
    private final Set<UUID> toggledOffPlayers;

    public OreMiningListener(Core plugin) {
        this.plugin = plugin;
        this.config = new OreMiningConfig(plugin);
        this.databaseManager = plugin.getDatabaseManager();
        this.messageManager = plugin.getMessageManager();
        this.oreMiningWebhook = plugin.getOreMiningWebhook();
        this.miningSessions = new ConcurrentHashMap<>();
        this.ignoredPlayers = new HashSet<>();
        this.toggledOffPlayers = new HashSet<>();
        loadIgnoredPlayers();
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

        // Check if player is blacklisted (don't monitor their mining)
        if (ignoredPlayers.contains(player.getUniqueId())) {
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

        // Only update session data
        handleDelayedNotification(player, material, block.getLocation());
        // No legacy stats, no in-memory stats, no mining history
        // No DB write here
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
            if (nearestPlayer != null && nearestPlayer.hasPermission("pc.ores.notify")
                    && !ignoredPlayers.contains(nearestPlayer.getUniqueId())) {
                // No longer send TNT mining webhook notification
                handleDelayedNotification(nearestPlayer, material, block.getLocation());
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // No stats to initialize
        Player player = event.getPlayer();
        if (player.hasPermission("pc.ores.notify")) {
            sendWelcomeMessage(player);
        }
    }

    @EventHandler
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        MiningSession session = miningSessions.remove(playerId);
        if (session != null && session.hasMinedBlocks()) {
            saveSessionAsync(playerId, session);
        }
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

        // No longer send individual block webhook notification
        // oreMiningWebhook.sendIndividualBlockNotification(player, material, location,
        // false);

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

        // Send batched session webhook notification
        oreMiningWebhook.sendBatchedSessionNotification(player, minedBlocks, totalBlocks, sessionDuration);

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
        player.sendMessage("§7Available commands: toggle, reload, stats, top, ignore");
    }

    private void loadIgnoredPlayers() {
        List<String> ignoredNames = config.getIgnoredPlayers();
        for (String name : ignoredNames) {
            Player player = Bukkit.getPlayer(name);
            if (player != null) {
                ignoredPlayers.add(player.getUniqueId());
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

    public void addToIgnore(Player player) {
        ignoredPlayers.add(player.getUniqueId());
        config.addIgnoredPlayer(player.getName());
        player.sendMessage("§a[OreMining] §fAdded to ignore list.");
    }

    public void removeFromIgnore(Player player) {
        ignoredPlayers.remove(player.getUniqueId());
        config.removeIgnoredPlayer(player.getName());
        player.sendMessage("§a[OreMining] §fRemoved from ignore list.");
    }

    public boolean isPlayerIgnored(UUID playerId) {
        return ignoredPlayers.contains(playerId);
    }

    public boolean isPlayerToggledOff(UUID playerId) {
        return toggledOffPlayers.contains(playerId);
    }

    public void flushAllMiningSessionsToStats() {
        // On server stop, save all sessions to DB asynchronously
        for (Map.Entry<UUID, MiningSession> entry : miningSessions.entrySet()) {
            UUID playerId = entry.getKey();
            MiningSession session = entry.getValue();
            if (session != null && session.hasMinedBlocks()) {
                saveSessionAsync(playerId, session);
            }
        }
        miningSessions.clear();
    }

    private void saveSessionAsync(UUID playerId, MiningSession session) {
        // Save the session data to H2 asynchronously
        if (databaseManager != null && databaseManager.isDatabaseAvailable()) {
            databaseManager.saveOreMiningSessionAsync(playerId, session.getMinedBlocks());
        }
    }
}