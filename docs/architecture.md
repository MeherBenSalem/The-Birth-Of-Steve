# Architecture

## Repository layout

The mod is written once and shipped for two loaders across several Minecraft
versions.

```
shared/                 the mod: mod.properties plus common/, fabric/, neoforge/
26.1.2/                 one Minecraft target: build scripts, build-logic, overrides/
26.2/                   another target, same shape
settings.gradle         the composite that includes every version folder
```

`shared/` is the only place mod code lives. Each version folder is a **complete,
independent Gradle build** with its own Loom and ModDevGradle versions, so a new
Minecraft target can move its loader plugins without disturbing the others. The
root build is a composite that fans `build` out to all of them; `gradlew -p 26.2
<task>` drives one target directly.

Mod identity (`version`, `group`, `mod_id`, description) is declared once in
`shared/mod.properties`; only Minecraft and loader versions live in
`<version>/gradle.properties`. Neither can drift.

### Overrides

A file at `<version>/overrides/<module>/src/main/<java|resources>/<path>` replaces
its `shared/` twin for that Minecraft version only. `multiloader-common.gradle`
excludes the shadowed shared copy from compilation, javadoc, the sources jar and
resource processing — including in the loader projects, which compile `:common`'s
source directories directly — so a duplicate class is impossible.
`gradlew -p <version> sourceOverrides` prints what is currently in effect.

Overrides are for the small deltas a Minecraft version genuinely forces, not for
forking the mod. Where a difference would otherwise spread through large files,
route the call through the compat seam instead: `compat.VanillaCompat` (server)
and `client.ClientCompat` (client) are ordinary shared classes whose *whole body*
each version may replace. 26.2 uses exactly these two overrides to absorb
`EntityType`'s constants moving to `EntityTypes`, `LivingEntity.knockback` gaining
a damage source, and `Gui` splitting into `Gui` + `Hud`.

## Modules

- `common`: nearly the whole mod, compiled against vanilla Minecraft only. It has
  no knowledge of NeoForge or Fabric.
- `neoforge`: the `@Mod` entry points plus adapters onto the NeoForge event bus,
  payload registrar, and `DeferredRegister`.
- `fabric`: the `ModInitializer`s plus Fabric API callbacks, and four mixins for
  the hooks Fabric API does not expose (heal scaling, explosion block filtering,
  block placement, and null-`DataFixTypes` saved data).

Common reaches a loader only through `platform.Services`, which resolves
`IPlatformHelper`, `IRegistryHelper` and `INetworkHelper` from `META-INF/services`.
Loader projects may use everything in common; common may use nothing from them.
The practical consequence for every new feature: write it in `common`, and only
add to a loader project when the vanilla API genuinely cannot express it.

Event handlers in common take vanilla parameters and return a verdict
(`allowBreak`, `allowPlace`, `allowDeath`, `scaleHeal`, `filterExplosion`) so that
one implementation serves a cancellable NeoForge event and a boolean-returning
Fabric callback alike.

## Packages

- `Yesterglass`: common bootstrap; `TbosNeoForge` and `TbosFabric` are the real
  entry points.
- `platform`: the loader seam — `Services`, the `Registrar`/`RegistryEntry`
  abstraction, and the three service interfaces.
- `registry`: deferred block, item, sound, and data-component registration. Each
  `Mod*` class fills `ModRegistries` from a static initialiser; the loader flushes.
- `site`: temporal state machine, compact persistence, spatial lookup, and a
  versioned authored-site catalog. Each site persists a stable definition ID,
  origin, rotation, progress flags, transition timing, and deterministic seed.
- `network`: versioned and bounded C2S/S2C custom payloads.
- `item`: server-validated Lens interaction.
- `command`: guarded showcase, locate, reset, shrine, and developer commands.
- `world`: Overworld adventure provisioning. It derives the deterministic
  three-variant Fracture Shrine plan from the world seed, materializes each shrine
  when its own chunk generates, anchors the Meridian Archive on that plan, and
  grants first-join onboarding.
