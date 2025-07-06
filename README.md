PiratesAddons Documentation
Overview
PiratesAddons is a Minecraft plugin (likely for Spigot/Paper) that provides advanced ore mining notifications, chat filtering, and quality-of-life features for server administrators and players. The plugin is modular, with a focus on extensibility and integration with external services (e.g., Discord webhooks).
Main Components
1. Core Plugin (Core.java)
Entry point for the plugin, extends JavaPlugin.
Initializes and manages all major subsystems (managers, listeners, configs).
Handles plugin enable/disable lifecycle, including flushing mining sessions to the database on shutdown.
2. Commands (command/)
PiratesCommand: /pirates reload command for reloading all configs and chat filter.
ShowCommand: /show command for players to broadcast the item they're holding, with LuckPerms prefix and item lore support.
OreMiningCommand: /oremining command with subcommands for toggling notifications, viewing stats, top miners, and managing ignore lists.
3. Listeners (listener/)
OreMiningListener: Tracks ore mining events, manages mining sessions, sends notifications, and interacts with the database and webhooks.
ChatFilterListener: Filters chat messages for bad words using a configurable YAML list.
BetterMending: Allows players to repair items with the Mending enchantment using XP and a sneak-right-click action.
4. Managers (manager/)
ConfigManager: Centralized config reload logic for all subsystems.
FeatureManager: Registers and manages plugin features (e.g., BetterMending, OreMining) based on config.
DatabaseManager: Handles all database operations (H2), including player stats, block counts, and mining history.
MessageManager: Formats and sends messages to players and the console, with support for PlaceholderAPI and JSON messages.
WebhookManager: Sends notifications to Discord (or other services) via webhooks, supports both simple and embed messages.
OreMiningWebhook: Specialized for sending ore mining notifications and session summaries to webhooks.
5. Services (service/)
MiningSession: Represents a player's current mining session, tracks blocks mined and session timing.
OreMiningStats: Stores persistent mining statistics and history for a player.
OreMiningData: Configuration data for each ore type (custom messages, sounds, commands, etc.).
6. Utilities (utils/)
OreMiningConfig: Loads and manages the oremining.yml config, including tracked blocks, alert thresholds, and message formats.
Configuration Files
oremining.yml: Main configuration for ore mining features, tracked blocks, alert settings, and more.
settings.yml: Feature toggles and general plugin settings.
webhook.yml: Webhook URLs and settings for Discord integration.
BadWords.yml: List of words to filter from chat.
Feature Highlights
Ore Mining Notifications: Real-time and batched notifications for rare ore mining, with Discord webhook support.
Mining Statistics: Tracks and displays player mining stats, top miners, and session summaries.
Chat Filtering: Replaces bad words in chat based on a configurable list.
Better Mending: Allows players to repair items using XP with a simple action.
Config Reloading: All configs can be reloaded in-game without restarting the server.
Extensible: Easily add new features or listeners via the FeatureManager.
Extending the Plugin
Add a new feature: Implement a Listener, register it in FeatureManager, and add a toggle in settings.yml.
Add a new command: Implement CommandExecutor, register in CommandManager, and add to plugin.yml.
Integrate with new webhooks: Add the webhook URL to webhook.yml and use WebhookManager to send messages.
Developer Notes
Database: Uses H2 embedded database for storing mining stats.
Async Operations: Database and webhook operations are performed asynchronously to avoid blocking the main server thread.
PlaceholderAPI: Optional support for advanced message placeholders.
LuckPerms: Used for player prefix display in chat/item broadcasts.
File Structure
Apply to piratesaddon...
