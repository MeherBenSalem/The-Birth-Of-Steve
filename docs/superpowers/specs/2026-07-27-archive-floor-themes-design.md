# Archive Floor Theme Variety (v0.3.0)

## Goal

Give every Echoes of the Past floor a distinct visual identity, exclusive custom
monsters (unique models and signature behaviors), themed block palettes, and
theme hazards, while preserving endless floor progression and the shared core
enemy roster.

## Decisions

- **Scope:** All eight cycling floor names (`ArchiveFloorPresentation.NAME_COUNT`).
- **Roster depth:** Two exclusive custom mobs per theme (16 total). A third slot
  per theme is reserved for a later patch.
- **Shared core:** Memory Leech, Lensward, Parallax Wraith, and Meridian Sentinel
  remain in every theme’s combat pools alongside selected vanilla fillers. Hour
  Cantor remains the final boss on every floor.
- **Environment:** Theme hazards and props only. Mobs fight with their own
  signature abilities; they do not read or mutate theme terrain.
- **Approach:** Code-native `ArchiveFloorTheme` catalog keyed by
  `nameIndex(floorIndex)`, same authority boundary as `ArchiveRoomTemplates` and
  `ArchiveDungeonRules`. JSON descriptors stay operator-facing.
- **Persistence:** Theme is derived from floor index. No run or network schema bump.
- **Echo cycles:** Reuse the same theme identity; existing difficulty caps still apply.

## Architecture

```
floorIndex → nameIndex (mod 8) → ArchiveFloorTheme
  ├─ RoomPalette (floor / wall / roof / trim)
  ├─ Hazard family + dressing rules
  └─ Exclusive ArchiveEnemyKind weights (merged into encounter pools)
```

`ArchiveRoomPlacer` selects palette and places hazards from the active run’s
theme, while category accents (final Cantor set, mini-boss chronicle, secret /
trap weathering) still override inside those rooms.

`ArchiveEncounterManager.planWave` / `ArchiveDungeonRules.chooseEnemy` resolve
the theme from `floorIndex` and merge shared core pools with theme exclusives.
Exclusives never appear off-theme.

## Theme catalog

| Index | Floor name | Accent | Exclusives | Hazard |
| --- | --- | --- | --- | --- |
| 0 | The Parallax Wake | indigo / cyan | Shard Drifter, Wake Cutter | Flickering parallax panels |
| 1 | The Starless Gallery | slate / bone | Null Portrait, Gallery Moth | Light-swallow dust |
| 2 | Meridian Descent | bronze / verdigris | Gnomon Knight, Armillary Scout | Collapsing meridian tiles |
| 3 | Choir of Dust | bone / muted bronze | Dust Cantorile, Ash Chorister | Brittle ash floor over solid underlayer |
| 4 | The Glassbound Vault | cyan glass / slate | Prism Stalker, Shardling Swarm | Shatter panes |
| 5 | The Hollow Catalogue | parchment / gold | Index Wight, Shelf Crawler | False shelves |
| 6 | The Cantor's Labyrinth | rune / bronze | Metronome Hound, Labyrinth Usher | Resonant pressure plates |
| 7 | The Unwritten Hour | indigo / gold | Blank Chronist, Hour-Hand Wraith | Ink pools (slow) |

### Exclusive behaviors (summary)

- **Shard Drifter** — short blink leaving a brief decoy silhouette.
- **Wake Cutter** — scything dash with brief mid-lunge invulnerability frames.
- **Null Portrait** — idle until line of sight, then lunges.
- **Gallery Moth** — soft glow that dims nearby lamps / applies Darkness in a small volume.
- **Gnomon Knight** — telegraphed shock pulse.
- **Armillary Scout** — orbits, then dives.
- **Dust Cantorile** — short silence / slow pulse.
- **Ash Chorister** — on low health, splits into two weaker wisps once.
- **Prism Stalker** — reduced damage unless attacker faces it along a glass sightline (simplified: vulnerable when not moving / after telegraph).
- **Shardling Swarm** — small pack mob; death breaks nearby shatter panes.
- **Index Wight** — marks a player so allies deal bonus damage.
- **Shelf Crawler** — ceiling cling, drops when under a target.
- **Metronome Hound** — attacks on a beat; off-beat window is safer.
- **Labyrinth Usher** — briefly seals one door of the containing room.
- **Blank Chronist** — applies a short Weakness tick.
- **Hour-Hand Wraith** — telegraphed long-reach sweep.

## Blocks and art

- About 4–6 new blocks per theme (palette + hazard/prop), authored via
  `tools/textures/archive_blocks.py` on the shared Archive palette with accent bias.
- Entities use hand-written vanilla `ModelLayer` meshes, procedural `setupAnim`,
  preferably 128×128 at `TEX_SCALE 0.5`, with emissive overlays where the silhouette
  needs a tell. No GeckoLib.
- All new theme blocks join `tbos:archive_run_palette`.
- Hazards must leave a solid underlayer or full block — never open void through
  the room floor.

## Difficulty

Existing `ArchiveFloorPresentation` curves
(`difficultyBonus`, `additionalRooms`, `additionalEnemies`) and room-depth
difficulty remain authoritative. Theme exclusives use the same HP/damage scaling
as other custom Archive enemies. Optional: on Echo ≥ II, exclusive weights may
bump slightly without new art.

## Config and docs

- Extend `echoesOfThePastDungeon` enemy pool lines for theme groups; try/catch
  falls back to `DEFAULT`.
- Update `docs/echoes-of-the-past.md`, `docs/design.md`, `docs/testing.md`,
  `CHANGELOG.md`. Bump `mod_version` to `0.3.0`.

## Verification

- Dungeon simulation: theme from floor is stable; palette membership; no void
  floors from hazards.
- GameTests: theme→pool exclusivity contracts; one live ability fixture per
  exclusive (combatFloor pattern); floor naming/intro still correct for 0–7.
- Manual: walk each theme once for silhouette, hazard telegraph, and Cantor climax.

## Out of scope

- Mob abilities that read or modify theme terrain
- Third exclusive per theme
- GeckoLib / Blockbench keyframe pipelines
- Final-quality new sounds (placeholders allowed unless documented as final)
- Replacing Hour Cantor or removing shared core / vanilla fillers
