# Changelog

## 0.2.0-alpha.2 - 2026-07-26

### Onboarding

- Added the **Archivist's Journal**, a nine-page in-game guidebook covering
  shrine discovery, the Lens repair, the Survey Map, the five authored rooms,
  the Last Curator, the Curator Gateway, and the rules of Echoes of the Past.
  Right-click to read it; it uses the standard book interface.
- Every player now receives the Journal, a short welcome, and a shrine hint the
  first time they join a world's Overworld. The greeting is recorded per player
  in world data, so it survives death, dimension changes, and server restarts,
  and never fires twice.

### Fracture Shrines

- Shrines are now built as the world generates around them instead of all three
  at once during the first login. Their three seeded locations are unchanged and
  still fixed per world seed; each one is constructed only when its own chunk
  loads, and the build is deferred to the following server tick so chunk loading
  is never re-entered.
- The shrine plan is persisted, so a world keeps the same three sites even if
  those chunks are never visited. Temporal site data moved to schema 4; older
  saves migrate in place and re-derive their plan from the world seed, keeping
  any shrines they had already built.
- The Archive Survey Map no longer forces three distant shrine regions into
  existence. It now anchors the Meridian Archive on the seeded plan instead.
- Added `/tbos shrine locate`, `/tbos shrine list`, `/tbos shrine place [variant]`
  to force-build a shrine at your position, and `/tbos shrine place_all`. All
  four require game-master permission. `/tbos debug place_shrines` still works.
- Added `/tbos debug give_journal`.

### Utility block overhaul

- Rebuilt the Curator Gateway, Meridian Relay, Archive Core, Memory Lantern, and
  Astronomical Alignment Dial. All five were texture-swapped vanilla cubes reusing
  other blocks' textures; each now has bespoke multi-part geometry and its own
  textures.
- The Curator Gateway is now a real arch — a base and cap lintel with four bronze
  jambs and the veil suspended in the opening — rather than a hollow cube.
- Added animated textures for the gateway's veil, the relay's charged coils, the
  Archive Core's iris, the lantern's glass, and the dial's drifting glyphs.
- Authored the Archive block and item art to a final, shippable standard on one
  shared palette: 16×16, hard pixel edges, consistent top-left lighting, and
  animated frames quantised onto discrete ramps so they stay legible at block
  scale. These textures are owned original project art, no longer placeholders,
  and `tools/textures/archive_blocks.py` is their authoring source.
- Fixed z-fighting across the reworked blocks. Every model's elements were
  overlapping in volume; they now meet only at edges.
- Fixed the Archive Core, Meridian Relay, and Alignment Dial rendering as x-ray
  holes through the floor. Their models are no longer full cubes, so all three
  now declare `noOcclusion()` and neighbouring blocks stop culling against them.
- Gave the Meridian Relay and Alignment Dial full-width footings so neither reads
  as floating above its block.
- Added ambient particles: the gateway pulls portal motes inward, a powered relay
  arcs sparks around its gimbal, the Archive Core draws enchantment glyphs in, the
  lantern releases slow cyan motes, and the dial traces sparks along its ring.
- Added block-entity renderers for the Archive Core, whose inner core now spins
  and precesses inside its slotted housing, and the Alignment Dial, which gained
  two counter-rotating armillary rings and a hovering glyph. Both animations are
  cosmetic and read no puzzle state. Both stop when `reducedMotion` is enabled.
- A dormant Memory Lantern now holds one drifting mote instead of appearing inert.

## 0.2.0-alpha.1 - 2026-07-25

- Replaced the Archive victory return with endless floor progression. Runs begin
  at floor 0 and advance through floor 1, floor 2, and onward without a normal
  victory teleport to the Overworld.
- Every cleared floor receives a new deterministic seed, wholly new dungeon
  graph, and a freshly allocated isolated instance in the Fractured Archive.
