# PiratesCore

[![Build Status](https://github.com/birajrai/PiratesCore/actions/workflows/maven.yml/badge.svg)](https://github.com/birajrai/PiratesCore/actions/workflows/maven.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![GitHub Repo](https://img.shields.io/badge/GitHub-birajrai%2FPiratesCore-blue?logo=github)](https://github.com/birajrai/PiratesCore)

> ⚓️ Make your Minecraft server smarter and more fun! Get instant mining alerts, chat protection, and easy-to-use features—all in one plugin.

---

## What is PiratesCore?
PiratesCore is a plugin for Minecraft servers (Spigot/Paper) that adds helpful tools for players and admins. It makes mining, chatting, and server management easier and more fun.

---

## Main Features

- **Mining Alerts:**
  - Get a message when someone finds rare ores (like diamonds).
  - You can get these alerts in-game or on Discord.
  - Alerts can be sent right away or grouped together.

- **Mining Stats:**
  - See how many ores you or others have mined.
  - Check who are the top miners on the server.
  - Use commands to see your stats or the leaderboard.

- **Show Hand Item:**
  - Use `/show` to broadcast the item you're holding to everyone on the server.
  - Shows your LuckPerms prefix and item lore if available.

- **Chat Filter:**
  - Bad words in chat are blocked or replaced.
  - You can change the list of bad words easily.
  - If you want, get a Discord message when someone uses a bad word.

- **Better Mending:**
  - Fix your items using XP with a simple action.

- **Easy Config Reload:**
  - Change settings and reload them without restarting the server.
  - Just use a command to reload everything.

- **Add or Remove Features:**
  - Turn features on or off in the settings file.
  - Add new features easily if you want to customize your server.

- **No Lag:**
  - All heavy work (like saving stats or sending webhooks) is done in the background, so your server stays fast.

---

## Commands

| Command                | What it does                                              |
|------------------------|----------------------------------------------------------|
| `/pirates reload`      | Reload all settings and chat filter                      |
| `/pirates updatesql`   | Reload SQL settings and update player stats (if enabled) |
| `/show`                | Show everyone the item you are holding                   |
| `/oremining toggle`    | Turn mining alerts on or off for yourself                |
| `/oremining stats`     | See your mining stats (or someone else's, if admin)      |
| `/oremining top`       | See the top miners on the server                         |
| `/oremining ignore`    | Manage ignore lists (admin only)                         |

---

## How to Set Up (Quick Guide)
1. Download the PiratesCore JAR and put it in your server's `plugins/` folder.
2. Make sure you have [Vault](https://www.spigotmc.org/resources/vault.34315/) (required), [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) (optional), and [LuckPerms](https://luckperms.net/) (optional).
3. Start your server. PiratesCore will make its own config files.
4. Edit the config files if you want to change settings.
5. Use the commands above to manage features in-game.

---

## Example Config Files

**OreMining.yml**
```yaml
tracked-blocks:
  - DIAMOND_ORE
  - ANCIENT_DEBRIS
alert-threshold: 5
message-format: "&b{player} found {amount} {block}!"
```

**Settings.yml**
```yaml
features:
  BetterMending: true
  OreMining: true
  ChatFilter: true
  Stats: true
```

**Webhook.yml**
```yaml
discord:
  url: "https://discord.com/api/webhooks/..."
  embed: true
```

**BadWords.yml**
```yaml
bad-words:
  - badword1
  - badword2
```

---

## How to Add Your Own Features
- Make a new Listener (a Java class that listens for events).
- Register it in `FeatureManager`.
- Add a toggle for it in `Settings.yml`.

## How to Add a New Command
- Make a new CommandExecutor (Java class).
- Register it in `CommandManager`.
- Add it to `plugin.yml`.

## How to Add a Webhook
- Add your webhook URL to `Webhook.yml`.
- Use `WebhookManager` to send messages from your code.

---

## Developer Notes
- **Database:** Uses H2 (built-in) for mining stats. Can use MySQL for player stats.
- **Async:** All heavy work is done in the background to keep your server fast.
- **PlaceholderAPI:** You can use placeholders in messages if you have this plugin.
- **LuckPerms:** Used to show player prefixes in chat and item broadcasts.

---

## License
[MIT](LICENSE)
