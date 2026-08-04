# Minecraft 1.20.1 overrides

This tree contains only the source and resource deltas needed to compile and run
the shared mod on Minecraft 1.20.1. A matching path replaces the shared copy for
this target; `gradlew.bat -p 1.20.1 :common:sourceOverrides` lists the active
shadow set.

This is the only target that ships **Forge** rather than NeoForge, so the module
is `forge/` and the loader jar is `tbos-forge-1.20.1-<version>.jar`. The Java
package is still `com.nightbeam.tbos.neoforge` and the entry point is still
`TbosNeoForge`: those names come from the shared layout, and renaming them here
would be a rename of `shared/`, not a delta. The target uses Java 17 with
Mojang/Parchment mappings; Loom is 1.9.2 and ModDevGradle's `legacyforge` plugin
drives Forge 47.2.30.

## Loader deltas worth knowing

- **`@Mod` construction.** Forge 1.20.1's `FMLModContainer` instantiates the mod
  class through its *no-argument* constructor and offers the mod event bus from
  `FMLJavaModLoadingContext`. Only NeoForge 1.20.4+ injects the bus as a
  constructor parameter.
- **Service providers.** `Services` loads the platform helpers through
  `ServiceLoader`, which needs a public no-arg constructor on every provider
  named in `META-INF/services`.
- **`pack.mcmeta` is mandatory.** Fabric Loader synthesizes pack metadata for a
  mod that ships none; Forge 1.20.1 does not. It logs `Missing metadata in pack
  mod:tbos` and drops the mod's entire data pack, taking the loot tables,
  advancements, recipes and the GameTest structure with it. The file lives here
  rather than in `shared/` because `pack_format` is per-version (15 for 1.20.1).
- **Mixin compatibility level** is `JAVA_17`, matching the target's toolchain. A
  higher level fails Mixin bootstrap outright.
- **Chunk-load callbacks must not write blocks.** `TemporalSiteEvents` records
  the loaded chunk and applies site phase geometry from the server tick, the way
  `FractureShrineQueue` already defers its builds. Fabric 1.20.1 fires the
  callback from inside `ChunkMap`'s proto-to-full conversion, so a block write
  reaching a chunk that is not already resident re-enters the chunk loader and
  parks the server thread on a future only that thread could complete — hanging
  the server on the first load of any world that already contains a site.

## Data pack layout

Minecraft 1.21 renamed the data pack registry directories to their singular form
(`recipes` → `recipe`, `tags/blocks` → `tags/block`, and so on), and `shared/` is
authored in that newer layout. 1.20.1 reads the plural names, so
`multiloader-common.gradle` relocates them during `processResources` instead of
forking ~140 data files into this tree purely to rename their parent directory.
That same step drops `test_instance/` and `test_environment/`, which are 26.x-only
registries.

A file whose *body* differs on 1.20.1 still belongs here, at its shared
(singular) path, and is relocated by that step. The bodies that differ:

- advancement display icons use `"item"`; `"id"` is the 1.20.5+ spelling,
- `ItemPredicate` requires `"items"` to be an array, not a bare id,
- crafting results use `"item"` rather than `"id"`,
- `archive_run_palette` swaps the 1.21 tuff family for deepslate tiles and
  `iron_chain` for `minecraft:chain`, matching this target's `ArchiveRoomPlacer`.
  A tag naming a block this version does not have fails to load *entirely*, and
  every `.is(ARCHIVE_RUN_PALETTE)` check then answers false.

## GameTests

`gradlew.bat -p 1.20.1 :fabric:runGameTestServer` is this target's test gate and
runs all 56 shared tests green. The root `gameTest` task routes 1.20.1 there.

`:forge:runGameTestServer` is wired to Forge's real `gameTestServer` launch
target and does run the suite, but 14 tests fail on it for a reason outside this
mod: `GameTestHelper.makeMockServerPlayerInLevel()` calls
`PlayerList.placeNewPlayer` with a `Connection` that has no netty channel, and
Forge patches that join path to call `NetworkFilters.injectIfNecessary`, which
dereferences `channel().pipeline()`. Vanilla tolerates a channel-less connection
— `Connection.send` only queues while disconnected — which is why the same tests
pass under Fabric. Substituting a hand-built mock player was tried and rejected:
it cannot reproduce what `placeNewPlayer` sets up, and it cost four Fabric tests
to recover ten Forge ones.

The all-air `tbos:empty` template is 97x32x97, preserving the 48-block test
isolation that the 26.x data-driven instances supply as padding. Forge composes
the structure name as `<GameTestHolder namespace>:<template>`, so the templates
are spelled bare; `@PrefixGameTestTemplate(false)` suppresses only the
class-name prefix.

## Build

`gradlew.bat -p 1.20.1 build` builds this target alone. Note that
`processResources` relocates directories and Gradle's incremental copy leaves the
previous layout behind when those rules change — if a jar ever shows both
`recipe/` and `recipes/`, run `clean`.
