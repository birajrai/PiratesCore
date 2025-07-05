package ovh.paulem.btm.config;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import ovh.paulem.btm.BetterMending;

import java.util.List;
import java.util.stream.Collectors;

public class ConfigBlacklist {
    private final List<String> blacklistedPlayers;
    private final List<String> blacklistedItems;

    public ConfigBlacklist() {
        blacklistedPlayers = BetterMending.getConf().getStringList("blacklisted-players")
                .stream()
                .map(String::toLowerCase)
                .collect(Collectors.toList());

        blacklistedItems = BetterMending.getConf().getStringList("blacklisted-items");
    }

    public boolean isBlacklisted(Player player) {
        return blacklistedPlayers.contains(player.getName().toLowerCase());
    }

    public boolean isBlacklisted(ItemStack stack) {
        return blacklistedItems
                .stream()
                .anyMatch(mat -> mat.equalsIgnoreCase(stack.getType().name()));
    }

    public List<String> getBlacklistedPlayers() {
        return blacklistedPlayers;
    }

    public List<String> getBlacklistedItems() {
        return blacklistedItems;
    }
}
