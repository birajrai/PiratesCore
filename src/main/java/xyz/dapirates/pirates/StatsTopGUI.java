package xyz.dapirates.pirates;

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
import xyz.dapirates.core.Core;
import xyz.dapirates.manager.PlayerStatsHandler;
import java.util.*;

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
} 