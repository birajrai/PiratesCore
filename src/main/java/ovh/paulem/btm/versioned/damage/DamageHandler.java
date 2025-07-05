package ovh.paulem.btm.versioned.damage;

import org.bukkit.inventory.ItemStack;

public interface DamageHandler {
    boolean hasDamage(ItemStack item);

    int getDamage(ItemStack item);

    void setDamage(ItemStack item, int damage);

    /**
     * May return true if isn't damaged
     */
    boolean isDamageable(ItemStack item);

    static int getDamageCalculation(int itemDamages, int expValue, double ratio) {
        return getDamageCalculation(itemDamages, expValue, 1, ratio);
    }

    static int getDamageCalculation(int itemDamages, int expValue, int xpDivisor, double ratio) {
        int value = (int) ((double) expValue / xpDivisor * ratio);
        int constrained = Math.min(Math.max(value, 0), itemDamages);
        return itemDamages - constrained;
    }
}
