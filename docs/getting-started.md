# Starting Remnants: Roguelite Dungeons

This is the player guide for **Remnants: Roguelite Dungeons** (formerly *The Birth of
Steve*). You do not need commands or cheats to begin the adventure.

## Install and launch

1. Install **Minecraft Java Edition 1.20.1, 1.21.1, 26.1.2, 26.2 or 26.3** and a
   loader for that version: **Forge** or Fabric on 1.20.1, **NeoForge** or Fabric on
   the others.
2. Put the JAR matching **both** your Minecraft version and your loader in the
   instance's `mods` folder, for example `tbos-neoforge-26.1.2-<version>.jar` or
   `tbos-fabric-1.21.1-<version>.jar`. The jar refuses to load on the wrong
   Minecraft version rather than misbehaving, so if the game reports a version
   mismatch you have the other one.
3. Launch the game and create or open an Overworld save. Existing worlds work:
   the first join still grants the Archivist's Journal, and Fracture Shrines
   appear when their planned chunk is loaded, including terrain that already
   existed before the mod was installed. For multiplayer, put the same JAR on
   both the server and every client.
4. Make a backup before adding the mod to an existing world. A shrine carves a
   small 9×9 ruin into whatever is there; the Meridian Archive is placed later, when
   you use the Archive Survey Map.

On NeoForge and Forge no other mod is required. On Fabric you also need **Fabric
API**. Server owners can tune the dungeon in `config/tbos-common.json` and the 1.0
Expeditions, Codex, Relics and parties in `config/tbos-expeditions.json`.

## Your first expedition

The first time you join a world you are given the **Archivist's Journal** and a
short chat message telling you where to start. Right-click the Journal at any
time to reread the full route; each step's detail pane also keeps a short note
from the last archivist.

Each world plans three Fracture Shrine variants at persistent, world-seeded
locations scattered 192–640 blocks from world spawn. Their positions do not
depend on which player joins first, and each shrine is built when its own chunk
is loaded — new terrain or already-generated chunks in an existing save. Exploring
toward one is what makes it appear. Already-loaded spawn or logout chunks are
scanned once when the world starts, so a shrine you are already standing on does
not wait for a reload.
The first time you reach a shrine it names itself. Explore for a shrine, open its
Fracture Coffer, recover the **Cracked Yesterglass Lens** and the **Archive Survey
Map**, plus Memory Plates and Lens repair materials. Repair the lens, then use the
Survey Map by right-clicking it: it reports the direction, distance, and coordinates
of the Meridian Archive.

Follow the map and play through the authored route:

1. Reconstruct the Parallax Atrium.
2. Align the Hall of Alignment.
3. Complete the Choir of Hours.
4. Cross the Broken Meridian.
5. Defeat the Last Curator and collect the **Curator Core**.

After the fight, the Archive Core in the center of the Grand Orrery transforms
into the **Curator Gateway**. Keep the Curator Core in your inventory, hold the
repaired Yesterglass Lens, and right-click the gateway. The server constructs
your run and teleports your party into **Echoes of the Past**, the replayable
procedural Fractured Archive dimension.

Inside it, watch the top-left objective tracker: it records room, puzzle, and
boss-gate progress. Normal dungeon blocks cannot be broken or placed. Archive
caches are the exception: clear their room first, then **break the cache** to
claim its loot. Right-clicking a cache only reads its seal.

Each floor has a **Waystone** in a side chamber off the start. Right-click it to
save that floor and return to the Overworld. Right-click a Waystone in the
Overworld — including one you craft from Archive Stone, Chronicle Bronze, and
Lenswork Crystal — to resume at the last floor you bound. Dungeon Waystones
cannot be broken; the ones you place can.

## Expeditions: Remember or Forget

Once the Fractured Archive is open, every run is an **Expedition**:

1. Craft a **Memory Key**: four Amethyst Shards in the corners, four Gold Ingots on
   the edges and an Ender Pearl in the centre. It starts at tier 1.
2. Use it on a Waystone or the Curator Gateway. The **Attunement** screen shows the
   tier and lets you toggle **Distortions** for more Heat and better rewards. Press
   **Begin**; party members within 24 blocks come with you.
3. Cleared rooms, wardens and floor bosses add Renown and Relics to your
   **Recollection**. At the floor's gateway choose **Remember** to bank everything
   (and take home a better key), or **Forget** to vote to go deeper for more
   Momentum. A failed run loses whatever you have not banked.
4. Banked Renown levels up your **Archive Codex** (press **N** or use `/codex`).
   Spend Insight on permanent perks, and claim the Weekly Recollection from it.

Use a key inside any other structure, a village or the Trial Chambers for
example, to open a **Recollection** of that place. `/party create`,
`/party invite <player>` and `/party accept` bring friends along. The full rules and
numbers are in [`remember-or-forget.md`](remember-or-forget.md).

### Expedition commands