- Preserved party identity, original return points, inventory, and remaining
  shared revives between floors while resetting floor-local rooms, checkpoints,
  encounters, container claims, and rewards.
- Persisted the unbounded floor counter and all retired-floor cleanup descriptors
  in Archive SavedData so generation and cleanup recover safely after restart.
- Teleport players directly to the new floor only after staged construction is
  complete, then delete every retired layout through the tick-budgeted cleanup
  queue and release its allocation slot.
- Fixed a server-tick divide-by-zero crash when generation completed and created
  the first retired-floor cleanup task in the same tick. The handoff now verifies
  a solid entrance floor and two blocks of spawn headroom before teleporting, and
  cleanup starts only after no online player remains in the retired instance.
- Updated Archive entry/status messages and replaced the former victory-return
  GameTest with floor-progression, fresh-layout, state-preservation, and codec
  coverage.

## 0.1.0-alpha.4 - 2026-07-24

- Added a dedicated **The Birth of Steve** creative tab that automatically
  includes every registered item and all authored Memory Plate variants.
- Added 38 graveyard props with block items, loot sidecars, directional
  placement, and crisp held-item models.
- Removed the half-stair floor replacements that exposed void-colored gaps;
  weathered room-floor variation now always uses full blocks.
- Extended vertical stair runs by one lower step and lowered their four-block
  side walls to seal the remaining staircase gaps.
- Made interior room dressing and props breakable during active Archive runs
  while keeping generated walls and floors protected.
- Removed roughly 440 milliseconds of leading silence from the crate-break cue
  so its impact starts when the crate disappears.
- Reworked the eight standalone item sprites into crisp hard-alpha textures and
  explicitly disabled texture blur.
- Removed generated presentation artwork and its packaged mod-logo metadata;
  project media is now limited to manually captured gameplay or separately
  commissioned original artwork.

## 0.1.0-alpha.3 - 2026-07-23

- Rebuilt Archive room dressing around an abandoned-civilization theme with
  deterministic weathered floor, wall, and ceiling patches; broken pillars;
  rubble; iron-barred relic alcoves; candle shrines; dense cobweb growth;
  ancient masonry; chains; lanterns; shelves; pots; and category-specific props.
- Increased each non-reward room to several breakable crate and barrel clusters
  while preserving clearance around entrances, stairs, encounters, puzzles,
  loot markers, bosses, and traversal routes.
- Replaced approximately 10% of every generated room floor with randomly
  oriented Archive or tuff-brick stairs to create walkable broken-floor relief,
  excluding protected gameplay tiles and doorway approaches.
- Fixed the supplied mixed crate/barrel model's unresolved barrel texture and
  added missing particle texture mappings so the new props no longer render
  magenta or emit missing-model warnings.

## 0.1.0-alpha.2 - 2026-07-23

- Added nine breakable Archive crate/barrel props from the supplied crate asset
  pack. They now appear deterministically across dungeon rooms as protected-run
  exceptions, use the supplied break SFX, and have optional loot rolls using the
  room's existing weighted dungeon loot tables.

## 0.1.0-alpha.1

- Integrated the Fractured Archive into normal progression: Fracture Shrines now
  use persistent world-seeded positions instead of the first player's location,
  their centers remain dormant, and defeating the Last Curator transforms the
  Grand Orrery's Archive Core into the real Curator Gateway. Entering now
  requires a repaired Yesterglass Lens in hand and the recovered Curator Core.
- Made every generated Archive loot cache break-to-claim. Right-click now only
  reads the seal; shared caches disappear on the first valid break, individual
  caches persist until every member claims them, and loot visibly drops at the
  cache without bypassing protected-instance rules.
- Added distinct event audio for wave starts, room clears, released doors,
  ordinary/locked/trapped/cursed/hidden caches, and the final Cantor Cache.
- Added paste-ready CurseForge page copy, media guidelines, and a ModJam
  submission checklist.
