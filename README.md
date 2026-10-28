# Remnants: Roguelite Dungeons

> Every ruin has a before. Remember it, or forget it and go deeper.

Remnants: Roguelite Dungeons (formerly *The Birth of Steve*) is a roguelite
dungeon RPG for Minecraft Java Edition 1.20.1, 1.21.1, 26.1.2, 26.2 and 26.3, on
Fabric and on NeoForge (Forge on 1.20.1), built from a single shared codebase. The
mod id stays `tbos`, so existing worlds, commands and jar names carry over.

## What it is

- **The prologue.** The authored *Birth of Steve* campaign: Fracture Shrines in
  every Overworld, the Yesterglass Lens, the Meridian Archive's four puzzle sites
  and the Last Curator. Defeating the Curator opens the Curator Gateway.
- **The Fractured Archive.** A replayable, procedural, multi-floor dungeon
  dimension: eight floor themes, twenty room templates, bespoke telegraphed
  creatures, floor bosses, secrets, per-member loot and the Living Memories combat
  build.
- **1.0 "Remember or Forget".** Memory Keys with Fracture Tiers and Distortions, the
  Remember-or-Forget push-your-luck loop, Recollections of any structure, generated
  Relics, the Archive Codex (Renown, Insights, Bestiary, Hall of Records and a
  Weekly Recollection) and parties. The design and every default are in
  [`docs/remember-or-forget.md`](docs/remember-or-forget.md); why these features is
  in [`docs/market-analysis.md`](docs/market-analysis.md).

Release notes are in [`patchnotes/`](patchnotes) and [`CHANGELOG.md`](CHANGELOG.md).
The listing copy and artwork are in [`marketing/`](marketing).

## Start playing

For installation, the first expedition, cache rules, multiplayer behaviour and the
complete operator command reference, read
[`docs/getting-started.md`](docs/getting-started.md). The Archive's design and
operator guide is [`docs/echoes-of-the-past.md`](docs/echoes-of-the-past.md); the
checked-in 1,000-seed evidence is
[`docs/archive-dungeon-simulation.md`](docs/archive-dungeon-simulation.md).

## Development setup

Requirements: JDK 17 for 1.20.1, JDK 21 for 1.21.1 and JDK 25 for the 26.x
targets. Use the checked-in Gradle wrapper:

```text
gradlew.bat build
gradlew.bat gameTest
gradlew.bat -p 26.1.2 :neoforge:runClient
gradlew.bat -p 26.1.2 :fabric:runClient
gradlew.bat -p 26.1.2 :neoforge:runGameTestServer
gradlew.bat -p 26.1.2 :common:runDungeonSimulation
gradlew.bat -p 1.21.1 :fabric:runGameTestServer
```

On Linux or macOS, use `./gradlew` instead.

`build` at the root builds **every** Minecraft version: ten jars, one per loader per
version. `-p <version>` drives a single target's own Gradle build; use `1.20.1`,
`1.21.1`, `26.1.2`, `26.2` or `26.3` to select a target.

The mod's source lives once in `shared/`, in the MultiLoader-Template layout:
`shared/common` holds nearly all of the mod and compiles against vanilla, while
`shared/neoforge` and `shared/fabric` only load it and supply loader-specific
glue. Each Minecraft version gets a top-level folder (`1.20.1/`, `1.21.1/`,
`26.1.2/`, `26.2/`, `26.3/`) holding only that target's build scripts and an
`overrides/` directory for the files that version has to do differently. The 1.0
systems reach vanilla only through three seams (`compat/RpgCompat`,
`client/rpg/Canvas` and `client/rpg/RpgScreen`), so each target overrides those
files and never forks the features. See [`docs/architecture.md`](docs/architecture.md)
for the full layout.

### Tools

- `tools/rpg/`: the generators behind the 1.0 resources. `build_data.py` writes the
  language entries, advancements, recipe, tags and `sounds.json`; `icons.py` paints
  the GUI icon sheet; `memory_key.py` and `logo.py` draw the item sprite and the mod
  logo; `sounds.py` synthesises the 27 sound effects. Each documents how to run it.
- `tools/qa/`: dev server and QA client control for in-game checks and the listing
  captures (RCON, xdotool and Xvfb).

## License

All Rights Reserved. See `LICENSE`. Minecraft, NeoForge, Fabric, Gradle, and
third-party components retain their respective ownership and licenses. The Cinzel
and EB Garamond fonts in `marketing/source/fonts/` are licensed under the SIL Open
Font License 1.1.
