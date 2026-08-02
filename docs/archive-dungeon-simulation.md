# Archive dungeon simulation report

Command:

```text
gradlew.bat -p 1.21.1 :common:runDungeonSimulation --console=plain
```

It writes `1.21.1/common/build/reports/tbos/archive-dungeon-simulation.json`. The
same task exists for every Minecraft version and, because the generator is shared
source, produces identical results; `-p 26.1.2` or `-p 26.2` writes the same
report under that target folder.

The task lives in the loader-agnostic `common` module because the generator is a
pure seeded function that never touches a world. It generates 1,000 consecutive
deterministic seeds with the production
defaults. It fails the build if any seed cannot generate, any room is unreachable,
any pair of room volumes overlaps, the derived lesser-boss count is wrong, the
final gate begins unlocked, or the final-gate quest begins completed.

Captured on 2026-07-23:

| Metric | Result |
| --- | ---: |
| Simulations | 1,000 |
| Failed generations | 0 (0.0000%) |
| Average room count | 16.923 |
| Average branch count | 3.931 |
| Average vertical-room count | 5.134 |
| Average loop count | 0.361 |
| Unreachable-room count | 0 |
| Overlap count | 0 |
| Lesser-boss count mismatches | 0 |
| Quest-gate violations | 0 |
| Average generation time | 0.3674 ms |
| Maximum generation time | 134.4763 ms |

Machine-readable output is written to
`build/reports/tbos/archive-dungeon-simulation.json`. Timing is a local JVM
measurement and will vary by host; the seed-derived topology metrics are
deterministic for this code and default configuration.
