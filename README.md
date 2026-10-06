# AdminVanish

A server-side Fabric mod for Minecraft **26.2** that lets staff vanish completely. Players do not
need to install anything. It also works in singleplayer/LAN.

## Features

- **`/adminvanish`** toggles vanish. While vanished you:
  - are hidden from the **tab list** of everyone who can't see vanished players
  - have **no player model, name tag, armor or held items** visible to them (your entity is never sent)
  - are put in **creative mode** and can **fly through blocks** (no-clip)
  - your previous game mode comes back when you unvanish
- **Nothing gives you away**:
  - you're left out of `/list` and the server list's player count and names
  - you don't earn advancements, so nothing is announced in chat
  - sounds you cause (breaking/placing blocks, doors, ...) aren't heard by other players, and you
    don't set off sculk sensors or wardens
  - chests, barrels, ender chests and shulker boxes open **silently with no lid animation**
- **Silent joins and leaves**: vanish state is saved (in `config/adminvanish.json`), so a vanished
  player stays vanished after relogging or a restart, and their join/leave messages are suppressed.
  Staff who can see vanished players get a private `[AdminVanish]` notice instead.
- **Fake join/leave messages**: vanishing broadcasts a fake "*X left the game*" and unvanishing a
  fake "*X joined the game*". You can also send them by hand.
- **Teleport while vanished**: `/adminvanish <player>` vanishes you (if you aren't already) and
  teleports you to that player, across dimensions too.

## Commands

| Command | Description |
| --- | --- |
| `/adminvanish` | Toggle vanish, with a fake leave/join message |
| `/adminvanish <player>` | Vanish (if needed) and teleport to `<player>` |
| `/adminvanish tp <player>` | Same as above (use this if a player is named `list`, `quiet`, ...) |
| `/adminvanish quiet` | Toggle vanish without a fake message |
| `/adminvanish fakejoin` | Broadcast a fake "joined the game" message for yourself |
| `/adminvanish fakeleave` | Broadcast a fake "left the game" message for yourself |
| `/adminvanish noclip` | Toggle moving through blocks while vanished |
| `/adminvanish list` | List vanished players |

## Permissions

| Node | Default | Grants |
| --- | --- | --- |
| `adminvanish.use` | op (level 2+) | Using `/adminvanish` |
| `adminvanish.see` | op (level 2+) | Seeing vanished players and getting staff notices |

Permission nodes work with any permissions mod that supports the
[Fabric Permissions API](https://github.com/lucko/fabric-permissions-api) (e.g. LuckPerms).
Without one, ops are used.

## How no-clip works (and its one quirk)

A vanilla client only moves through blocks in spectator mode. AdminVanish keeps you in **creative**
on the server but tells *your own* client that you are a spectator, so you get no-clip *and* the
creative inventory, block placing and breaking.

The quirk: because your client thinks it's a spectator, the **number keys and scroll wheel don't
change hotbar slots**. Pick items in the inventory screen (`E`) instead, or turn no-clip off with
`/adminvanish noclip` while you build.

## Not hidden

Chat messages you send and commands that target players (like `/msg` or `@a`) can still reveal
you. Staff with `adminvanish.see` still see you everywhere and hear your sounds, and the console
still lists you in `/list`.

## Building

Requires Java 25.

```sh
./gradlew build
```

The jar is written to `build/libs/`. Install it in the server's `mods` folder together with
[Fabric API](https://modrinth.com/mod/fabric-api).
