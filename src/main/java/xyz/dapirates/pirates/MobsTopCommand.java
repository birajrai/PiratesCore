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

public class MobsTopCommand {
    private final Core plugin;
    public MobsTopCommand(Core plugin) {
        this.plugin = plugin;
    }
    public void openMobsTop(Player player, String mob) {
        getTopNMobKillsAsync(mob, 10).thenAccept(list -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                Inventory inv = Bukkit.createInventory(null, 18, "Top Mobs: " + mob);
                int slot = 0;
                for (var entry : list) {
                    ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                    SkullMeta skull = (SkullMeta) head.getItemMeta();
                    OfflinePlayer offline = Bukkit.getOfflinePlayer(entry.getKey());
                    skull.setOwningPlayer(offline);
                    skull.setDisplayName(entry.getKey());
                    skull.setLore(Collections.singletonList("Kills: " + entry.getValue()));
                    head.setItemMeta(skull);
                    inv.setItem(slot, head);
                    slot++;
                }
                player.openInventory(inv);
            });
        });
    }
    public CompletableFuture<List<Map.Entry<String, Integer>>> getTopNMobKillsAsync(String mob, int n) {
        // Not implemented: No mob kill tracking in OreMiningStats/DatabaseManager
        return CompletableFuture.completedFuture(Collections.emptyList());
    }
} 