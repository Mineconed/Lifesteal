<p align="center"><img src="Lifesteal_ikon_128.png" width="128"></p>
<h1 align="center">Lifesteal</h1>
<p align="center">Kill players to steal their hearts. Lose them all and you are out.<br>Made by <b>Mineconed</b> • Minecraft 1.8 to 1.21</p>

## Features
- ❤️ **Steal a heart** when you kill a player
- ☠️ **0 hearts = out**: ban for X time (or forever) or spectator
- 💎 **/withdraw** turns hearts into items you can trade or sell
- ✨ Right click a heart item to get +1 heart
- 📈 Set start hearts, max hearts and hearts per kill
- 🛡️ **Anti farm**: same IP kills and killing the same player again too soon do not count
- 🎁 At max hearts the heart drops as an item instead
- ✚ **/lifesteal revive** brings players back
- 🌍 Turn it off in some worlds
- 🧩 Works great with CustomRecipes for a heart recipe
- 🌐 English and Danish built in

## Setup
1. Put **Lifesteal.jar** in your plugins folder
2. Start the server. Done!
3. (Optional) change hearts and ban time in the config and run **/ls reload**

## Commands
| Command | What it does |
|---|---|
| `/hearts [player]` | see hearts |
| `/withdraw [amount]` | turn hearts into items |
| `/ls set|add|remove <player> <hearts>` | change hearts |
| `/ls giveheart <player> [amount]` | give heart items |
| `/ls revive <player>` | unban and give hearts back |
| `/ls reload` | reload |

## Permissions
| Permission | What it does |
|---|---|
| `lifesteal.withdraw` | use /withdraw (everyone) |
| `lifesteal.admin` | admin commands (op) |

## Config
| Setting | What it does |
|---|---|
| `start_hearts / max_hearts` | hearts at start and max |
| `hearts_per_kill` | hearts stolen per kill |
| `lose_heart_on_natural_death` | lose a heart without a killer |
| `drop_heart_when_full` | drop a heart item when the killer is full |
| `elimination.action` | ban or spectator |
| `elimination.ban_minutes` | ban time, 0 = forever |
| `revive_hearts` | hearts after ban or revive |
| `anti_farm` | same IP and same victim rules |
| `heart_item.material` | the heart item |
| `disabled_worlds` | worlds without lifesteal |

## Versions
1.8 to 1.21.

## Build
You need Java 8 or newer and Maven.
```
mvn package
```
The plugin is in `target/Lifesteal.jar`.

## License
MIT. Made by Mineconed.
