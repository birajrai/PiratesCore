package ovh.paulem.btm.listeners;

import ovh.paulem.btm.BetterMending;
import ovh.paulem.btm.listeners.extendables.ManagersListener;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class PreventDestroyListener extends ManagersListener {

    @EventHandler(priority = EventPriority.HIGH)
    public void onPreventDestroyBlock(BlockBreakEvent e){
        Player player = e.getPlayer();

        if(isPreventNeeded(player)) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPreventDestroyAttack(EntityDamageByEntityEvent e){
        if(!(e.getDamager() instanceof Player)) return;
        Player player = (Player) e.getDamager();

        if(isPreventNeeded(player)) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPreventDestroyPathOrFarmland(PlayerInteractEvent e){
        Player player = e.getPlayer();

        if((e.getAction() == Action.LEFT_CLICK_BLOCK || e.getAction() == Action.RIGHT_CLICK_BLOCK) &&
                isPreventNeeded(player)) {
            e.setCancelled(true);
        }
    }

    public boolean isPreventNeeded(Player player){
        if(!BetterMending.getConf().getBoolean("prevent-destroy", false)) return false;

        if(!player.hasPermission("btm.use")) return false;

        ItemStack item = player.getInventory().getItemInMainHand();

        if(item.getType() == Material.AIR) return false;

        if(!damageHandler.isDamageable(item)) return false;

        // Continue if item has Mending, and he's not right-clicking in air
        if(!item.containsEnchantment(Enchantment.MENDING)) return false;

        if(!repairManager.canRepairItem(player, item)) return false;

        return damageHandler.getDamage(item) == item.getType().getMaxDurability()-1;
    }
}
