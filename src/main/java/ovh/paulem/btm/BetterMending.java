package ovh.paulem.btm;

import org.bukkit.Bukkit;
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
import org.bukkit.plugin.java.JavaPlugin;

public class BetterMending extends JavaPlugin implements Listener {
    @Override
    public void onEnable() {
        saveDefaultConfig();
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("BetterMending enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("BetterMending disabled!");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onItemUse(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        if (!player.hasPermission("btm.use") ||
                player.getGameMode() == GameMode.SPECTATOR ||
                player.getGameMode() == GameMode.CREATIVE)
            return;

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
        if (!player.isSneaking() || e.getAction() != Action.RIGHT_CLICK_AIR)
            return;

        int expValue = getConfig().getInt("expValue", 20);
        int playerXP = getPlayerXP(player);
        int itemDamage = meta.getDamage();
        if (playerXP < expValue)
            return;

        int newDamage = Math.max(0, itemDamage - expValue);
        meta.setDamage(newDamage);
        item.setItemMeta(meta);
        changePlayerExp(player, -expValue);

        if (getConfig().getBoolean("playSound", true)) {
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1, 1);
        }

        e.setCancelled(true);
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
