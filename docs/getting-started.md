# Starting The Birth of Steve

This is the player guide for **The Birth of Steve**. You do not need commands
or cheats to begin the adventure.

## Install and launch

1. Install **Minecraft Java Edition 26.1.2** and **NeoForge 26.1.2.83 or newer**
   for that Minecraft version.
2. Put `tbos-0.1.0-alpha.1.jar` in the instance's `mods` folder.
3. Launch NeoForge and create or open an Overworld save. For multiplayer, put
   the same JAR on both the server and every client.
4. Because this is an alpha, make a backup before updating an existing world.

No other mod is required. Server owners can tune dungeon settings in
`config/tbos-common.toml`.

## Your first expedition

Each world places three Fracture Shrine variants at persistent, world-seeded
locations scattered 192–640 blocks from world spawn. Their positions do not
depend on which player joins first. Explore for a shrine, break its Fracture
Coffer, recover the **Cracked Yesterglass Lens**, and gather its repair
materials. Repair the lens, then use the **Archive Survey Map** by right-clicking
it: it reports the direction, distance, and coordinates of the Meridian Archive.

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
| `/tbos run complete` | Forces the current active run into its victory return. |
| `/tbos run abandon` | Forces the current active run into its failure return. |
| `/tbos debug give_cracked_lens` | Gives a Cracked Yesterglass Lens. |
| `/tbos debug give_survey_map` | Gives an Archive Survey Map. |
| `/tbos debug place_shrines` | Places or reports the three Fracture Shrines near you. |
| `/tbos debug give_memory_kit` | Gives the Memory Lantern and every Memory Plate. |

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

- **I cannot find a shrine:** an operator can run `/tbos debug place_shrines`.
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
