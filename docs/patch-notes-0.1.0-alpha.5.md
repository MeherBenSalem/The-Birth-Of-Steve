# Patch notes — 0.1.0-alpha.5

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
- The 1,000-seed Archive dungeon simulation and full GameTest suite pass.
