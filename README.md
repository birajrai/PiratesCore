# PiratesCore

[![Build Status](https://github.com/birajrai/PiratesCore/actions/workflows/maven.yml/badge.svg)](https://github.com/birajrai/PiratesCore/actions/workflows/maven.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![GitHub Repo](https://img.shields.io/badge/GitHub-birajrai%2FPiratesCore-blue?logo=github)](https://github.com/birajrai/PiratesCore)

> ⚓️ Supercharge your Minecraft server with modular features, smart mining alerts, and seamless Discord integration—all in one plugin!

---

## Overview
- Modular Minecraft plugin for Spigot/Paper servers
- Advanced ore mining notifications (real-time and batched)
- Mining statistics tracking and display
- Chat filtering with configurable bad words list
- Quality-of-life features for admins and players
- Extensible via FeatureManager for easy addition of new features/listeners
- Seamless integration with external services (e.g., Discord webhooks)

---

## Unique Features
- **Real-Time & Batched Ore Alerts:** Instantly notify admins or Discord when rare ores are mined, with support for delayed/batched notifications.
- **Discord Webhook Integration:** Send rich, customizable mining alerts directly to Discord channels.
- **Player Mining Stats:** Track, display, and rank player mining activity with `/oremining stats` and `/oremining top`.
- **In-Game Config Reload:** Instantly reload all configs and chat filters with `/pirates reload`—no server restart needed.
- **Better Mending:** Repair items using XP with a simple action.
- **Chat Filtering:** Replace bad words in chat using a YAML-configurable list, with optional Discord logging.
- **Extensible Feature System:** Easily add or toggle features via `Settings.yml` and the `FeatureManager`.
- **Async Database & Webhook Operations:** All heavy operations are async to keep your server lag-free.

---

## Features
- **Ore Mining Notifications:**
  - Real-time and batched alerts for rare ore mining
  - Discord webhook support with rich embeds
  - Customizable tracked blocks, thresholds, and messages
- **Mining Statistics:**
  - Tracks player mining stats and top miners
  - Session summaries and leaderboards
- **Chat Filtering:**
  - Configurable bad words list (`BadWords.yml`)
  - Optional Discord webhook for flagged messages
- **Better Mending:**
  - Repair items with XP
- **Config Reloading:**
  - Reload all configs in-game with `/pirates reload`
- **Extensible:**
  - Add new features/listeners via `FeatureManager` and `Settings.yml`

---

## Commands

| Command                | Description                                                      |
|------------------------|------------------------------------------------------------------|
| `/pirates reload`      | Reload all configs and chat filter                               |
| `/pirates updatesql`   | Reload SQL config and update all player stats (if enabled)        |
| `/show`                | Broadcast the item you're holding (with LuckPerms prefix/lore)    |
| `/oremining toggle`    | Toggle ore mining notifications                                  |
| `/oremining stats`     | View your mining stats (or another's, if admin)                  |
| `/oremining top`       | Show top miners leaderboard                                      |
| `/oremining ignore`    | Manage ignore lists (admin only)                                 |

---

## Configuration

Example: `OreMining.yml`
```yaml
tracked-blocks:
  - DIAMOND_ORE
  - ANCIENT_DEBRIS
alert-threshold: 5
message-format: "&b{player} found {amount} {block}!"
```

Example: `Settings.yml`
```yaml
features:
  BetterMending: true
  OreMining: true
  ChatFilter: true
  Stats: true
```

Example: `Webhook.yml`
```yaml
discord:
  url: "https://discord.com/api/webhooks/..."
  embed: true
```

Example: `BadWords.yml`
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
- **Database:** Uses H2 embedded database for mining stats; MySQL support for player stats.
- **Async Operations:** Database and webhook operations are asynchronous to avoid blocking the main server thread.
- **PlaceholderAPI:** Optional support for advanced message placeholders.
- **LuckPerms:** Used for player prefix display in chat/item broadcasts.

---

## License
[MIT](LICENSE)
