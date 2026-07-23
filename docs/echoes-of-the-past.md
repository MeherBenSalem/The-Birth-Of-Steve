# Echoes of the Past procedural dungeon

## Overworld entry

Three persistent Fracture Shrine variants are scattered at deterministic
world-seeded positions 192–640 blocks from world spawn. Their center markers are
dormant and cannot bypass the campaign. After the Last Curator is defeated, the
Grand Orrery's Archive Core transforms into the Curator Gateway. A player must
right-click it with a repaired Yesterglass Lens while carrying the Curator Core;
only then does the server allocate, construct, and enter a Fractured Archive
instance.

## Runtime shape

An Archive run owns one deterministic `ArchiveDungeonGraph`. The graph contains
7–48 room nodes, reciprocal six-direction connections, transformed template
metadata, difficulty, loot and encounter allowlists, modifiers, and durable room
state. Construction rejects non-cardinal edges, non-reciprocal doors, overlapping
room volumes, invalid mandatory-room indices, and any node unreachable from the
start.

The default generator produces 14–20 rooms. It always creates a start room, places
the final boss on a distant starting-floor branch, and seals a distinct reward
room behind that boss. Normal expansion may branch north, south, east, west, up,
or down. It supports dead ends and loops while enforcing horizontal, vertical,
depth, and above/below-count limits.

Twenty reusable code-native schematics cover every room category. Each template
defines dimensions, six-direction door support, monster/chest/direct-loot/trap/
decoration/puzzle/secret/boss/entry markers, allowed loot tables, historical enemy
groups, selection weight, and safe rotation/mirroring flags. The data-pack
descriptor is
`data/tbos/archive_room_templates/echoes_of_the_past.json`; the authoritative
validated marker geometry is `ArchiveRoomTemplates`.

`ArchiveRoomPlacer` transforms every door and marker with the selected room
transform. Every room receives a complete roof. Horizontal connections become
direct two-wide enclosed passages with floors, side walls, roofs, lighting, and
open headroom rather than exposed bridges. Vertical connections become enclosed
two-wide rising phase-stair tunnels with two clear blocks above every step and
their lock seal on the actual walkable cross-section. Hidden horizontal
connections begin as cracked walls.

## Server lifecycle

1. Entry captures every party member's exact return location and allocates one
   isolated 1536-block instance cell in the void Archive dimension.
2. The graph and `PREPARING` state are persisted before world mutation.
3. `ArchiveGenerationQueue` clears and places only a configurable number of
   blocks per server tick. It loads a chunk only when the cursor enters that
   chunk, skips already-correct air/blocks, and shares the budget fairly between
   simultaneous builds and cleanups.
4. Players teleport only after geometry is complete. A restart requeues an
   incomplete `PREPARING` run without duplicating its allocation.
5. Each occupied room advances independently. First entry persists visit and
   per-member checkpoint state. Combat/puzzle/trap rooms seal their actual routes,
   start a deterministic encounter, wait for tagged room enemies to be gone,
   release rewards, persist completion, and reopen reciprocal doors.
6. The final-boss entrance begins sealed. `Open the Last Recollection` requires
   clearing a graph-derived 60% of eligible rooms and every generated lesser
   warden. Completing it opens that entrance; the boss-to-reward route remains
   sealed until the Hour Cantor is defeated.
7. The top-left quest HUD shows cleared-room and lesser-warden progress, sealed/
   open state, and a gold completion sweep. Reduced-effects settings suppress the
   nonessential sweep animation.
8. Every generated Archive cache grants loot only when broken. Right-clicking
   only reads the seal. Combat-room caches remain bound until the room is clear;
   individual mode leaves a cache for members who have not claimed it, while
   shared mode removes it after the first valid claim. The final Cantor Cache
   additionally requires victory.
9. Death consumes the shared revive pool and returns only that player to their
   own persisted branch checkpoint. Victory, failure, abandonment, reconnect, and
   offline return paths are server-authoritative.
10. By default, terminal geometry and storage are removed after all members return.
   Operators may retain completed runs through config.

All world mutations, entity spawns, loot rolls, door changes, and SavedData writes
occur on the server thread. No global "current dungeon" singleton exists; runtime
maps are keyed by run, member, or allocation slot.

## Encounters, loot, and modifiers

Encounter composition is drawn from configurable weighted groups containing the
original Parallax Wraith, Meridian Sentinel, and Hour Cantor plus selected vanilla
monsters: husks, skeletons, strays, cave spiders, silverfish, vindicators, evokers,
and ravagers. Selection uses the room's historical group, seed, wave, size,
difficulty, and number of active players in that room. Health, damage, and enemy
count have separate depth/player scaling. Spawn markers must be in-room,
air-filled, floor-supported, and at least five blocks from every party member.

Skirmishes, hunts, guardians, puzzle waves, ambush-style entry waves, multi-wave
rooms, lesser wardens, the final boss, and trap hazards share the same durable
per-room encounter state. Depending on graph size, a run contains one, two, or
three random lesser-warden rooms. Their scaled Meridian Sentinel, vindicator,
evoker, or ravager bosses are weaker than the final Hour Cantor. Trap rooms
combine weighted enemies with timed marker hazards.

