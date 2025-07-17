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

public class BlocksTopCommand {
    private final Core plugin;
    public BlocksTopCommand(Core plugin) {
        this.plugin = plugin;
    }
    public void openBlocksTop(Player player) {
        getTopNBlocksBrokenAsync(10).thenAccept(list -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                Inventory inv = Bukkit.createInventory(null, 18, "Top Blocks Broken");
                int slot = 0;
                for (var entry : list) {
                    ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                    SkullMeta skull = (SkullMeta) head.getItemMeta();
                    OfflinePlayer offline = Bukkit.getOfflinePlayer(entry.getKey());
                    skull.setOwningPlayer(offline);
                    skull.setDisplayName(entry.getKey());
                    skull.setLore(Collections.singletonList("Blocks: " + entry.getValue()));
                    head.setItemMeta(skull);
                    inv.setItem(slot, head);
                    slot++;
                }
                player.openInventory(inv);
            });
        });
    }
    public CompletableFuture<List<Map.Entry<String, Integer>>> getTopNBlocksBrokenAsync(int n) {
        return plugin.getDatabaseManager().getTopPlayersAsync(n).thenApply(statsList -> {
            List<Map.Entry<String, Integer>> result = new ArrayList<>();
            for (var stats : statsList) {
                String name = Bukkit.getOfflinePlayer(stats.getPlayerId()).getName();
                if (name == null) name = stats.getPlayerId().toString();
                result.add(new AbstractMap.SimpleEntry<>(name, stats.getTotalBlocks()));
            }
            result.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
            return result.size() > n ? result.subList(0, n) : result;
        });
    }
} 