- Renamed the mod to **The Birth of Steve**, moved all Java sources under
  `com.nightbeam.tbos`, and migrated the runtime namespace, assets, data,
  configuration, command root, reports, and artifact ID to `tbos`.
- Prevented encounter seals from closing through a player by deferring the
  physical and persisted lock until every party hitbox clears the doorway.
- Rebuilt vertical connectors with Minecraft stair collision, full lower/upper
  landings, enclosed roofs, and three blocks of verified headroom.
- Added deterministic enemy mutations: telegraphed echo bolts, shockwaves,
  parallax blinks, protective ward auras, and bounded one-generation splitters.
- Added Isaac-inspired combat pickups using useful vanilla items: Echo/Soul
  healing potions, Memory Coins, Archive Keys, Ash Bombs, and Soul Charges.
- Replaced procedural Hall/Choir action-bar instructions with a stacked,
  reduced-motion-aware objective HUD featuring glyph progress, failure pulses,
  combat stages, and completion sweeps.
- Replaced the fixed linear Archive run with Echoes of the Past: a deterministic
  7–48-node, six-direction, multi-floor graph with branches, loops, secrets,
  guaranteed distant boss and sealed reward rooms, and strict reachability/
  overlap validation.
- Added twenty reusable, safely transformed room schematics covering all sixteen
  gameplay categories, functional horizontal and vertical doors, marker-driven
  encounters/loot/traps/puzzles, and tick-budgeted placement and cleanup.
- Added configurable weighted enemy pools and Minecraft loot-table rolls, nine
  functional difficulty modifiers, per-active-player scaling, shared/individual
  loot rules, per-member branch checkpoints, restart-safe state, and exactly-once
  final artifacts.
- Added the complete `/tbos dungeon` administration/debug suite, generated
  server configuration, data-pack defaults, graph export, visible boundaries and
  markers, an expanded GameTest contract, and a build-gating 1,000-seed
  simulation with zero overlaps and zero unreachable rooms.
- Initialized the NeoForge 26.1.2 / Java 25 project.
- Implemented the Milestone 1 16×16 temporal reconstruction spike foundation.
- Added codec-backed temporal site persistence, chunk indexing, interruption
  reconciliation, bounded phase geometry, compact versioned payloads, late-join
  snapshots, Lens validation, protected geometry, and guarded showcase commands.
- Added a deterministic radial particle-segment fallback with client quality and
  reduced-motion settings.
- Added twenty-two passing required GameTests and Java 25 CI.
- Added a versioned, rotation-aware authored-site definition model with persisted
  definition identity, orientation, and progress flags.
- Built the first Parallax Atrium shell with cracked masonry, meridian floor detail,
  a reconstructing fourteen-block staircase, and four remembered-state lamps.
- Added the cracked Lens onboarding item and repair recipe, plus the first four
  story advancements and a guarded debug command for acquiring the cracked Lens.
- Expanded coverage with site-definition validation, rotated marker transforms,
  and site-codec round trips.
- Added the connected Hall of Alignment: three discrete astronomical dials, three
  directional engraved targets, solid Yesterglass beam feedback, an in-world
  reset mechanism, persistent completion, and a Ruin-only reward crossing.
- Expanded site snapshots to schema `2` with authored definition, rotation,
  transition-center, and progress metadata for multiplayer late joins.
- Added the Hall story advancement plus Engraved Meridian Tile, Yesterglass, and
  Astronomical Alignment Dial blocks.
- Added the connected Choir of Hours with four patterned resonant bells, bounded
  server-timed demonstration playback, light/pitch/imprint/text redundancy,
  persisted sequence progress, safe reset and stronger two-failure hint, a
  Ruin-only reward crossing, and its story advancement.
- Added the connected Broken Meridian: alternating remembered/Ruin crossings, a
  three-position server-authoritative Meridian Relay, readable powered channels,
  authored causality, occupied-destination safety, anchor reset, fall recovery,
  cracked future remains, and its story advancement.
