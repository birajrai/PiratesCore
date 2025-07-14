package xyz.dapirates.listener;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

public class BetterMending implements Listener {
    @EventHandler(priority = EventPriority.HIGH)
    public void onItemUse(final PlayerInteractEvent e) {
        final Player player = e.getPlayer();
        if (!player.hasPermission("pc.bettermending.use") ||
                player.getGameMode() == GameMode.SPECTATOR ||
                player.getGameMode() == GameMode.CREATIVE)
            return;

        if (!player.isSneaking())
            return;

        // Right click for main hand items
        if (e.getAction() == Action.RIGHT_CLICK_AIR) {
            ItemStack item = player.getInventory().getItemInMainHand();
            if (item == null || item.getType() == Material.AIR)
                return;
            if (!item.containsEnchantment(Enchantment.MENDING))
                return;
            if (!(item.getItemMeta() instanceof Damageable))
                return;
            Damageable meta = (Damageable) item.getItemMeta();
            if (!meta.hasDamage())
                return;

            // Calculate how much can be repaired: 1 XP = 2 durability
            int itemDamage = meta.getDamage();
            int playerXP = getPlayerXP(player);
            int maxRepairable = Math.min(itemDamage, playerXP * 2);
            if (maxRepairable <= 0)
                return;
            int xpToUse = (maxRepairable + 1) / 2; // round up if odd
            int newDamage = itemDamage - maxRepairable;
            meta.setDamage(newDamage);
            item.setItemMeta(meta);
            changePlayerExp(player, -xpToUse);

            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1, 1);
            e.setCancelled(true);
        }
    }

    // Utility: get total player XP
    private int getPlayerXP(Player player) {
        int level = player.getLevel();
        float exp = player.getExp();
        int xp = getExperienceForLevel(level) + Math.round(exp * player.getExpToLevel());
        return xp;
    }

    // Utility: set player XP
    private void changePlayerExp(Player player, int amount) {
        int current = getPlayerXP(player);
        int newXP = Math.max(0, current + amount);
        player.setExp(0);
        player.setLevel(0);
        player.giveExp(newXP);
    }

    // Utility: XP required for a level
    private int getExperienceForLevel(int level) {
        if (level == 0)
            return 0;
        if (level > 0 && level < 16)
            return level * level + 6 * level;
        else if (level < 32)
            return (int) (2.5 * level * level - 40.5 * level + 360);
        else
            return (int) (4.5 * level * level - 162.5 * level + 2220);
    }
}