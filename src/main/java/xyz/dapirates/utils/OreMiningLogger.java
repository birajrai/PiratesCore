package xyz.dapirates.utils;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import xyz.dapirates.Core;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;

public class OreMiningLogger {
    
    private final Core plugin;
    private final File logFile;
    private final SimpleDateFormat dateFormat;
    
    public OreMiningLogger(Core plugin) {
        this.plugin = plugin;
        this.logFile = new File(plugin.getDataFolder(), "oremining.log");
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        
        // Create log file if it doesn't exist
        if (!logFile.exists()) {
            try {
                logFile.getParentFile().mkdirs();
                logFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create oremining.log file", e);
            }
        }
    }
    
    public void logMiningActivity(Player player, Material material, Location location) {
        logMiningActivity(player, material, location, false);
    }
    
    public void logMiningActivity(Player player, Material material, Location location, boolean isTNT) {
        String timestamp = dateFormat.format(new Date());
        String playerName = player.getName();
        String blockName = material.name();
        String worldName = location.getWorld().getName();
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        String tntFlag = isTNT ? " (TNT)" : "";
        
        String logEntry = String.format("[%s] %s mined %s at %s (%d, %d, %d)%s",
            timestamp, playerName, blockName, worldName, x, y, z, tntFlag);
        
        writeToLog(logEntry);
    }
    
    public void logCommandExecution(Player player, String command) {
        String timestamp = dateFormat.format(new Date());
        String logEntry = String.format("[%s] %s executed command: %s",
            timestamp, player.getName(), command);
        
        writeToLog(logEntry);
    }
    
    public void logConfigurationChange(String change) {
        String timestamp = dateFormat.format(new Date());
        String logEntry = String.format("[%s] Configuration changed: %s",
            timestamp, change);
        
        writeToLog(logEntry);
    }
    
    public void logError(String error) {
        String timestamp = dateFormat.format(new Date());
        String logEntry = String.format("[%s] ERROR: %s",
            timestamp, error);
        
        writeToLog(logEntry);
        plugin.getLogger().warning("OreMining Error: " + error);
    }
    
    private void writeToLog(String message) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(logFile, true))) {
            writer.println(message);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not write to oremining.log", e);
        }
    }
    
    public void clearLog() {
        try (PrintWriter writer = new PrintWriter(logFile)) {
            writer.print(""); // Clear the file
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not clear oremining.log", e);
        }
    }
    
    public File getLogFile() {
        return logFile;
    }
} 