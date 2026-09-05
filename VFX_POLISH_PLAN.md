# Living Memories — presentation contract

| Event | Presentation |
|---|---|
| Ordinary cast | Thin cyan directional trace, quiet short cue |
| Successful hit | Brief local flash; no cue for rejected damage |
| Guard | Directional arc and single pitched impact |
| Room clear | Amber accent and short resolving sound |
| Discovery | Small icon/name/flavor card that expires |
| Overwrite | Restrained violet accent with exact cost before accepting |
| Boss danger | Persistent footprint until release; independent of friendly effect quality |
| Secret | Local reveal and distinct short sound |

HUD: three compact ability icons with remappable key labels and cooldown fractions; K opens loadout and pending choices. Never cover crosshair, health or existing puzzle instructions. Scale layout to GUI size.

Batch effect descriptions to nearby party clients. Render cosmetics locally, using existing effectQuality/reducedMotion settings. Limit voices and particle generation, preserve impact/telegraph priority, and avoid global screen shake or server hit-stop.

Acceptance: capture ordinary/maximal builds at normal and reduced effects, small GUI resolution and multiplayer. Inspect frozen combat for clear enemy silhouettes and warning shapes. Asset ownership and live-client quality must be recorded rather than assumed.

## Implemented choreography and limits

Recall collapses opposing rings around the casting axis over 16 client ticks. Well uses two rotating rings; release expands rings and inward spokes. Storm paths use seeded seven-segment jagged lines with a short side branch. Shatter expands a low ring with eight initial fragments. Reconstruction lifts a ring through the feature. Delayed Ink draws a contracting pre-impact marker. Guard creates a directional ring; Overwrite has a violet station loop. Discovery and room-clear sounds share a five-tick voice gate.

The client retains at most 96 events. Server batches hold at most 32 descriptions per recipient every two ticks. Danger events evict friendly events when full. Friendly emission budgets per client tick are 16/64/160/256 by quality setting; danger has a separate 192-emission budget. These are emission limits, not measured live-particle counts: native particle lifetimes extend beyond the tick that creates them. Native particle assets establish the current colors; exact cyan/amber/violet art direction still needs screenshot review.

Reduced motion removes ring expansion/rotation where applicable. No camera shake, global slowdown, entity rewinding or custom shader is introduced. Memory events are shown only to nearby party members. Enemy warnings remain enabled at minimal cosmetic quality.

Unpassed visual gates: native-particle warning persistence between packets, friendly-Ember versus danger-flame distinction, four-build overlap, minimal settings, GUI scaling, actual five trailer moments. Do not use this document as a visual signoff.

## Journal UI art pass

The Abilities & Stats chapter is accessible through a third journal tab and the K shortcut. The chapter uses native focusable buttons, explicit locked/empty states, fitted labels with full tooltips, server-backed equipment and reward requests, and a separate Stats page. The existing Story and Archive Run chapters remain available. Cooldown, healing and incoming-damage modifiers are displayed separately from health and armor.

`assets/tbos/textures/gui/memory/icons.png` is original AI-generated artwork created for this update on 2026-09-05. The 1774×887 RGBA atlas contains six ability icons and twelve artifact icons in enum order, six columns by three rows. It is sampled directly with integer cell bounds, no per-frame texture allocation. Minecraft texture metadata disables blur and clamps the texture. Runtime layout, borders, tooltips and typography remain native Java UI, not a screenshot of a menu.

The first actual cast screenshot exposed an eye-level particle covering the crosshair. Particle submission now excludes a one-block radius around the local eye. This is a presentation-only exclusion and does not change server collision or damage.
