package xyz.dapirates.command;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import xyz.dapirates.core.Core;
import xyz.dapirates.manager.PlayerStatsHandler;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class StatsTopGUI implements CommandExecutor {
    private final Core plugin;
    private final PlayerStatsHandler statsHandler;
    public static final List<String> ORES = Arrays.asList(
        "DIAMOND_ORE", "DEEPSLATE_DIAMOND_ORE",
        "IRON_ORE", "DEEPSLATE_IRON_ORE",
        "GOLD_ORE", "DEEPSLATE_GOLD_ORE",
        "COAL_ORE", "DEEPSLATE_COAL_ORE",
        "EMERALD_ORE", "DEEPSLATE_EMERALD_ORE",
        "LAPIS_ORE", "DEEPSLATE_LAPIS_ORE",
        "REDSTONE_ORE", "DEEPSLATE_REDSTONE_ORE",
        "COPPER_ORE", "DEEPSLATE_COPPER_ORE"
    );
    public static final List<String> MOBS = Arrays.asList(
        "ZOMBIE", "CREEPER", "SKELETON", "SPIDER", "WITCH"
    );

    public StatsTopGUI(Core plugin) {
        this.plugin = plugin;
        this.statsHandler = plugin.getPlayerStatsHandler();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        Player player = (Player) sender;
        openMainMenu(player);
        return true;
    }

    private void openMainMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 9, "Leaderboards");
        // Blocks
        ItemStack blocks = new ItemStack(Material.DIAMOND_PICKAXE);
        ItemMeta blocksMeta = blocks.getItemMeta();
        blocksMeta.setDisplayName("Blocks Broken");
        blocks.setItemMeta(blocksMeta);
        inv.setItem(2, blocks);
        // Ores
        ItemStack ores = new ItemStack(Material.DIAMOND_ORE);
        ItemMeta oresMeta = ores.getItemMeta();
        oresMeta.setDisplayName("Ores Broken");
        ores.setItemMeta(oresMeta);
        inv.setItem(4, ores);
        // Mobs
        ItemStack mobs = new ItemStack(Material.ZOMBIE_HEAD);
        ItemMeta mobsMeta = mobs.getItemMeta();
        mobsMeta.setDisplayName("Mob Kills");
        mobs.setItemMeta(mobsMeta);
        inv.setItem(6, mobs);
        player.openInventory(inv);
        // Listen for clicks (should be handled in a listener, but for brevity, you can add a listener to handle clicks and open sub-menus)
    }

    // Example: open blocks leaderboard
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

    // --- Added implementations ---
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
    public CompletableFuture<List<Map.Entry<String, Integer>>> getTopNMobKillsAsync(String mob, int n) {
        // Not implemented: No mob kill tracking in OreMiningStats/DatabaseManager
        return CompletableFuture.completedFuture(Collections.emptyList());
    }
}