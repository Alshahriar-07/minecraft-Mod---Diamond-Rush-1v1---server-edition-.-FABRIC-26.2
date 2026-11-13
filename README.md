# DIAMOND RUSH 1v1

A competitive, server-authoritative Fabric mod for **Minecraft 26.2**: two players
drop into an arena, wait for both to press **READY**, then race to mine the first
diamond. Best-of-five series, first to **3** match wins takes the tournament.

* Custom client GUI with original art (no vanilla dirt menu, no default buttons)
* Fully server-authoritative: winner detection, scores, timers, teleports and
  inventory resets never trust the client
* Persistent arenas and statistics across server restarts
* Death does **not** end a match; players respawn inside the arena

Built jar: `build/libs/diamond-rush-1.0.0.jar`

---

## 1. Requirements

| Component | Version |
|---|---|
| Minecraft | **26.2** (Java Edition, released 2026-06-16) |
| Mod loader | **Fabric Loader 0.20.1** (0.20.x supported) |
| Fabric API | **0.161.0+26.2** |
| Java | **25** (Minecraft 26.2 requires Java 25) |
| Gradle (build only) | **9.9.1** (Gradle 9.8+ is required by Fabric Loom 1.19) |

Minecraft 26.x is **unobfuscated**. Yarn mappings do not exist for 26.2 and the
build therefore uses the non-remapping Loom plugin
(`net.fabricmc.fabric-loom` 1.19-SNAPSHOT) with Mojang class names and no
`mappings(...)` dependency. Do not add Yarn or the legacy `fabric-loom` plugin.

---

## 2. Installation

### 2.1 Dedicated server

1. Install **Fabric Loader 0.20.1 for Minecraft 26.2** on the server
   (Fabric installer → *Install server* → Minecraft version `26.2`).
2. Download **Fabric API 0.161.0+26.2** and place it in `mods/`.
3. Copy `diamond-rush-1.0.0.jar` into `mods/`.
4. Start the server once (or run `/reload confirm` after uploading).
5. Configure at least one arena (see §4).

### 2.2 Player clients

Both players must run the mod, because the custom interface and its buttons are
client-side:

1. Install **Fabric Loader 0.20.1** for Minecraft **26.2**.
2. Put **Fabric API 0.161.0+26.2** in `.minecraft/mods/`.
3. Put `diamond-rush-1.0.0.jar` in `.minecraft/mods/`.
4. Launch the Fabric 26.2 profile and join the server.

The mod is `environment: "*"`, so the same jar works on the server and client.
If a client joins without the mod the server still runs the match logic, but that
player will not see the match screens or READY buttons.

---

## 3. Gameplay flow

1. Player A runs `/diamondrush join` → sees **WAITING FOR OPPONENT**.
2. Player B runs `/diamondrush join` → the two are paired, teleported to the same
   arena (separate spawns), inventories are cleared and both see the **READY**
   screen with the current score.
3. Each player independently clicks **READY** (server validated). A 3-second
   countdown runs; block breaking and leaving spawn are blocked during it.
4. **GO!** The live match timer starts.
5. The first player to legitimately break **DIAMOND_ORE** or
   **DEEPSLATE_DIAMOND_ORE** wins the game. The win is recorded exactly once,
   even for simultaneous breaks.
6. The result screen shows winner, score and match time. Both players must click
   **PLAY AGAIN**. The next game teleports both to a *different* arena when
   another configured arena exists, resets inventories/armour/offhand/XP/effects
   and starts a fresh READY sequence.
7. First to **3** wins ends the tournament. The champion is announced server-wide,
   the final scoreboard is shown and statistics are saved. Both players may click
   **NEW TOURNAMENT** to run it back.

Death never ends a match: the player respawns at their arena spawn with a fresh
kit while the timer and score keep running.

Starting kit per match: iron pickaxe, 32 torches, 16 cooked beef.
Inventory, armour, offhand, XP levels/points and potion effects are always reset
between games.

---

## 4. Arena setup (admin)

Arenas are two spawn points in any loaded dimension. Stand where a player should
start, then run:

