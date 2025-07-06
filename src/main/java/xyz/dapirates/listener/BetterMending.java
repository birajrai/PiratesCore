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
    public void onItemUse(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        if (!player.hasPermission("btm.use") ||
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

            int expValue = 20; // Default value, config not available here
            int playerXP = getPlayerXP(player);
            int itemDamage = meta.getDamage();
            if (playerXP < expValue)
                return;

            int newDamage = Math.max(0, itemDamage - expValue);
            meta.setDamage(newDamage);
            item.setItemMeta(meta);
            changePlayerExp(player, -expValue);

            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1, 1);
            e.setCancelled(true);
        }
        // Left click for armor/elytra
        else if (e.getAction() == Action.LEFT_CLICK_AIR) {
            ItemStack[] armorContents = player.getInventory().getArmorContents();
            for (int i = 0; i < armorContents.length; i++) {
                ItemStack armorItem = armorContents[i];
                if (armorItem != null && armorItem.getType() != Material.AIR) {
                    if (!armorItem.containsEnchantment(Enchantment.MENDING))
                        continue;
                    if (!(armorItem.getItemMeta() instanceof Damageable))
                        continue;

                    Damageable meta = (Damageable) armorItem.getItemMeta();
                    if (!meta.hasDamage())
                        continue;

                    int expValue = 20; // Default value, config not available here
                    int playerXP = getPlayerXP(player);
                    int itemDamage = meta.getDamage();
                    if (playerXP < expValue)
                        continue;

                    int newDamage = Math.max(0, itemDamage - expValue);
                    meta.setDamage(newDamage);
                    armorItem.setItemMeta(meta);
                    changePlayerExp(player, -expValue);

                    // Update the armor slot with the repaired item
                    armorContents[i] = armorItem;
                    player.getInventory().setArmorContents(armorContents);

                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1, 1);
                    e.setCancelled(true);
                    return;
                }
            }
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