package ovh.paulem.btm.versioned.sounds;

import org.bukkit.Sound;
import org.jetbrains.annotations.Nullable;
import ovh.paulem.btm.versioned.Versioning;

public interface SoundsHandler {
    @Nullable Sound getEndermanTeleportSound();

    static SoundsHandler getSoundHandler() {
        return Versioning.isLegacy() ? new SoundsLegacy() : new SoundsNewer();
    }
}
