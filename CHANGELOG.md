# Changelog

## 0.1.0-alpha.1 - Unreleased

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
- Added a 400×400 project/mod icon, a 3:1 CurseForge concept banner, packaged
  NeoForge logo metadata, paste-ready CurseForge page copy, release-media
  disclosure, and a ModJam submission checklist.
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
