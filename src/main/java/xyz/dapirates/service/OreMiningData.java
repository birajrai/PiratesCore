package xyz.dapirates.service;

import org.bukkit.Material;
import org.bukkit.Sound;

import java.util.ArrayList;
import java.util.List;

public class OreMiningData {
    private final String message;
    private final String sound;
    private final boolean showCoordinates;
    private final List<String> commands;

    public OreMiningData(final String message, final String sound, final boolean showCoordinates, final List<String> commands) {
        this.message = message;
        this.sound = sound;
        this.showCoordinates = showCoordinates;
        this.commands = new ArrayList<>(commands);
    }

    public String getMessage() {
        return message;
    }

    public String getSound() {
        return sound;
    }

    public boolean isShowCoordinates() {
        return showCoordinates;
    }

    public List<String> getCommands() {
        return new ArrayList<>(commands);
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
            return new OreMiningData(customMessage, customSound.toString(), showCoordinates, customCommands);
        }
    }
}