package ovh.paulem.btm.versioned.damage;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

public class DamageNewer implements DamageHandler {
    @Override
    public boolean hasDamage(ItemStack item) {
        return ((Damageable) item.getItemMeta()).hasDamage();
    }

    @Override
    public int getDamage(ItemStack item) {
        return ((Damageable) item.getItemMeta()).getDamage();
    }

    @Override
    public void setDamage(ItemStack item, int damage) {
        Damageable damageable = (Damageable) item.getItemMeta();
        if(damageable == null) return;
        if(getDamage(item) == 1) damageable.setDamage(0);
        else damageable.setDamage(damage);
        item.setItemMeta(damageable);
    }

    @Override
    public boolean isDamageable(ItemStack item) {
        return item.getItemMeta() instanceof Damageable;
    }
}
