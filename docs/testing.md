# Testing

## Automated commands

```text
gradlew.bat clean build
gradlew.bat runData
gradlew.bat runGameTestServer
gradlew.bat runDungeonSimulation
```

`clean build` also runs the 1,000-seed dungeon simulation through Gradle's
`check` lifecycle.

## Echoes of the Past manual matrix

- In a fresh world, confirm all three Fracture Shrine locations are persistent,
  scattered 192–640 blocks from world spawn, and unchanged when a different
  player joins first. Their center markers must not start an Archive run.
- Complete the full authored route and defeat the Last Curator. Confirm the Grand
  Orrery's Archive Core visibly transforms into the Curator Gateway with portal
  particles and sound. Empty-hand use, a cracked Lens, a repaired Lens without
  the Curator Core, and using another block must not enter the dimension.
- Carry the Curator Core, hold the repaired Lens, and right-click the gateway.
  Confirm the run is constructed, the nearby party is teleported into
  `tbos:fractured_archive`, and every member returns to their captured Overworld
  position after victory, failure, or abandonment.
- Generate a fresh run after this overhaul. Existing generated instances retain
  their old placed geometry and cannot prove the new roofs, corridors, stair
  tunnels, or final-room palette.
- Start solo runs at each difficulty, confirm room-count/depth bounds, finish the
  quest and boss, break the final Cantor Cache for its reward once, exit, and
  restart from a new seed.
- Use two real clients to split at a branch, clear rooms independently, die and
  re-enter at each member's own checkpoint, disconnect/reconnect, and restart the
  server while both players are in different rooms.
- Inspect every room category, all four rotations, complete room ceilings, direct
  enclosed passages, two-way vertical stair tunnels, secret walls,
  locked/trapped/cursed/hidden caches, vanilla/custom enemy mixes, lesser wardens,
  and every room modifier.
- Verify combat doors cannot be bypassed horizontally or vertically and that the
  final-boss entrance stays sealed until `Open the Last Recollection` reaches its
  room-clear and lesser-warden targets. Verify its completion sweep appears in the
  top-left HUD and the gate opens. The boss-to-reward route must remain sealed
  until the Hour Cantor is defeated.
- Try breaking and placing throughout an allocated instance, including with
  explosions. All world edits must be denied except breaking the final Cantor
  Cache after victory. Right-clicking that cache must not grant loot.
- Compare individual and shared loot modes; verify ordinary caches include useful
  vanilla dungeon loot and that opened caches/final rewards cannot be duplicated
  after death, reconnect, regeneration, or server restart.
- Run multiple parties concurrently and confirm their allocated instance cells do
  not intersect. Finish or abandon runs and verify configured cleanup behavior.
- Profile generation with the configured per-tick block budget while other players
  explore loaded chunks; watch server tick time and memory during repeated runs.
- Exercise reduced effects, minimal particles, high-contrast/readability settings,
  GUI scales, and low/high render distances.
- Use `/tbos dungeon generate [seed]`, `export_graph`, `regenerate`,
  `force_clear`, `remove`, and the other documented dungeon commands as an
  operator.

Recorded automated assertions cover the deterministic equivalents of these cases;
visual readability, two real clients, and production-server profiling remain
manual sign-off items.

## Echoes overhaul client evidence — 2026-07-23

- PASS: the NeoForge client is running on Minecraft 26.1.2 and loaded the updated
  item/block atlases without a matching Yesterglass model, texture, or resource
  error. Corrected item silhouettes and the new pixel-art material family were
  visible in the existing test world.
- NOT APPLICABLE TO THE OLD INSTANCE: that world was generated before the geometry
  overhaul, so its exposed ceilings and old connections are persisted historical
  blocks rather than evidence about current generation.
- PENDING FRESH-RUN VISUAL SIGN-OFF: roofs, direct enclosed passages, full stair
  headroom, final-room distinction, quest-HUD completion animation, door timing,
  break protection, break-only Cantor Cache behavior, and live vanilla/custom
  wave composition. The running client can be used for this check by creating a
  new run/seed.
- PENDING MULTIPLAYER SIGN-OFF: simultaneous branches, individual final-cache
  claims, reconnect/restart behavior, GUI-scale/reduced-effects presentation, and
  production tick/memory profiling.

## Milestone 1 manual matrix

- Fresh single-player world and repeated ten-transition test.
- Dedicated server boot and two-player simultaneous activation.
- Join during transition; disconnect; save/quit and restart during transition.
- Chunk unload/reload and death within the spike site.
- Reduced effects and minimal particles.
- Unsafe occupancy of every target phase block.
- Resource reload, GUI scale changes, and low/high render distance.

Results remain unverified until recorded with the exact command or test
procedure.

## Manual results — 2026-07-22

- `gradlew.bat runClient --stacktrace --console=plain`: PASS for interactive client
  startup on Minecraft 26.1.2 with NeoForge 26.1.2.83.
- The author loaded the single-player showcase, used the Lens, and confirmed that
  the reconstruction interaction and presentation work as intended.
- The client session then exited normally; the integrated server saved the
  overworld, Nether, and End, and `runClient` ended with `BUILD SUCCESSFUL`.
- This confirmation does not yet cover the dedicated two-client, interrupted
  save/reload, or repeated ten-transition profiling cases.

## Automated results — 2026-07-23

- `gradlew.bat runGameTestServer --stacktrace --console=plain`: PASS, all 42
  required tests. The suite covers world-seeded Shrine placement, dormant Shrine
  centers, Curator Gateway transformation, Lens/Core entry validation, the
  authored phase sites, and dungeon generation
  bounds, reachability, overlap rejection, complete room roofs, direct enclosed
  horizontal passages, enclosed two-wide vertical stair tunnels with clear
  headroom, transformed doors/markers, quest-locked final-boss progression,
  derived lesser-warden counts, vanilla/custom encounters, instance-protection
  decisions, loot, party scaling, cache claims, serialization/restart, split
  checkpoints, death/re-entry, and exactly-once rewards.
- The dungeon contract samples exact minimum and maximum configurations and 64
  deterministic seeds in the in-game server environment.
- `runDungeonSimulation` is the large topology gate: 1,000 deterministic seeds
  must produce zero generation failures, unreachable rooms, and overlapping room
  volumes, zero lesser-boss count mismatches, and zero pre-completed/unlocked
  final quests. Its machine-readable report is written to
  `build/reports/tbos/archive-dungeon-simulation.json`.
- `gradlew.bat clean build --stacktrace --console=plain`: PASS. The build includes
  the 1,000-seed simulation through `check`; the regenerated artifact is
  `build/libs/tbos-0.1.0-alpha.4.jar`.
- `gradlew.bat runData --no-daemon`: PASS.
- Standards `JSON.parse`: PASS, all 157 checked-in JSON resources.
- Texture/model validation: PASS. All 32 item/block textures are 16×16 or 32×32
  (24 at 16×16 and 8 at 32×32), and no item model retains the stretched
  `minecraft:item/handheld` parent.
- The final GameTest server run shut down cleanly while a separate development
  client remained open.
