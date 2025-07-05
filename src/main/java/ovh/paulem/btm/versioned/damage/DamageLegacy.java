package ovh.paulem.btm.versioned.damage;

import org.bukkit.inventory.ItemStack;

public class DamageLegacy implements DamageHandler {
    @Override
    public boolean hasDamage(ItemStack item) {
        return (item.getType().getMaxDurability() - item.getDurability()) < item.getType().getMaxDurability();
    }

    @Override
    public int getDamage(ItemStack item) {
        return item.getDurability();
    }

    @Override
    public void setDamage(ItemStack item, int damage) {
        item.setDurability((short) damage);
    }

    @Override
    public boolean isDamageable(ItemStack item) {
        return item.getDurability() != 0;
    }
}
