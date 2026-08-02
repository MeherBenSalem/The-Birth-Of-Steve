# Minecraft 1.21.1 overrides

This tree contains only the source and resource deltas needed to compile the
shared mod on Minecraft 1.21.1. A matching path replaces the shared copy for
this target; `gradlew.bat -p 1.21.1 :common:sourceOverrides` lists the active
shadow set.

The baseline loader and mapping versions are from
`C:\Users\Meher\Desktop\MultiLoader-Template-1.21.1.zip`. The target uses
Java 21 and Mojang/Parchment mappings because 1.21.1 is not shipped with the
deobfuscated 26.x development names.

Loom is 1.9.2 rather than the template's 1.8-SNAPSHOT: the latter calls a
Gradle API removed by this repository's Gradle 9.5 wrapper. The newer Loom line
is the smallest compatible replacement; all Minecraft, mappings, Fabric API,
Fabric Loader, and NeoForge baseline values remain those from the template.

Minecraft 1.21.1 does not expose the newer `TEST_FUNCTION` registry. The full
shared GameTest bodies are therefore shadowed only at their registration seam:
the Fabric adapter is a `fabric-gametest` entrypoint and the NeoForge adapter
uses `@GameTestHolder`. Both delegate to the same 56 target-common test bodies.
The target-local all-air `tbos:empty` NBT template is 97x32x97, which preserves
the 48-block horizontal test isolation that the 26.x data-driven instances supply
as padding.
