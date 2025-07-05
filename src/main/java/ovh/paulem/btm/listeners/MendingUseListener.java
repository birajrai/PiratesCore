package ovh.paulem.btm.listeners;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import ovh.paulem.btm.BetterMending;
import ovh.paulem.btm.utils.ExperienceUtils;
import ovh.paulem.btm.versioned.damage.DamageHandler;
import org.bukkit.event.player.PlayerInteractEvent;

public class MendingUseListener implements Listener {
    @EventHandler(priority = EventPriority.HIGH)
    public void onItemUse(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        if (!player.hasPermission("btm.use") ||
                player.getGameMode() == GameMode.SPECTATOR ||
                player.getGameMode() == GameMode.CREATIVE) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) return;
        if (!item.containsEnchantment(Enchantment.MENDING)) return;
        if (!BetterMending.getDamageHandler().isDamageable(item)) return;
        if (!BetterMending.getDamageHandler().hasDamage(item)) return;
        if (!player.isSneaking() || e.getAction() != Action.RIGHT_CLICK_AIR) return;

        int expValue = BetterMending.getConf().getInt("expValue", 20);
        int playerXP = ExperienceUtils.getPlayerXP(player);
        int itemDamage = BetterMending.getDamageHandler().getDamage(item);
        if (playerXP < expValue) return;

        // Repair the item by reducing its damage by expValue, but not below 0
        int newDamage = Math.max(0, itemDamage - expValue);
        BetterMending.getDamageHandler().setDamage(item, newDamage);
        ExperienceUtils.changePlayerExp(player, -expValue);

        if (BetterMending.getConf().getBoolean("playSound", true)) {
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1, 1);
        }

        e.setCancelled(true);
    }
}
