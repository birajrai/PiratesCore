# PiratesAddons

A Minecraft plugin that provides quality of life improvements including BetterMending and comprehensive ore mining notifications.

## Features

### BetterMending
- Quality of life improvements to the Mending enchantment

### Ore Mining Notifications
A comprehensive system that tracks and notifies players when valuable ores are mined.

#### Core Features
- **Real-time notifications** when players mine valuable ores
- **Customizable messages** for each block type
- **Custom sounds** for different ores
- **Coordinate tracking** (can be toggled on/off)
- **TNT mining detection** and attribution
- **Time-based alerts** for excessive mining
- **Player statistics** and leaderboards
- **Whitelist system** for notification recipients
- **Comprehensive logging** of all mining activities

#### Supported Blocks
By default, the system tracks:
- **Diamond Ores** (Diamond Ore, Deepslate Diamond Ore)
- **Emerald Ores** (Emerald Ore, Deepslate Emerald Ore)
- **Gold Ores** (Gold Ore, Deepslate Gold Ore)
- **Iron Ores** (Iron Ore, Deepslate Iron Ore)
- **Copper Ores** (Copper Ore, Deepslate Copper Ore)
- **Coal Ores** (Coal Ore, Deepslate Coal Ore)
- **Redstone Ores** (Redstone Ore, Deepslate Redstone Ore)
- **Lapis Ores** (Lapis Ore, Deepslate Lapis Ore)
- **Nether Ores** (Ancient Debris, Nether Gold Ore, Nether Quartz Ore)

#### Advanced Features
- **Height control system** - Only track blocks within specified Y coordinates
- **Light level control** - Only track blocks within specified light levels
- **Custom command execution** - Run commands when specific blocks are mined
- **Delayed notifications** - Batch mining notifications to reduce spam
- **JSON message support** - Rich text with clickable coordinates and hover effects
- **Asynchronous processing** - Database operations and notifications run async to prevent server lag
- **H2 database storage** - Persistent data storage with caching for performance
- **PlaceholderAPI support** - Use any PlaceholderAPI placeholders in messages
- **Player toggle system** - Players can disable notifications for themselves
- **Admin controls** - Comprehensive admin commands for management

## Installation

1. Download the latest release JAR file
2. Place it in your server's `plugins` folder
3. Restart your server
4. Configure the plugin using the generated `oremining.yml` file

## Configuration

The plugin generates an `oremining.yml` configuration file with the following sections:

### General Settings
```yaml
general:
  enabled: true                    # Enable/disable the entire system
  console-notifications: true      # Send notifications to console
  self-notifications: true         # Allow self-notifications
  whitelist-enabled: false         # Enable whitelist system
  tnt-mining-enabled: true         # Enable TNT mining detection
  message-format: "text"           # Message format (text/json)
  
  # Delayed notification settings
  delayed-notifications:
    enabled: true                  # Enable delayed notifications
    delay-seconds: 30              # Wait time before sending notification
    batch-messages: true           # Combine multiple blocks into one message
```

### Height and Light Controls
```yaml
controls:
  min-height: -64                  # Minimum Y coordinate
  max-height: 320                  # Maximum Y coordinate
  min-light-level: 0               # Minimum light level
  max-light-level: 15              # Maximum light level
```

### Time-based Alerts
```yaml
alerts:
  time-based:
    enabled: true                  # Enable time-based alerts
    timeframe-minutes: 5           # Timeframe for checking
    threshold: 10                  # Block count threshold
    message: "Alert message format"
```

### Block Configuration
Each block can be configured individually:
```yaml
blocks:
  diamond_ore:
    enabled: true
    message: "§b[OreMining] §f{player} found §bDiamond Ore§f!"
    sound: "ENTITY_PLAYER_LEVELUP"
    show-coordinates: true
    commands: []                   # Custom commands to execute
```

## Commands

### Player Commands
- `/oremining toggle` - Toggle notifications for yourself
- `/oremining stats` - Show your mining statistics
- `/oremining top [amount]` - Show top miners

### Admin Commands
- `/oremining reload` - Reload configuration
- `/oremining stats [player]` - Show player statistics
- `/oremining whitelist add/remove <player>` - Manage whitelist
- `/oremining clear [player]` - Clear statistics
- `/oremining logs [clear]` - View or clear logs

## Permissions

- `pc.ores.notify` - Receive ore mining notifications
- `pc.ores.notify.admin` - Access admin commands (includes `pc.ores.notify`)

## Placeholders

The following placeholders can be used in messages and commands:
- `{player}` - Player name
- `{block}` - Block name
- `{x}`, `{y}`, `{z}` - Coordinates
- `{world}` - World name

### PlaceholderAPI Support

If PlaceholderAPI is installed, you can use any PlaceholderAPI placeholders in your messages:
- `%player_name%` - Player name
- `%player_health%` - Player health
- `%player_level%` - Player level
- And many more...

## Custom Commands

You can configure custom commands to execute when specific blocks are mined:

```yaml
diamond_ore:
  enabled: true
  message: "§b[OreMining] §f{player} found §bDiamond Ore§f!"
  sound: "ENTITY_PLAYER_LEVELUP"
  show-coordinates: true
  commands:
    - "broadcast {player} found diamonds!"
    - "give {player} diamond 1"
    - "effect give {player} minecraft:glowing 30 1"
```

## Logging

The plugin logs all mining activities to `plugins/PiratesAddons/oremining.log` with timestamps and detailed information.

## Database

The plugin uses H2 database for persistent storage of:
- Player statistics
- Block counts
- Mining history
- All data is cached in memory for fast access

Database files are stored in `plugins/PiratesAddons/oremining.mv.db`

**Note:** The H2 database driver is bundled with the plugin, so no additional setup is required. If the H2 database is not available for any reason, the plugin will fall back to in-memory storage. Data will be lost on server restart in this case, but the plugin will continue to function normally.

## Performance

- **Asynchronous processing** - Database operations and notifications run on separate threads
- **Memory caching** - Frequently accessed data is cached in memory
- **Optimized queries** - Database queries are optimized for performance
- **Non-blocking operations** - All heavy operations are async to prevent server lag

## API Version

This plugin supports PaperMC 1.21.7 and requires Java 21.

## Support

For support, issues, or feature requests, please visit the project page or contact the development team.

## License

This project is licensed under the appropriate license as specified in the project files. 