- `blockentity`: block-entity types. The Memory Lantern persists its scene and
  playback; the Archive Core and Alignment Dial anchors are stateless and carry no
  NBT, existing only so a renderer can attach.
- `entity`: the five original creatures. Each is a `Monster` subclass owning one
  signature move as a phase enum plus a phase-tick counter in `SynchedEntityData`,
  driven by a single non-interruptable `Goal`. The server resolves every effect;
  the client only reads the phase and its progress. A move in flight is reset to
  idle on load rather than resumed.
- `client`: client-only effects, HUD, and render spike; never referenced from common
  dedicated-server classloading paths.
- `client/render`: block-entity renderers plus one `EntityModel`, `RenderState`, and
  `MobRenderer` per creature. Animation is procedural inside `setupAnim`: continuous
  motion from `ageInTicks` and the walk animation, signature motion from the phase
  and progress carried on the render state. `ArchiveEmissiveLayer` is the shared
  full-bright overlay for their glowing cores.
- `advancement`: server-owned story progression and showcase-chain reconciliation.
- `gametest`: idempotency, transition, range, authored-definition, codec, geometry,
  and reward-safety tests.
- `run`: the Echoes of the Past procedural Archive. It owns deterministic 3D graph
  generation, reusable transformed room schematics, instance allocation,
  tick-budgeted placement/cleanup, independent per-room encounters, weighted
  loot/enemy selection, secret discovery, member-specific checkpoints, debug
  overlays, endless floor progression, and codec-backed run persistence.

The server owns physical and puzzle state. Network schema `2` broadcasts site ID,
definition ID, origin, authored transition center, rotation, progress flags, target
state, start tick, duration, and deterministic effect seed. Clients derive
animation progress and never submit puzzle completion.

Authored definitions store local-space bounds and semantic markers. Rotation is
applied at the definition boundary, so placement, chunk indexing, interaction
range, transition geometry, and safety checks all consume the same transformed
coordinates instead of duplicating world-space constants.

Fracture Shrine generation is split into a plan and a build. `AdventureWorldManager`
derives one target per variant from the world seed and persists it; nothing is
written to the world at that point. A chunk-load callback that covers an unbuilt
target only enqueues it into `FractureShrineQueue`, because chunk loading must not
be re-entered from inside a load callback. The server tick then drains at most one
shrine per tick, resolving its dry surface and recording the placement. Temporal
site SavedData is schema `4`; a save without a persisted plan re-derives it from
the seed and keeps whatever shrines it had already built.

Block-entity renderers for the Archive Core and Alignment Dial are cosmetic. They
read world time and, for the dial, its facing — never puzzle state. That keeps
`progressFlags` the single store for Hall dial orientation, so no visual blockstate
property can drift away from the authoritative value.

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
instances share one allocation index without sharing gameplay state. Run schema 5
persists graph schema 2, the visible floor number, retired-floor cleanup
descriptors, room/door/runtime state, independent encounter states, member
room/checkpoint/container/reward state, return points, seed, status, and allocation.
PREPARING floors and retired cleanup reservations are safely reconstructed after
restart; active entities use vanilla entity persistence plus run/room tags.

Minecraft's datapack registry owns the single `tbos:fractured_archive` dimension.
Each endless floor is therefore a fresh, isolated dimension instance cell rather
than a runtime-registered dimension key. The next cell is fully generated before
the direct Archive-to-Archive teleport; only then may the previous cell enter
staged deletion.

`ArchiveEncounterManager` resolves the player's containing room instead of using a
global linear cursor. This is what permits simultaneous party branches: each room
has its own encounter state and every member has a separate persisted checkpoint.
Only one durable transition is written per run per tick, avoiding stale-snapshot
overwrites while still progressing all occupied rooms fairly.

See `docs/echoes-of-the-past.md` for the complete lifecycle, configuration, and
operator surface.
