package xyz.dapirates.service;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MiningSession {
    private final UUID playerId;
    private final String playerName;
    private final Map<Material, Integer> minedBlocks;
    private final long startTime;
    private long lastMiningTime;
    private boolean notificationScheduled;

    public MiningSession(final Player player) {
        this.playerId = player.getUniqueId();
        this.playerName = player.getName();
        this.minedBlocks = new HashMap<>();
        this.startTime = System.currentTimeMillis();
        this.lastMiningTime = System.currentTimeMillis();
        this.notificationScheduled = false;
    }

    public void addBlock(final Material material) {
        minedBlocks.merge(material, 1, Integer::sum);
        lastMiningTime = System.currentTimeMillis();
    }

    public Map<Material, Integer> getMinedBlocks() {
        return new HashMap<>(minedBlocks);
    }

    public int getTotalBlocks() {
        return minedBlocks.values().stream().mapToInt(Integer::intValue).sum();
    }

    public long getStartTime() {
        return startTime;
    }

    public long getLastMiningTime() {
        return lastMiningTime;
    }

    public long getSessionDuration() {
        return System.currentTimeMillis() - startTime;
    }

    public long getTimeSinceLastMining() {
        return System.currentTimeMillis() - lastMiningTime;
    }

    public String getPlayerName() {
        return playerName;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public boolean isNotificationScheduled() {
        return notificationScheduled;
    }

    public void setNotificationScheduled(final boolean notificationScheduled) {
        this.notificationScheduled = notificationScheduled;
    }

    public boolean hasMinedBlocks() {
        return !minedBlocks.isEmpty();
    }

    public void clear() {
        minedBlocks.clear();
    }
}