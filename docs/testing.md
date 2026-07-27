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

`runGameTestServer` covers the Archive creatures through `memory_leech_pounce`,
`lensward_contract`, `lensward_beam`, `lensward_line_of_sight`, `lensward_tether`,
`parallax_wraith_displacement`, `meridian_sentinel_slam`, and `hour_cantor_refrain`.
The last three assert that each signature move resolves exactly once per cooldown,
respects its radius, and returns the creature to idle.

### Flaky tests and their shared cause

Both known flakes come from the same mismatch: chunk loading is asynchronous and
therefore bound to wall clock time, while the assertions are counted in ticks. When
chunk work lags the tick counter, the world is not ready when the assertion fires.
Suite duration is **not** a reliable predictor — failures were observed across runs
from 15 s to 38 s.

`tbos:memory_leech_pounce` — **fixed in 0.2.0-alpha.3.** It laid its own floor
without force-loading the chunks, so `onGround()` never became true and the pounce
goal, which is gated on it, never fired. It now uses the shared `combatFloor`
helper, which force-loads first. Measured on 2026-07-26: it failed 6 of 9
`runGameTestServer` runs before the change and 0 of 5 after, including runs at
20.3 s and 25.7 s that had previously failed.

`tbos:orrery_interaction` — **still flaky, not a regression.** It fails at tick 45
with "did not reverse the arena toward Ruin". `TemporalSiteManager.hasLoadedChunks`
returns false, so `beginTransition` bails out, but `activateCuratorAnchor` returns
`true` regardless, so the test's earlier "interaction was rejected" assertion passes
while the site silently never leaves `REMEMBERED`. Reproduced on an unmodified
`main` at `d2e99e4` on 2026-07-26 — 1 of 8 consecutive runs there. Re-run the suite
before treating a failure in it as real.

Every other test in the namespace is stable, including all eight Archive creature
tests.

## Onboarding and shrine worldgen manual matrix

- Create a fresh world. Confirm the Archivist's Journal is granted once and the
  welcome and shrine hint appear in chat.
- Right-click the Journal. The quest screen must open directly, with a bronze
  frame, both tabs, quest seals and item icons rendering — no magenta, and no
  missing-sprite warnings in the log. Shift+right-click must open the same screen;
  the vanilla book interface should be unreachable by any input.
- Click between the Story and Run tabs. The active tab must read as joined to the
  panel and the inactive one as sunk, with a page-turn cue on each change. Hover
  each quest row and confirm the detail pane follows the pointer, then returns to
  the current step when the pointer leaves the list.
- Complete a story step and confirm its seal changes from keyhole to tick and the
  progress bar advances. Start a run and confirm the Run tab fills in its floor,
  revives, rooms, wardens and gateway state, refreshing about once a second.
- Resize the window small and confirm the panel clamps without the frame corners
  stretching or the tabs overlapping.
- Disconnect and rejoin, die and respawn, and travel to the Fractured Archive and
  back. None of these may grant a second Journal or repeat the welcome.
- Run `/tbos shrine list` in the fresh world: three shrines, all `PENDING`.
  Travel to one reported target and confirm the shrine builds as its chunk
  generates, with no visible tick spike or login stall, then that `list` reports
  it `GENERATED` and `/tbos shrine locate` gives a correct bearing.
- Run `/tbos shrine place curator_workshop`, confirm it builds at your feet, is
  discoverable, and that repeating it moves rather than duplicates the shrine.
- Use the Archive Survey Map in a world where no shrine has generated. It must
  report the Meridian Archive without building any shrine.
- Load a world saved before 0.2.0-alpha.2. Confirm temporal site data migrates
  from schema 3 to 4, previously built shrines survive, no duplicates are planned,
  and the existing player is greeted exactly once.

## Utility block visual matrix

- Use `/tbos showcase`, then inspect the Curator Gateway, Meridian Relay, Archive
  Core, Memory Lantern, and Alignment Dial. Each must show its own geometry and
  textures, animate, and emit its ambient particles. The relay must only arc while
  powered. Confirm no magenta/missing textures and no model warnings in the log.
- Confirm the Archive Core's inner core and the Alignment Dial's armillary rings
  animate, then enable `reducedMotion` in `config/tbos-client.toml` and confirm
  both settle while the blocks still render.
- Solve the Hall of Alignment and the Broken Meridian end to end. The dial and
  relay must behave identically to before the block-entity conversion.
- Check the five blocks' inventory icons and held models in the creative tab.

## Archive creature matrix

Unverified until run and dated below.

- `/summon tbos:parallax_wraith`, `/summon tbos:meridian_sentinel`, and
  `/summon tbos:hour_cantor` on flat ground. Each must render its own silhouette —
  no zombie proportions — with no magenta textures and no model warnings in the
  log. Confirm the emissive cores glow in darkness: the wraith's core and mask
  slit, the sentinel's gnomon and gear teeth, the Cantor's metronome, crown, and
  hour marks.
