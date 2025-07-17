package xyz.dapirates.pirates;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import xyz.dapirates.core.Core;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class OresTopCommand {
    private final Core plugin;
    public OresTopCommand(Core plugin) {
        this.plugin = plugin;
    }
    public void openOresTop(Player player, String ore) {
        getTopNOreBrokenAsync(ore, 10).thenAccept(list -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                Inventory inv = Bukkit.createInventory(null, 18, "Top Ores: " + ore);
                int slot = 0;
                for (var entry : list) {
                    ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                    SkullMeta skull = (SkullMeta) head.getItemMeta();
                    OfflinePlayer offline = Bukkit.getOfflinePlayer(entry.getKey());
                    skull.setOwningPlayer(offline);
                    skull.setDisplayName(entry.getKey());
                    skull.setLore(Collections.singletonList("Broken: " + entry.getValue()));
                    head.setItemMeta(skull);
                    inv.setItem(slot, head);
                    slot++;
                }
                player.openInventory(inv);
            });
        });
    }
    public CompletableFuture<List<Map.Entry<String, Integer>>> getTopNOreBrokenAsync(String ore, int n) {
        Material mat = Material.getMaterial(ore);
        if (mat == null) return CompletableFuture.completedFuture(Collections.emptyList());
        return plugin.getDatabaseManager().getTopPlayersAsync(100).thenApply(statsList -> {
            List<Map.Entry<String, Integer>> result = new ArrayList<>();
            for (var stats : statsList) {
                String name = Bukkit.getOfflinePlayer(stats.getPlayerId()).getName();
                if (name == null) name = stats.getPlayerId().toString();
                int count = stats.getBlockCount(mat);
                if (count > 0) result.add(new AbstractMap.SimpleEntry<>(name, count));
            }
            result.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
            return result.size() > n ? result.subList(0, n) : result;
        });
    }
} 