| Command | What it does |
| --- | --- |
| `/codex` | Opens the Archive Codex (also `/codex insights`, `bestiary`, `records` and `weekly`). |
| `/expedition` or `/expedition status` | Reports your current Expedition. |
| `/expedition choose` | Reopens the Remember or Forget choice. |
| `/expedition reroll` | Uses Second Draft to rewrite the current Living Memories draft. |
| `/party create · invite · accept · decline · leave · kick · disband · list` | Party management; no operator permission needed. |
| `/expedition key <tier> [distortions]` | Operator: gives a Memory Key. |
| `/expedition weekly_key` | Operator: gives this week's Weekly key. |
| `/expedition relic <tier> <rarity>` | Operator: gives a generated Relic. |
| `/codex renown <player> <amount>` and `/codex reset <player>` | Operator: adjusts or resets a player's Codex. |

## Multiplayer notes

Each party member keeps their own checkpoint and cache claim state. Doors wait
for every party member to clear the entrance before sealing. A death spends from
the party revive pool and returns that player to their saved branch checkpoint.

## Operator and test commands

All `/tbos` commands require game-master permission. They are for server
operators, development, and recovery—not normal progression. Commands that
alter a run can spoil or remove live player progress.

| Command | What it does |
| --- | --- |
| `/tbos showcase` | Builds the authored showcase route at your position and gives a repaired lens. |
| `/tbos locate` | Reports the nearest authored site. |
| `/tbos reset_site` | Resets the nearest authored site. |
| `/tbos debug_transition` or `/tbos debug transition` | Toggles a nearby site transition for testing. |
| `/tbos run start` | Starts a run from the threshold at your position. |
| `/tbos run status` | Shows your current run state. |
| `/tbos run complete` | Treats the current floor as won and queues the next Archive floor. |
| `/tbos run abandon` | Forces the current active run into its failure return. |
| `/tbos debug give_cracked_lens` | Gives a Cracked Yesterglass Lens. |
| `/tbos debug give_survey_map` | Gives an Archive Survey Map. |
| `/tbos debug give_journal` | Gives an Archivist's Journal. |
| `/tbos debug place_shrines` | Alias of `/tbos shrine place_all`. |
| `/tbos debug give_memory_kit` | Gives the Memory Lantern and every Memory Plate. |

### Fracture Shrine controls

| Command | What it does |
| --- | --- |
| `/tbos shrine locate` | Reports the nearest shrine's variant, bearing, distance, coordinates, and whether it has generated yet. |
| `/tbos shrine list` | Lists all three planned shrines and their generated/pending state. |
| `/tbos shrine place [variant]` | Force-builds a shrine at your position. Omit `variant` for the nearest one still pending; otherwise pass `observatory`, `curator_workshop`, or `evacuation_gate`. |
| `/tbos shrine place_all` | Immediately builds every planned shrine instead of waiting for its chunk. |

### Echoes of the Past controls

| Command | What it does |
| --- | --- |
| `/tbos dungeon generate [seed]` | Creates a debug Archive run; omit `seed` for a time-derived seed. |
| `/tbos dungeon enter` | Teleports you into your active generated run. |
| `/tbos dungeon seed` | Prints the current run ID and seed. |
| `/tbos dungeon room` | Prints the room containing you and its state. |
| `/tbos dungeon regenerate` | Rebuilds your live dungeon from its persisted seed. |
| `/tbos dungeon remove` | Queues cleanup for your current dungeon. |
| `/tbos dungeon force_clear` | Force-clears your current room. |
| `/tbos dungeon unlock_all` | Opens all normal doors in your active run. |
| `/tbos dungeon spawn_template <template>` | Places a room-template preview below you. |
| `/tbos dungeon validate_templates` | Validates the registered room templates and descriptor. |
| `/tbos dungeon export_graph` | Writes the current dungeon graph JSON under `debug/tbos/dungeons`. |
| `/tbos dungeon boundaries` | Toggles the personal dungeon-boundary overlay. |
| `/tbos dungeon markers` | Toggles the personal dungeon-marker overlay. |

## Quick fixes

- **I cannot find a shrine:** reread the Archivist's Journal, or ask an operator
  to run `/tbos shrine locate`. A shrine only appears once its chunk is loaded,
  so keep travelling toward the reported coordinates even in an already-explored
  existing world. Operators can force every planned shrine with
  `/tbos shrine place_all`.
- **I cannot find the Archive:** right-click the Archive Survey Map again; it
  reports its coordinates in chat.
- **A cache will not open:** that is expected—clear the room and break the
  cache instead of right-clicking it.
- **The boss door is sealed:** finish the objective shown at the top left.
- **The Curator Gateway refuses entry:** hold the repaired Lens and keep the
  Curator Core in your inventory.
- **A development run needs a clean restart:** use `/tbos dungeon remove`, then
  `/tbos dungeon generate [seed]`.

For dungeon design and configuration details, see
[`echoes-of-the-past.md`](echoes-of-the-past.md).