```
/diamondrush arena create <name>
/diamondrush arena setspawn <name> player1
/diamondrush arena setspawn <name> player2
/diamondrush arena list
```

Arenas marked `[ready]` are complete. `[incomplete]` arenas need both spawns and
are never selected for a match. Create at least two arenas so rematches rotate to
a different location; with a single arena the mod reuses it (there is no
alternative).

Arena data is stored in `config/diamondrush.json` (JSON, human editable). Spawn
dimensions are stored as identifiers such as `minecraft:overworld`, so arenas
survive restarts, world reloads and dimension registry changes gracefully.

Example:

```json
{
  "version": 1,
  "arenas": {
    "cavern": {
      "name": "cavern",
      "spawn1": { "dimension": "minecraft:overworld", "x": 100.0, "y": 70.0, "z": 100.0, "yaw": 0.0, "pitch": 0.0 },
      "spawn2": { "dimension": "minecraft:overworld", "x": 120.0, "y": 70.0, "z": 100.0, "yaw": 180.0, "pitch": 0.0 }
    }
  }
}
```

---

## 5. Commands

Admin commands require game-master permission (op level 2). Player commands are
available to everyone.

| Command | Who | Description |
|---|---|---|
| `/diamondrush join` | player | Join the 1v1 queue; pairs automatically with the waiting player |
| `/diamondrush leave` | player | Leave the queue, or forfeit an active match |
| `/diamondrush menu` | player | Reopen the current match screen |
| `/diamondrush stats <player>` | player | Show match/tournament wins and fastest win |
| `/diamondrush arena create <name>` | admin | Create an empty arena |
| `/diamondrush arena setspawn <name> <player1\|player2>` | admin | Set a spawn to your current position/rotation |
| `/diamondrush arena list` | all | List arenas, completeness and in-use state |
| `/diamondrush arena remove <name>` | admin | Delete an arena (refused while in use) |
| `/diamondrush start` | admin | Start the next pair from the queue |
| `/diamondrush start <player1> <player2>` | admin | Start a tournament between two online players |
| `/diamondrush stop` | admin | Stop all tournaments and clear the queue |

---

## 6. Files written by the mod

| Path | Contents |
|---|---|
| `config/diamondrush.json` | Arena definitions and last used arena |
| `config/diamondrush-stats.json` | Per-player match wins, matches played, tournaments won, fastest win |

Writes are atomic (temporary file + move) so a crash cannot leave a truncated
config. Both files are read at startup, which is what makes arenas and stats
survive a restart.

---

## 7. Architecture

All important logic is server-side and split into focused components:

| Component | Responsibility |
|---|---|
| `DiamondRushMod` | Entrypoint, wiring, lifecycle/tick/command registration |
| `ConfigurationManager` | Atomic JSON persistence |
| `ArenaManager` | Arena CRUD, validation, random selection, reservations |
| `SpawnManager` | Teleports, respawn anchoring, countdown spawn clamp |
| `MatchmakingManager` | Two-slot queue and pairing |
| `MatchManager` | Authoritative state machine (READY → COUNTDOWN → MATCH → RESULT/CHAMPION) |
| `PlayerReadyManager` | Ready / Play Again / New Tournament confirmations |
| `DiamondDetectionManager` | Server block-break detection and mining gate |
| `TournamentManager` | Best-of-five rules, champion, statistics hooks |
| `ScoreManager` | Duplicate-safe score mutation (one award per match number) |
| `StatisticsManager` | Persistent per-player stats |
| `InventoryManager` | Inventory/XP/effect reset and starting kit |
| `NetworkHandler` | Payload registration and snapshot delivery |
| `CustomGuiManager` | Client screen lifecycle |
| `DiamondRushScreen` | Custom GUI rendering and hit-testing |

### Networking

* `diamondrush:gui_state` (S→C) — complete GUI snapshot: state, names, scores,
  timer, countdown, ready/rematch flags, headline, footer, arena.
* `diamondrush:gui_action` (C→S) — intent only: `READY`, `PLAY_AGAIN`,
  `NEW_TOURNAMENT`, `LEAVE`.

