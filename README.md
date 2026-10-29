<h1 align="center">TabPlus</h1>

<p align="center">
  A clean, animated and rank-aware tablist plugin for Paper and Spigot.
</p>

<p align="center">
  <img alt="License" src="https://img.shields.io/badge/license-MIT-blue.svg">
  <img alt="Java" src="https://img.shields.io/badge/Java-17+-orange.svg">
  <img alt="Minecraft" src="https://img.shields.io/badge/Minecraft-1.18+-green.svg">
  <img alt="Platform" src="https://img.shields.io/badge/platform-Paper%20%7C%20Spigot-lightgrey.svg">
</p>

---

## Overview

TabPlus replaces the default player list (the one shown when you hold **Tab**) with a fully configurable one. It does the tablist and nothing else, so it won't clash with your chat, scoreboard or permission plugins.

## Features

- **Animated title** that cycles through color frames
- **Configurable header and footer** with live placeholders
- **Rank prefixes and colors** per permission group
- **Automatic sorting** of players by rank
- **Live ping** next to each name, colored by latency
- **Hex color support** (`&#RRGGBB`) along with standard `&` codes
- **Per-player toggle** to turn the custom tab off
- **Hot reload** without restarting the server
- No external dependencies

## Requirements

| Requirement | Version |
|---|---|
| Java | 17 or newer |
| Server | Paper or Spigot 1.18+ |

> TPS is shown on Paper only. On Spigot it displays a constant `20.0`.

## Installation

1. Download the latest `TabPlus-x.x.x.jar` from the [Releases](../../releases) page.
2. Place it in your server's `plugins/` folder.
3. Restart the server.
4. Edit `plugins/TabPlus/config.yml` to your liking.
5. Run `/tabplus reload`.

## Building from source

```bash
git clone https://github.com/NOTGAMERTHUZ/TABPLUS.git
cd TABPLUS
mvn clean package
```

The compiled jar will be at `target/TabPlus-1.0.0.jar`.

## Commands

| Command | Description | Permission |
|---|---|---|
| `/tabplus reload` | Reloads the configuration | `tabplus.admin` |
| `/tabplus toggle` | Toggles the custom tab for yourself | `tabplus.toggle` |

Alias: `/tp`

## Permissions

| Permission | Description | Default |
|---|---|---|
| `tabplus.admin` | Use `/tabplus reload` | op |
| `tabplus.toggle` | Use `/tabplus toggle` | everyone |
| `tabplus.group.owner` | Owner rank in tab | none |
| `tabplus.group.admin` | Admin rank in tab | none |
| `tabplus.group.mod` | Mod rank in tab | none |
| `tabplus.group.vip` | VIP rank in tab | none |

Assign a rank by giving the player its permission. With LuckPerms, for example:

```
/lp user <name> permission set tabplus.group.vip
```

## Placeholders

Usable in `header`, `footer` and `player-format.suffix`:

| Placeholder | Value |
|---|---|
| `%title%` | Current animated title frame |
| `%player%` | Player name |
| `%displayname%` | Player display name |
| `%online%` / `%max%` | Online and max players |
| `%ping%` | Player ping in ms |
| `%ping_color%` | Color based on ping |
| `%tps%` | Server TPS (Paper) |
| `%world%` | Current world |
| `%group%` | Player's rank |
| `%time%` | Server time |
| `%server%` | Server name from config |

## Configuration

Everything is in `config.yml`. A short example of adding a custom rank:

```yaml
groups:
  youtuber:
    permission: tabplus.group.youtuber
    priority: 5
    prefix: "&#FF0000&lYT &r"
    name-color: "&#FF5555"
```

Lower `priority` numbers appear higher in the list.

## Contributing

Issues and pull requests are welcome. For larger changes, please open an issue first to discuss what you'd like to change.

## License

Released under the [MIT License](LICENSE).

---

<p align="center">Made by <a href="https://github.com/NOTGAMERTHUZ">NOTGAMERTHUZ</a></p>
