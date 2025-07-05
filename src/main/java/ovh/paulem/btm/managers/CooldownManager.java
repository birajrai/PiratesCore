package ovh.paulem.btm.managers;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

public class CooldownManager {
    private final Map<UUID, Instant> map = new HashMap<>();
    private final int defaultCooldown;

    public CooldownManager(int defaultCooldown){
        this.defaultCooldown = defaultCooldown;
    }

    // Set cooldown
    public void setCooldown(UUID key, Duration duration) {
        map.put(key, Instant.now().plus(duration));
    }

    // Check if cooldown has expired
    public boolean hasCooldown(UUID key) {
        Instant now = Instant.now();
        Instant cooldown = map.getOrDefault(key, now);
        return now.isBefore(cooldown);
    }

    // Remove cooldown
    public Instant removeCooldown(UUID key) {
        return map.remove(key);
    }

    // Get remaining cooldown time
    public Duration getRemainingCooldown(UUID key) {
        Instant now = Instant.now();
        Instant cooldown = map.getOrDefault(key, now);

        return now.isBefore(cooldown) ? Duration.between(now, cooldown) : Duration.ZERO;
    }

    public int getDefaultCooldown() {
        return defaultCooldown;
    }
}