Each spawn also receives a deterministic mutation from its enemy kind and seed.
Skeleton-family enemies telegraph ranged Echo Bolts; heavy guardians release
delayed Meridian shockwaves; Parallax enemies blink behind valid targets;
lesser wardens project resistance auras; and spider/silverfish splitters create
exactly one bounded generation of children. Clear particles and sound cues precede
the damaging effects. Enemy deaths can release Echo Heart or Soul Heart potions,
Memory Coins, Archive Keys, Ash Bombs, or Soul Charges; lesser wardens always
release a pickup.

All nine modifiers have runtime behavior:

- Darkness, time distortion, and ancient curse apply bounded player effects.
- Reduced healing halves server healing events in the affected uncleared room.
- Reinforced enemies and ancient curse scale enemy attributes.
- Continuous waves adds a wave.
- Unstable floors and faster traps drive marker hazards at different cadences.
- Regenerating guardians grants regeneration to that room's spawned enemies.

Cache markers are selected independently per seeded room. Mandatory treasure,
secret, and reward rooms never omit their first cache. Deterministic container
treatments include ordinary, combat-locked, trapped, cursed, hidden, and boss
reward caches. Loot is evaluated from actual weighted Minecraft loot tables.
Common, secret, lesser-warden, and boss-reward pools mix Archive materials with
useful vanilla dungeon loot such as food, diamonds, emeralds, gold, iron,
experience bottles, arrows, and enchanted books. Direct room drops use the same
tables. Mandatory rolls have a safe nonempty fallback, individual/shared claims
are configurable, per-member claims persist, and the unique final reward is
exactly-once per member.

## Instance protection and visual language

Allocated dungeon bounds deny block breaking, block placement, and explosion
terrain damage. Generated Archive caches are the only break exceptions: room
caches can be broken after their room requirements are satisfied, and the final
Cantor Cache can be broken only after victory. The canceled block event
suppresses vanilla block drops while the persisted loot-claim path releases the
actual reward.

Combat doors are transactional: the server checks every party member's full
collision box against a padded seal volume, then writes the locked run state only
after the doorway is physically clear. A player crossing the threshold therefore
defers the close instead of being sealed inside a block. Vertical connectors use
actual stair collision, floor-level landings on both ends, and three blocks of
headroom.

Dungeon surfaces use a Minecraft-scale ancient-Archive pixel-art family rather
than realistic material scans. Archive bricks, weathered and mossed masonry,
chiseled stone, chronicle tile, and seeded floor/wall/trim/roof variants create
room-to-room variation. Lesser-warden rooms receive distinct accents. The final
room uses exclusive Cantor floor, wall, and rune blocks with a brighter rune cross
and pillars so it reads immediately as the run climax.

The Hall and Choir no longer report puzzle progress through the action bar. Their
glyph targets, stage/wave counters, progress, and failure count appear in a second
top-left objective card below the Cantor Seal quest. Correct input, mistakes, and
completion have distinct particles, sounds, color pulses, and a reduced-motion-
aware completion sweep.

## Secrets and navigation

Secret nodes never overlap another grid cell and are generated only on horizontal
connections, where a cracked wall can hide the route. Using the repaired Lens near
that wall reveals both reciprocal sides and persists discovery. The graph stores
visited, completed, locked, secret-discovered, and floor coordinates. Operators
can export its complete JSON representation; a player-facing minimap remains
optional and is not required for navigation.

## Server configuration

NeoForge writes the authoritative values to
`config/tbos-common.toml` under `echoesOfThePastDungeon`. Checked-in theme
defaults also live at
`data/tbos/archive_dungeon/echoes_of_the_past.json` and
`data/tbos/archive_encounters/echoes_of_the_past.json`.

The surface includes:

- room min/max, horizontal/vertical limits, maximum graph depth, maximum rooms
  above/below, branching, dead-end, loop, special, secret, trap, chest, direct
  loot, and modifier probabilities;
- allowed templates, per-template weights, boss template, loot-table weights, and
  weighted enemy pools;
- health/damage/active-player enemy scaling;
- combat door locking and individual/shared loot mode;
- forced or derived seed, debug logging, per-tick block budget, retry count,
  restart regeneration, and completed-run retention.

Invalid identifiers, enemies, weights, probabilities, mandatory-template
allowlists, and boss templates fail closed with a clear generation/config error.

## Administrative commands

All commands are under `/tbos dungeon` and require game-master permission:

- `generate [seed]`, `remove`, `regenerate`, `enter`
- `seed`, `room`, `force_clear`, `unlock_all`
- `spawn_template <id>`, `validate_templates`, `export_graph`
- `boundaries`, `markers`

Debug mode logs successful graph metrics and retry rejections, queued block counts,
template/door state, encounter group and composition, loot tables and seeds,
container treatment, missing safe markers, and generation failures.

## Platform scope

The Birth of Steve targets NeoForge 26.1.2 on Minecraft Java 26.1.2. Paper/Folia region
scheduling is therefore not an applicable runtime target. The placement queue
still preserves the equivalent safety invariant: background/pure graph work is
separate from world changes, and every world change occurs on Minecraft's server
thread.
