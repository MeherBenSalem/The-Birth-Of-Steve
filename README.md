# The Birth of Steve

> Every ruin has a before.

The Birth of Steve is an adventure mod for Minecraft Java Edition 26.1.2 and 26.2,
built for both NeoForge and Fabric from a single shared codebase. Its central
mechanic reconstructs a remembered version of authored ruins at the same world
coordinates.

## Current state

This repository is in an integrated gameplay-alpha phase. The authored five-room
adventure now leads into Echoes of the Past: a deterministic, replayable,
multi-floor procedural Archive with reusable room templates, independent
multiplayer encounters, weighted loot and enemies, secrets, persistence, and
tick-budgeted generation. Three Fracture Shrine variants, the Last Curator,
Memory Lantern, and six Memory Plates are also implemented. Shrine locations are
derived from the world seed, and the Last Curator's defeated Archive Core becomes
the gateway into the procedural dimension. Included art, sounds, and effects must
still be treated as developer placeholders unless their ownership and final
status are recorded in the project documentation.

The dungeon design and operator guide is in
[`docs/echoes-of-the-past.md`](docs/echoes-of-the-past.md). The checked-in
1,000-seed evidence is in
[`docs/archive-dungeon-simulation.md`](docs/archive-dungeon-simulation.md).
## Start playing

For installation, the first expedition, cache rules, multiplayer behavior, and
the complete operator command reference, read
[`docs/getting-started.md`](docs/getting-started.md).

## Development setup

Requirements: JDK 25. Use the checked-in Gradle wrapper:

```text
gradlew.bat build
gradlew.bat -p 26.1.2 :neoforge:runClient
gradlew.bat -p 26.1.2 :fabric:runClient
gradlew.bat -p 26.1.2 :neoforge:runGameTestServer
gradlew.bat -p 26.1.2 :common:runDungeonSimulation
```

On Linux or macOS, use `./gradlew` instead.

`build` at the root builds **every** Minecraft version — four jars, one per
loader per version. `-p <version>` drives a single target's own Gradle build;
swap `26.1.2` for `26.2` above to work on the other one.

The mod's source lives once in `shared/`, in the MultiLoader-Template layout:
`shared/common` holds nearly all of the mod and compiles against vanilla, while
`shared/neoforge` and `shared/fabric` only load it and supply loader-specific
glue. Each Minecraft version gets a top-level folder (`26.1.2/`, `26.2/`) holding
only that target's build scripts and an `overrides/` directory for the handful of
files that version has to do differently. See
[`docs/architecture.md`](docs/architecture.md) for the full layout.

## License

All Rights Reserved. See `LICENSE`. Minecraft, NeoForge, Fabric, Gradle, and
third-party components retain their respective ownership and licenses.
