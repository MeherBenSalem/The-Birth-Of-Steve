# Architecture

- `Yesterglass`: minimal mod entry point.
- `registry`: deferred block, item, sound, particle, and data-component registration.
- `site`: temporal state machine, compact persistence, spatial lookup, and a
  versioned authored-site catalog. Each site persists a stable definition ID,
  origin, rotation, progress flags, transition timing, and deterministic seed.
- `network`: versioned and bounded C2S/S2C custom payloads.
- `item`: server-validated Lens interaction.
- `command`: guarded showcase, locate, reset, and developer commands.
- `client`: client-only effects, HUD, and render spike; never referenced from common
  dedicated-server classloading paths.
- `advancement`: server-owned story progression and showcase-chain reconciliation.
- `gametest`: idempotency, transition, range, authored-definition, codec, geometry,
  and reward-safety tests.
- `run`: the Echoes of the Past procedural Archive. It owns deterministic 3D graph
  generation, reusable transformed room schematics, instance allocation,
  tick-budgeted placement/cleanup, independent per-room encounters, weighted
  loot/enemy selection, secret discovery, member-specific checkpoints, debug
  overlays, and codec-backed run persistence.

The server owns physical and puzzle state. Network schema `2` broadcasts site ID,
definition ID, origin, authored transition center, rotation, progress flags, target
state, start tick, duration, and deterministic effect seed. Clients derive
animation progress and never submit puzzle completion.

Authored definitions store local-space bounds and semantic markers. Rotation is
applied at the definition boundary, so placement, chunk indexing, interaction
range, transition geometry, and safety checks all consume the same transformed
coordinates instead of duplicating world-space constants.

Hall dial orientations use three bounded two-bit fields in `progressFlags`; a
separate completion bit is monotonic during normal play. Interacting with a dial
mutates SavedData on the server, updates only that room's bounded mechanism/beam
geometry, and broadcasts a complete snapshot. Crouch-using the room's Memory
Anchor resets an unsolved configuration without creating a second state store.

Choir cursor, failure count, and completion use separate bounded fields in the same
persisted `progressFlags`. A per-dimension, per-site runtime tracker schedules the
Remembered demonstration only for active loaded Choir sites; it never scans the
world. The server owns bell flashes, pitch, imprint blocks, particles, overlays,
attempt reset, and completion, while normal site snapshots carry the durable state
needed by late joiners.

Broken Meridian uses the same durable `progressFlags` for its bounded relay index
and completion bit. Its definition contains exactly three object sockets and three
matching power-channel paths; interactions may only advance between those authored
positions. The server rejects an occupied destination before mutation, reapplies
all relay/channel/bridge geometry from the snapshot state, and includes every
collision-changing relay and crossing position in transition safety checks.

## Procedural Archive boundary

`ArchiveDungeonGraph` is the durable source of truth. `ArchiveRunGenerator` is a
pure seeded function over validated `ArchiveDungeonSettings`; it never touches a
world. `ArchiveRoomPlacer.blueprint` converts the graph into bounded block and
clear-volume operations. `ArchiveGenerationQueue` is the only normal generation
path that applies those operations, on the server tick with a shared mutation
budget.

`ArchiveRunSavedData` lives in overworld server SavedData so all Archive dimension
instances share one allocation index without sharing gameplay state. Run schema 4
persists graph schema 2, room/door/runtime state, independent encounter states,
member room/checkpoint/container/reward state, return points, seed, status, and
allocation. PREPARING runs are safely reconstructed after restart; active entities
use vanilla entity persistence plus run/room tags.

`ArchiveEncounterManager` resolves the player's containing room instead of using a
global linear cursor. This is what permits simultaneous party branches: each room
has its own encounter state and every member has a separate persisted checkpoint.
Only one durable transition is written per run per tick, avoiding stale-snapshot
overwrites while still progressing all occupied rooms fairly.

See `docs/echoes-of-the-past.md` for the complete lifecycle, configuration, and
operator surface.
