package xyz.dapirates.managers;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.dapirates.command.OreMiningCommand;
import xyz.dapirates.command.ShowCommand;
import xyz.dapirates.features.OreMiningNotifier;

import java.util.HashMap;
import java.util.Map;

public class CommandManager {
    
    private final JavaPlugin plugin;
    private final Map<String, CommandExecutor> commands;
    private final Map<String, TabCompleter> tabCompleters;
    
    public CommandManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.commands = new HashMap<>();
        this.tabCompleters = new HashMap<>();
    }
    
    public void registerCommands(OreMiningNotifier oreMiningNotifier) {
        // Register existing commands
        registerCommand("show", new ShowCommand());
        
        // Register ore mining commands
        OreMiningCommand oreMiningCommand = new OreMiningCommand((xyz.dapirates.Core) plugin, oreMiningNotifier);
        registerCommand("oremining", oreMiningCommand);
        registerTabCompleter("oremining", oreMiningCommand);
    }
    
    private void registerCommand(String name, CommandExecutor executor) {
        commands.put(name, executor);
        plugin.getCommand(name).setExecutor(executor);
    }
    
    private void registerTabCompleter(String name, TabCompleter completer) {
        tabCompleters.put(name, completer);
        plugin.getCommand(name).setTabCompleter(completer);
    }
    
    public CommandExecutor getCommand(String name) {
        return commands.get(name);
    }
    
    public TabCompleter getTabCompleter(String name) {
        return tabCompleters.get(name);
    }
    
    public void unregisterAll() {
        commands.clear();
        tabCompleters.clear();
    }
} 