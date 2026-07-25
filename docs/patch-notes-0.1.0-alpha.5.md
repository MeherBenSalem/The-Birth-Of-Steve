# Patch notes — 0.1.0-alpha.5

## Lensward

- Added the **Lensward** (`tbos:lensward`), a hovering Archive construct with a fully custom native Minecraft model, procedural animation, and a bespoke 64×64 texture. It is the first mob in the mod that does not walk: a weathered housing wrapped in two counter-rotating rings, with hanging shards and an amethyst lens core.
- The lens core renders through a dedicated emissive layer, so it stays visible in unlit rooms and brightens as the Lensward charges.
- Lenswards ward the position they were placed at. They engage targets inside a ten-block radius of that anchor and drift back to it rather than pursuing across the dungeon.
- Their focused beam telegraphs for thirty ticks before firing. **Breaking line of sight during the charge cancels the shot**, so cover is a real defence.
- Added the creature to Forgotten Legion encounters at weight 1 and Elite Echoes encounters at weight 3. It is not included in lesser-boss or final-boss pools.
- Lenswards use their own attack logic and are excluded from the Parallax Blink mutation, which would otherwise teleport them off the anchor they guard.
- Added four `tbos:` sound events for charge, fire, ambient, and hurt cues, with subtitles.

## Memory Leech

- Added the **Memory Leech** (`tbos:memory_leech`), an elite Archive monster with a fully custom native Minecraft model, procedural animation, and bespoke 64×64 texture.
- Memory Leeches telegraph a siphoning pounce with violet particles and amethyst chimes. A successful bite deals normal attack damage, applies Weakness I for three seconds, and restores four health; a missed pounce grants no healing.
- Added the creature to Forgotten Legion encounters at weight 2 and Elite Echoes encounters at weight 3. It is not included in lesser-boss or final-boss pools.
- Memory Leeches use their own attack logic and are excluded from the Parallax Blink mutation, preventing the mutation from interrupting a pounce.

## Compatibility and configuration

- Existing Archive enemy enum order and deterministic drop rolls are preserved.
- Fresh server configuration defaults include the new encounter weights. Existing customized `tbos-common.toml` files are intentionally unchanged; add `tbos:memory_leech` entries manually if desired.

## Verification

- Added GameTest coverage for Memory Leech parsing, encounter weighting, attributes, mutation exclusion, boss-pool exclusion, deterministic drops, and pounce effects.
- Added GameTest coverage for Lensward parsing, encounter weighting, boss-pool exclusion, mutation exclusion, attributes, enum-ordinal stability, single-strike beam damage, line-of-sight cancellation, and ward tethering.
- The 1,000-seed Archive dungeon simulation and full GameTest suite pass.
