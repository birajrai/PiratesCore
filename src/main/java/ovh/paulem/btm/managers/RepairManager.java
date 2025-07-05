package ovh.paulem.btm.managers;

import com.github.Anon8281.universalScheduler.UniversalRunnable;
import com.github.Anon8281.universalScheduler.UniversalScheduler;
import com.github.Anon8281.universalScheduler.scheduling.schedulers.TaskScheduler;
import ovh.paulem.btm.BetterMending;
import ovh.paulem.btm.versioned.damage.DamageHandler;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import ovh.paulem.btm.utils.ExperienceUtils;
import ovh.paulem.btm.utils.MathUtils;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class RepairManager {
    private final TaskScheduler scheduler;

    private final ParticleManager particleManager;

    public RepairManager(BetterMending plugin){
        this.scheduler = UniversalScheduler.getScheduler(plugin);

        this.particleManager = new ParticleManager();
    }

    public void initAutoRepair(){
        long delay = BetterMending.getConf().getLong("delay", 40L);

        scheduler.runTaskTimer(new UniversalRunnable() {
            @Override
            public void run() {
                for(Player player : Bukkit.getOnlinePlayers()){
                    if(!player.hasPermission("btm.use") || BetterMending.getConfigBlacklist().isBlacklisted(player)) continue;

                    List<ItemStack> damageables = Arrays.stream(player.getInventory().getContents())
                            .filter(i -> i != null &&
                                    i.getItemMeta() != null &&
                                    !BetterMending.getConfigBlacklist().isBlacklisted(i) &&
                                    i.getType() != Material.AIR &&
                                    i.containsEnchantment(Enchantment.MENDING) &&
                                    BetterMending.getDamageHandler().isDamageable(i) &&
                                    BetterMending.getDamageHandler().hasDamage(i)
                            ).collect(Collectors.toList());

                    if(!damageables.isEmpty()) {
                        if (BetterMending.getConf().getBoolean("repairFullInventory", true)) {
                            for (ItemStack item : damageables) {
                                if (item != null) {
                                    repairItem(player, item, false, false, true);
                                }
                            }

                        } else {
                            ItemStack item = damageables.get(ThreadLocalRandom.current().nextInt(damageables.size()));

                            if (item != null) {
                                repairItem(player, item, false, false, true);
                            }
                        }
                    }
                }
            }
        }, delay, delay);
    }

    public void repairItem(Player player, ItemStack item, boolean playSound, boolean playParticle, boolean isAutoRepair){
        int playerXP = ExperienceUtils.getPlayerXP(player);

        int itemDamages = BetterMending.getDamageHandler().getDamage(item);

        String expValueConfig = BetterMending.getConf().getString("expValue", "20");
        int expValue = (int) MathUtils.evaluate(expValueConfig.replace("%exp%", "x"), player);

        double ratio = item.getEnchantmentLevel(Enchantment.MENDING) * BetterMending.getConf().getDouble("ratio", 2.0);

        int autoRepairExpValue = BetterMending.getConf().getInt("auto-repair-config.expConsumed", 20);

        if (playerXP >= 30 && itemDamages >= expValue * ratio) {
            BetterMending.getDamageHandler().setDamage(item, DamageHandler.getDamageCalculation(itemDamages, expValue, ratio));
            if(isAutoRepair) ExperienceUtils.changePlayerExp(player, -autoRepairExpValue);
            else ExperienceUtils.changePlayerExp(player, -expValue);
        } else if (playerXP >= expValue/10) {
            BetterMending.getDamageHandler().setDamage(item, DamageHandler.getDamageCalculation(itemDamages, expValue, 10, ratio));
            if(isAutoRepair) ExperienceUtils.changePlayerExp(player, -autoRepairExpValue/10);
            else ExperienceUtils.changePlayerExp(player, -expValue/10);
        } else return;

        // Should play sound?
        if(playSound) {
            player.playSound(player.getLocation(),
                    Sound.BLOCK_ANVIL_PLACE,
                    (float) BetterMending.getConf().getDouble("soundVolume", 1),
                    (float) BetterMending.getConf().getDouble("soundPitch", 1));
        }

        // Should play particle?
        if(playParticle) {
            particleManager.summonCircle(player, BetterMending.getConf().getInt("range", 3));
        }
    }

    public boolean canRepairItem(Player player, ItemStack item){
        double ratio = item.getEnchantmentLevel(Enchantment.MENDING) * BetterMending.getConf().getDouble("ratio", 2.0);
        int playerXP = ExperienceUtils.getPlayerXP(player);

        int itemDamages = BetterMending.getDamageHandler().getDamage(item);

        String expValueConfig = BetterMending.getConf().getString("expValue", "20");
        int expValue = (int) MathUtils.evaluate(expValueConfig.replace("%exp%", "x"), player);

        return (playerXP >= 30 && itemDamages >= expValue * ratio) || (playerXP >= expValue / 10);
    }
}