Every action is re-validated against membership, phase and current flags. Because
the server sends whole snapshots rather than deltas, a reconnecting or
desynchronised client can never display a fabricated score or winner.

### 26.2 API notes

* Minecraft is unobfuscated; classes use Mojang names (`Identifier`, not
  `ResourceLocation`).
* Screens render through the retained-mode `GuiGraphicsExtractor`
  (`extractRenderState(...)`) instead of the old `GuiGraphics`.
* Screens are opened with `Minecraft.getInstance().gui.setScreen(...)`.
* Command permissions use `Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)`.
* Player teleports use `ServerPlayer#teleportTo(ServerLevel, x, y, z, Set<Relative>, yaw, pitch, resetCamera)`
  and respawn anchors use `ServerPlayer.RespawnConfig`.
* Damage/break hooks come from Fabric API (`PlayerBlockBreakEvents`,
  `ServerPlayerEvents`, `ServerPlayConnectionEvents`, `ServerTickEvents`).

---

## 8. Assets

Original artwork, drawn as SVG by `design/generate_svgs.py` (hand-written 5×7
pixel font, no third-party art) and rasterised with `sharp` into:

```
assets/diamondrush/icon.png
assets/diamondrush/textures/gui/panel.png
assets/diamondrush/textures/gui/logo.png
assets/diamondrush/textures/gui/scoreboard.png
assets/diamondrush/textures/gui/diamond.png
assets/diamondrush/textures/gui/champion.png
assets/diamondrush/textures/gui/victory.png
assets/diamondrush/textures/gui/defeat.png
assets/diamondrush/textures/gui/button_ready(_hover).png
assets/diamondrush/textures/gui/button_play_again(_hover).png
assets/diamondrush/textures/gui/button_new_tournament(_hover).png
assets/diamondrush/textures/gui/button_leave(_hover).png
```

Palette: dark Minecraft-inspired panels, diamond cyan `#3FE0F5`, green READY
`#57E86B`, gold champion `#FFC542`.

Regenerate (optional, requires Python 3 and Node):

```bash
python design/generate_svgs.py
node design/svg_to_png.js      # uses tools/img/node_modules/sharp
```

---

## 9. Building from source

```bash
# Java 25 and Gradle 9.8+ required
./gradlew build
# output: build/libs/diamond-rush-1.0.0.jar
```

The wrapper pins Gradle 9.9.1. `./gradlew runServer` and `./gradlew runClient`
launch Loom development environments (useful for local testing).

---

## 10. Verification performed

Verified on a real Fabric 26.2 dedicated server (Fabric Loader 0.20.1, Fabric API
0.161.0+26.2, Java 25):

* Server boots with the mod and Fabric API; log shows
  `Diamond Rush 1v1 loaded ... with 1 arena(s)`.
* Command tree registers (`/diamondrush` returns the help output over RCON).
* `/diamondrush arena create`, duplicate-name rejection, `arena list`,
  `arena remove`, `start` (no queue) and `stop` behave as documented.
* Arena data written to `config/diamondrush.json`, both spawns set in-game with
  `/diamondrush arena setspawn`, and reloaded correctly after a full server
  restart (arena showed `[ready]`).
* A real 26.2 Fabric client launched against the test server, connected, and
  rendered the custom **WAITING FOR OPPONENT** screen with the shipped textures,
  logo, queue counter and LEAVE QUEUE button (adaptive scaling confirmed on a
  1024x512 window).
* Mod jar contains expanded `fabric.mod.json`, all classes and all PNG assets.

Not exercised in this session: a full two-player match (READY → countdown →
diamond win → rematch), death/respawn inside a match, and champion/next-tournament
flow. Those code paths are server-authoritative and guarded; treat a manual
two-client playtest as the final sign-off.

Known limitations:

* Arenas are defined by spawn points only, not protected regions; build arenas in
  a closed area if other players share the world.
* If the server process is killed (not stopped) mid-match, players keep the
  starting kit; a graceful stop resets participants' inventories.
* One active queue globally (the queue is a single two-slot line).
