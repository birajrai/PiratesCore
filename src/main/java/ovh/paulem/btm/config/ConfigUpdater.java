package ovh.paulem.btm.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import com.google.common.base.Charsets;
import ovh.paulem.btm.BetterMending;
import ovh.paulem.btm.utils.PluginUtils;

public class ConfigUpdater {
    private final boolean instantDeprecated = false; // Just an option to depecrate the config if the version is different
    private final int newVersion; // The next version of the Config
    private final BetterMending plugin;

    private File path;
    private List<String> lines;

    public ConfigUpdater(BetterMending plugin) {
        this.plugin = plugin;

        FileConfiguration embeddedConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(plugin.getResource("config.yml")));
        this.newVersion = embeddedConfig.getInt("version", 0);
    }

    public void checkUpdate(int oldV) {
        path = new File(plugin.getDataFolder(), "config.yml");

        if (instantDeprecated) {
            if (oldV != newVersion)
                deprecateConfig();
            return;
        }

        if (oldV == newVersion) {
            plugin.getLogger().info("The config is updated!");
            return;
        }

        lines = readFile(path);
        updateConfig();
    }

    public void updateConfig() {
        plugin.getLogger().info("Updating your config...");

        List<String> newLines = readInsideFile("/config.yml");

        lines.removeIf(s -> s.trim().isEmpty() || s.trim().startsWith("#") || s.split(":").length == 1);
        lines.forEach(s -> {
            String[] a = s.split(":");
            String newS = joinString(Arrays.copyOfRange(a, 1, a.length), ":");
            int index = getIndex(a[0], newLines);
            if (index > -1)
                newLines.set(index, newLines.get(index).split(":")[0] + ":" + newS);
        });

        String versionLine = "version: ";
        newLines.set(getIndex(versionLine, newLines), versionLine + newVersion);
        writeFile(path, newLines);
        plugin.getLogger().info("Your configuration has been updated! You can find more informations about new option on the plugin resource page!");

        PluginUtils.reloadConfig();
    }

    private void deprecateConfig() {
        plugin.getLogger().info("Now your config is deprecated please check your folder for re-setting it!");
        String depName = "deprecated_config_" + LocalDate.now();
        File old = new File(path.getParentFile(), depName + ".yml");
        try {
            Files.copy(path.toPath(), old.toPath(),
                    StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
            path.delete();
        } catch (Exception ignored) {}

        //Here we re-create the config.
    }

    public String joinString(String[] text, String character) {
        return String.join(character, text);
    }

    public int getIndex(String line, List<String> list) {
        for (String s : list)
            if (s.startsWith(line) || s.equalsIgnoreCase(line))
                return list.indexOf(s);
        return -1;
    }

    public void writeFile(File file, List<String> toWrite) {
        try {
            Files.write(file.toPath(), toWrite, Charsets.ISO_8859_1);
        } catch (Exception ignored) {}
    }

    public List<String> readFile(File file) {
        try {
            return Files.readAllLines(file.toPath(), Charsets.ISO_8859_1);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public List<String> readInsideFile(String path) {
        try (InputStream in = plugin.getClass().getResourceAsStream(path);
             BufferedReader input = new BufferedReader(new InputStreamReader(in));) {
            return input.lines().collect(Collectors.toList());
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

}