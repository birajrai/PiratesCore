package xyz.dapirates.manager;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.dapirates.command.OreMiningCommand;
import xyz.dapirates.command.ShowCommand;
import xyz.dapirates.listener.OreMiningListener;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class CommandManager {

    private final JavaPlugin plugin;
    private final Map<String, CommandExecutor> commands;
    private final Map<String, TabCompleter> tabCompleters;

    public CommandManager(final JavaPlugin plugin) {
        this.plugin = plugin;
        this.commands = new HashMap<>();
        this.tabCompleters = new HashMap<>();
    }

    public void registerCommands(final OreMiningListener oreMiningListener) {
        // Register commands using a more abstract approach
        registerCommand("show", ShowCommand::new);
        
        // Register ore mining commands
        OreMiningCommand oreMiningCommand = new OreMiningCommand((xyz.dapirates.core.Core) plugin, oreMiningListener);
        registerCommand("oremining", oreMiningCommand);
        registerTabCompleter("oremining", oreMiningCommand);

        // Register pirates command
        xyz.dapirates.command.PiratesCommand piratesCommand = new xyz.dapirates.command.PiratesCommand((xyz.dapirates.core.Core) plugin);
        registerCommand("pirates", piratesCommand);
        registerTabCompleter("pirates", piratesCommand);
    }

    private void registerCommand(final String name, final CommandExecutor executor) {
        commands.put(name, executor);
        plugin.getCommand(name).setExecutor(executor);
    }

    private void registerCommand(final String name, final Supplier<CommandExecutor> executorSupplier) {
        CommandExecutor executor = executorSupplier.get();
        registerCommand(name, executor);
    }

    private void registerTabCompleter(final String name, final TabCompleter completer) {
        tabCompleters.put(name, completer);
        plugin.getCommand(name).setTabCompleter(completer);
    }

    public CommandExecutor getCommand(final String name) {
        return commands.get(name);
    }

    public TabCompleter getTabCompleter(final String name) {
        return tabCompleters.get(name);
    }

    public void unregisterAll() {
        commands.clear();
        tabCompleters.clear();
    }
}