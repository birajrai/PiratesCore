package xyz.dapirates.service;

import org.bukkit.Material;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class OreMiningStats {
    private final UUID playerId;
    private final Map<Material, Integer> blockCounts;
    private final List<MiningEntry> miningHistory;
    private final int maxHistorySize;

    public OreMiningStats(final UUID playerId) {
        this.playerId = playerId;
        this.blockCounts = new ConcurrentHashMap<>();
        this.miningHistory = new ArrayList<>();
        this.maxHistorySize = 1000; // Keep last 1000 mining entries
    }

    public void addBlock(final Material material) {
        blockCounts.merge(material, 1, Integer::sum);

        // Add to history
        final MiningEntry entry = new MiningEntry(material, System.currentTimeMillis());
        miningHistory.add(entry);

        // Remove old entries if history is too large
        if (miningHistory.size() > maxHistorySize) {
            miningHistory.remove(0);
        }
    }

    public int getTotalBlocks() {
        return blockCounts.values().stream().mapToInt(Integer::intValue).sum();
    }

    public int getBlocksInTimeframe(final long currentTime, final long timeframeMs) {
        final long cutoffTime = currentTime - timeframeMs;
        return (int) miningHistory.stream()
                .filter(entry -> entry.timestamp >= cutoffTime)
                .count();
    }

    public Map<Material, Integer> getBlockCounts() {
        return new HashMap<>(blockCounts);
    }

    public int getBlockCount(final Material material) {
        return blockCounts.getOrDefault(material, 0);
    }

    public List<MiningEntry> getMiningHistory() {
        return new ArrayList<>(miningHistory);
    }

    public List<MiningEntry> getMiningHistory(final long since) {
        return miningHistory.stream()
                .filter(entry -> entry.timestamp >= since)
                .toList();
    }

    public void clearHistory() {
        miningHistory.clear();
    }

    public void clearStats() {
        blockCounts.clear();
        miningHistory.clear();
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public static class MiningEntry {
        private final Material material;
        private final long timestamp;

        public MiningEntry(final Material material, final long timestamp) {
            this.material = material;
            this.timestamp = timestamp;
        }

        public Material getMaterial() {
            return material;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }
}