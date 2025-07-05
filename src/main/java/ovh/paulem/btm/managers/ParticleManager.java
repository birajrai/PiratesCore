package ovh.paulem.btm.managers;

import com.github.fierioziy.particlenativeapi.api.ParticleNativeAPI;
import com.github.fierioziy.particlenativeapi.api.utils.ParticleException;
import com.github.fierioziy.particlenativeapi.core.ParticleNativeCore;
import org.bukkit.Color;
import org.bukkit.Particle;
import ovh.paulem.btm.BetterMending;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import ovh.paulem.btm.utils.ReflectionUtils;
import ovh.paulem.btm.versioned.Versioning;

public class ParticleManager {
    private ParticleNativeAPI api;

    public ParticleManager(){
        try {
            this.api = ParticleNativeCore.loadAPI(BetterMending.getInstance());
        } catch (ParticleException e) {// optional runtime exception catch
            this.api = null;
        }
    }

    public void summonCircle(Player player, int size) {
        Location location = player.getLocation()
                .add(
                        BetterMending.getConf().getDouble("offset.x", 0),
                        BetterMending.getConf().getDouble("offset.y", 0),
                        BetterMending.getConf().getDouble("offset.z", 0)
                );

        if(location.getWorld() == null) return;

        Location particleLoc = new Location(location.getWorld(), location.getX(), location.getY(), location.getZ());
        for (int d = 0; d <= 90; d += 1) {
            particleLoc.setX(location.getX() + Math.cos(d) * size);
            particleLoc.setZ(location.getZ() + Math.sin(d) * size);

            Color particleColor = Color.fromRGB(
                    checkRGB(BetterMending.getConf().getInt("color.red", 144), 144),
                    checkRGB(BetterMending.getConf().getInt("color.green", 238), 238),
                    checkRGB(BetterMending.getConf().getInt("color.blue", 144), 144)
            );

            if(Versioning.isIn13()) {
                player.spawnParticle(ReflectionUtils.getValueFromEnum(Particle.class, "REDSTONE"),
                        particleLoc, 0, 0, 0, 0, 1, new Particle.DustOptions(particleColor, 1));
            } else {
                api.LIST_1_8.REDSTONE
                        .packetColored(false, particleLoc, particleColor)
                        .sendTo(player);
            }
        }
    }

    private int checkRGB(int color, int defaultColor){
        if(color < 0 || color > 255) return defaultColor;
        else return color;
    }
}