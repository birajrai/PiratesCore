package xyz.dapirates.data;

import org.bukkit.Material;
import org.bukkit.Sound;

import java.util.List;

public class OreMiningData {
    private final Material material;
    private final String customMessage;
    private final Sound customSound;
    private final List<String> customCommands;
    private final boolean showCoordinates;
    private final boolean enabled;

    public OreMiningData(Material material, String customMessage, Sound customSound,
            List<String> customCommands, boolean showCoordinates, boolean enabled) {
        this.material = material;
        this.customMessage = customMessage;
        this.customSound = customSound;
        this.customCommands = customCommands;
        this.showCoordinates = showCoordinates;
        this.enabled = enabled;
    }

    public Material getMaterial() {
        return material;
    }

    public String getCustomMessage() {
        return customMessage;
    }

    public Sound getCustomSound() {
        return customSound;
    }

    public List<String> getCustomCommands() {
        return customCommands;
    }

    public boolean isShowCoordinates() {
        return showCoordinates;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public static class Builder {
        private Material material;
        private String customMessage = "§a[OreMining] §f{player} found {block}!";
        private Sound customSound = Sound.BLOCK_NOTE_BLOCK_PLING;
        private List<String> customCommands = List.of();
        private boolean showCoordinates = true;
        private boolean enabled = true;

        public Builder(Material material) {
            this.material = material;
        }

        public Builder customMessage(String message) {
            this.customMessage = message;
            return this;
        }

        public Builder customSound(Sound sound) {
            this.customSound = sound;
            return this;
        }

        public Builder customCommands(List<String> commands) {
            this.customCommands = commands;
            return this;
        }

        public Builder showCoordinates(boolean show) {
            this.showCoordinates = show;
            return this;
        }

        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public OreMiningData build() {
            return new OreMiningData(material, customMessage, customSound,
                    customCommands, showCoordinates, enabled);
        }
    }
}