- Check the double-density textures at melee range. No face may look stretched,
  seamed, or half-resolution — a wrong `texScale` shows up as one box sampling at
  the wrong size rather than as a missing texture, so this needs eyes on each
  creature rather than a clean log.
- Confirm light falls from above: tops of boxes lit, undersides dark. An earlier
  build had this inverted.
- Confirm the Sentinel reads as riveted bronze plate and the Cantor's robe as
  hanging parchment. Neither may read as wooden planking.
- Let each acquire you and confirm the full signature move plays, with the
  telegraph strictly *before* the damage: wraith plates scatter and reform around
  the teleport, sentinel mauls reach full height before the slam lands, Cantor
  rings open through all four conducting strokes before the refrain releases.
- Damage each one and confirm the hurt animation, hurt sound, and ambient sound.
  None of the three may play zombie audio.
- Take the Cantor below half health. The pendulum swing and the refrain cadence
  must visibly tighten and the rings must tilt steeper, while the boss bar still
  tracks health correctly.
- Submerge a wraith and a sentinel. Neither may convert to a Drowned. Stand a
  villager and an iron golem nearby; neither may be targeted.
- In a live run, fight a wave containing wraiths and sentinels. A wraith must
  never blink mid-fracture, and delayed shockwaves must come only from husks,
  vindicators, and ravagers. Confirm the slam and refrain never damage other
  Archive monsters.

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
  `tbos:fractured_archive`. Clear floors 0–7 once each and confirm the eight
  themes read differently in palette, hazard telegraph, and exclusive enemy
  silhouettes; Echo II should reuse theme identity. Confirm only failure or
  abandonment returns members to their captured Overworld positions.
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

## Automated results — 2026-07-27

- `gradlew.bat clean build --console=plain`: PASS. The 1,000-seed simulation ran
  through `check` with zero failures across every counter. Artifact:
  `build/libs/tbos-0.2.0-alpha.4.jar`.
- `gradlew.bat runGameTestServer --console=plain`: 54 required tests, run four
  times. Three were clean at 54/54; one hit the known `orrery_interaction` flake
  documented above, which reproduces on unmodified `main` and is unrelated to this
  change.
- `python tools/textures/archive_entities.py`: PASS, six 128×128 sheets, UV
  `validate()` clean. `python tools/textures/archive_gui.py`: PASS, nine sprites.
- Sprite metadata: all six nine-slice `.png.mcmeta` files parse, declare
  dimensions matching their PNG, and have borders smaller than the sprite.
- `gradlew.bat runClient`: the client reached the main menu and stitched
  `minecraft:textures/atlas/gui.png-atlas` with the `tbos:journal/*` sprites
  present in the resource set and **no** texture, sprite, atlas or model errors in
  the log.
- **Unverified, needs a person at the keyboard:** how the creatures and the
  Journal screen actually look. The Archive creature matrix and the Journal rows
  in the onboarding matrix above are the checklist for that pass. A wrong
  `texScale` or a mis-sliced frame renders without logging anything.

## Automated results — 2026-07-26

- `gradlew.bat clean build --console=plain`: PASS. The 1,000-seed simulation ran
  through `check` with zero failed generations, zero unreachable rooms, zero
  overlapping volumes, zero lesser-boss mismatches, and zero quest-gate
  violations. Artifact: `build/libs/tbos-0.2.0-alpha.3.jar`.
- `gradlew.bat runGameTestServer --console=plain`: 54 required tests, run
  fourteen times. The last five runs, after the `memory_leech_pounce` chunk fix,
  were clean at 54/54. The three new creature tests —
  `parallax_wraith_displacement`, `meridian_sentinel_slam`, `hour_cantor_refrain`
  — passed in all fourteen.
- Baseline comparison: eight `runGameTestServer` runs on unmodified `main` at
  `d2e99e4` reproduced both known flaky failures, confirming neither belongs to
  the creature rewrite.
- Model geometry: all three new entity models were checked for rest-pose volume
  overlaps between parts, since coplanar faces z-fight in world. Four real
  overlaps were found and corrected before release.
- The Archive creature manual matrix above is **unverified**; it needs a client
  session.

## Automated results — 2026-07-23

- `gradlew.bat runGameTestServer --stacktrace --console=plain`: PASS, all 47
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
  `build/libs/tbos-0.2.0-alpha.1.jar`.
- `gradlew.bat runData --no-daemon`: PASS.
- Standards `JSON.parse`: PASS, all 157 checked-in JSON resources.
- Texture/model validation: PASS. All 32 item/block textures are 16×16 or 32×32
  (24 at 16×16 and 8 at 32×32), and no item model retains the stretched
  `minecraft:item/handheld` parent.
- The final GameTest server run shut down cleanly while a separate development
  client remained open.
