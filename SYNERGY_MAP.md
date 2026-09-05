# Living Memories — composition contract

Every effect carries owner, run, floor, room, root action, lineage depth and a modifier snapshot. Only eligible original successful attacks and primary casts enter a 32-record ring. Recall consumes up to three records from the previous 80 ticks; it uses fresh world collision and cannot record its own output.

| Modifier | Composition |
|---|---|
| Split Prism | Fork directional delivery; descendants inherit modifiers without splitting again |
| Seeking Glass | Steer toward eligible room enemies, preserving block collision |
| Ember Script | Successful impact marks and burns |
| Storm Filament | Successful impact arcs to a nearby distinct target |
| Shatter Seal | An attributed marked kill releases a bounded local burst |
| Piercing Index | Delivery can continue through an additional distinct target |
| Returning Thread | One return leg; no infinite bouncing |
| Delayed Ink | One visibly delayed impact with a fresh target/visibility check |
| Resonant Nail | Effects route through authored linked nodes |
| Memory Wick | Improves bounded well capacity and release |
| Ward Fragment | A successful frontal guard releases modified counter-echoes |
| Mason's Remnant | Reconstruction emits a modified pulse |

Secondary depth ≤3; ≤64 resolutions/player/tick and ≤256/run/tick. Overflow is deterministic and cosmetic counts cannot increase authoritative damage. A kill resolves once. On-hit requires successful damage. Room/run/floor and ownership checks apply at execution, not just creation. No party damage or cross-instance targeting.

Test chains: split+seek+ember; pierce+return; ember+storm+shatter; recall+split+delayed; guard+ward+storm; reconstruct+mason+nail; well+wick+split. Include invulnerability frames, dead/moved targets, walls, terminal runs, disconnects and budget exhaustion.
