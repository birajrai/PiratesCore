# PiratesCore

[![Build Status](https://github.com/birajrai/PiratesCore/actions/workflows/maven.yml/badge.svg)](https://github.com/birajrai/PiratesCore/actions/workflows/maven.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![GitHub Repo](https://img.shields.io/badge/GitHub-birajrai%2FPiratesCore-blue?logo=github)](https://github.com/birajrai/PiratesCore)

> ⚓️ Supercharge your Minecraft server with modular features, smart mining alerts, and seamless Discord integration—all in one plugin!

## Overview
- Modular Minecraft plugin for Spigot/Paper servers
- Advanced ore mining notifications (real-time and batched)
- Mining statistics tracking and display
- Chat filtering with configurable bad words list
- Quality-of-life features for admins and players
- Extensible via FeatureManager for easy addition of new features/listeners
- Seamless integration with external services (e.g., Discord webhooks)

---

## Features
- **Ore Mining Notifications:** Real-time and batched alerts for rare ore mining, with Discord webhook support.
- **Mining Statistics:** Tracks and displays player mining stats, top miners, and session summaries.
- **Chat Filtering:** Replaces bad words in chat using a configurable YAML list.
- **Better Mending:** Allows players to repair items using XP with a simple action.
- **Config Reloading:** Reload all configs in-game without restarting the server.
- **Extensible:** Easily add new features or listeners via the FeatureManager.

---

## Quickstart / Installation
1. **Download** the latest PiratesCore JAR and place it in your server's `plugins/` directory.
2. **Dependencies:**
   - [Vault](https://www.spigotmc.org/resources/vault.34315/) (required, included in `lib/`)
   - [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) (optional, for advanced message placeholders)
   - [LuckPerms](https://luckperms.net/) (optional, for player prefix display)
3. **Start your server.** PiratesCore will generate default configuration files in `plugins/PiratesCore/`.
4. **Edit configuration files** as needed (see below).
5. **Use in-game commands** to reload configs or manage features.

---

## Usage

### Main Commands
- `/pirates reload` — Reload all configs and chat filter.
- `/show` — Broadcast the item you're holding, with LuckPerms prefix and item lore support.
- `/oremining` — Manage ore mining features:
  - `/oremining toggle` — Toggle notifications
  - `/oremining stats` — View your mining stats
  - `/oremining top` — Show top miners
  - `/oremining ignore` — Manage ignore lists

### Example Configuration Files

#### `OreMining.yml`
```yaml
tracked-blocks:
  - DIAMOND_ORE
  - ANCIENT_DEBRIS
alert-threshold: 5
message-format: "&b{player} found {amount} {block}!"
```

#### `Settings.yml`
```yaml
features:
  BetterMending: true
  OreMining: true
```

#### `Webhook.yml`
```yaml
discord:
  url: "https://discord.com/api/webhooks/..."
  embed: true
```

#### `BadWords.yml`
```yaml
bad-words:
  - badword1
  - badword2
```

---

## File Structure
- `src/main/java/xyz/dapirates/` — Main Java source code
  - `command/` — Command executors
  - `listener/` — Event listeners
  - `manager/` — Managers for configs, features, database, etc.
  - `service/` — Data models and session tracking
  - `utils/` — Utility classes and config loaders
- `src/main/resources/` — Default configuration files
- `lib/` — Bundled dependencies (e.g., Vault)

---

## Building from Source
1. **Clone the repository:**
   ```sh
   git clone https://github.com/birajrai/PiratesCore.git
   cd PiratesCore
   ```
2. **Build with Maven:**
   ```sh
   mvn clean package
   ```
   The compiled JAR will be in the `target/` directory.

> **Note:** Every push and pull request is automatically built and tested using [GitHub Actions](https://github.com/birajrai/PiratesCore/actions) with Maven. Check the badge above for the current build status.

---

## Extending PiratesCore
- **Add a new feature:**
  1. Implement a Listener.
  2. Register it in `FeatureManager`.
  3. Add a toggle in `Settings.yml`.
- **Add a new command:**
  1. Implement `CommandExecutor`.
  2. Register in `CommandManager`.
  3. Add to `plugin.yml`.
- **Integrate with new webhooks:**
  1. Add the webhook URL to `Webhook.yml`.
  2. Use `WebhookManager` to send messages.

---

## Developer Notes
- **Database:** Uses H2 embedded database for mining stats.
- **Async Operations:** Database and webhook operations are asynchronous to avoid blocking the main server thread.
- **PlaceholderAPI:** Optional support for advanced message placeholders.
- **LuckPerms:** Used for player prefix display in chat/item broadcasts.

---

## License
[MIT](LICENSE) (or specify your license here)
