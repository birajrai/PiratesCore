package ovh.paulem.btm.utils;

import org.bukkit.entity.Player;

public class ExperienceUtils {
    public static void changePlayerExp(Player player, int exp){
        int currentExp = getPlayerXP(player);

        player.setExp(0);
        player.setLevel(0);

        player.giveExp(currentExp + exp);
    }

    public static int getPlayerXP(Player player) {
        return (int) (getExperienceForLevel(player.getLevel()) + (player.getExp() * player.getExpToLevel()));
    }

    public static int getExperienceForLevel(int level) {
        if (level == 0) return 0;
        if (level > 0 && level < 16) return (int) (Math.pow(level, 2) + 6 * level);
        else if (level > 15 && level < 32) return (int) (2.5 * Math.pow(level, 2) - 40.5 * level + 360);
        else return (int) (4.5 * Math.pow(level, 2) - 162.5 * level + 2220);
    }
}
