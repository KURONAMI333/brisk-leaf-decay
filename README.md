# Brisk Leaf Decay

> Chop a tree and its leaves fall within about a second, instead of hanging in the air for minutes.

[![License: All Rights Reserved](https://img.shields.io/badge/License-All_Rights_Reserved-red.svg)](LICENSE)
Homepage (intended CurseForge page, not yet published): https://www.curseforge.com/minecraft/mc-mods/brisk-leaf-decay
Bugs and questions: comment on the CurseForge page, or DM @kuronami333 on X (https://x.com/kuronami333).

---

## Supported Loaders / Versions

| Minecraft | NeoForge | Forge | Fabric |
|---|:---:|:---:|:---:|
| 1.21.1 | ✅ | — | ✅ |
| 1.20.1 | — | ✅ | ✅ |

No dependencies (Fabric API not needed). Server side is enough; it is harmless on clients.

## What it does

- Leaves that are no longer connected to a log fall 2–8 ticks after the game notices, then the next ones follow — a chopped canopy is gone in about a second.
- Drops are exactly what natural decay gives (saplings, sticks, apples), because each leaf still falls through its own decay code. Modded leaves that build on vanilla leaves work the same way.
- Leaves you placed yourself never decay, as in vanilla. A leaf that gets reconnected to a log before it falls is kept.
- With the `randomTickSpeed` game rule at 0, leaves don't decay at all, just like vanilla.
- Nothing to configure.

## Installation

1. Install NeoForge (1.21.1), Forge (1.20.1) or Fabric Loader
2. Drop the jar for your loader and Minecraft version into `mods/`

## License

[All Rights Reserved](LICENSE) — modpack inclusion welcome, no credit required.

## Credits

- Author: KURONAMI
