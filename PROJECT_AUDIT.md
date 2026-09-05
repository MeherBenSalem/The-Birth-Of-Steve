# Living Memories — project audit

## Evidence and preservation

The pre-update working tree was clean. Baseline aggregate build launched before edits; results are recorded in IMPLEMENTATION_PLAN.md. Code inspection is evidence of behavior, not evidence of fun or visual quality.

The shared/common source is the gameplay authority; shared/fabric and shared/neoforge adapt events and packets. Version overrides replace whole files, including the older Archive managers, networking and HUDs. All eight loader/version combinations must remain aligned. README/architecture still describe six jars, whereas the root build and 0.8.0 notes correctly list eight.

Player → authored Fracture Shrines → repaired Lens → reconstructed adventure puzzles → Last Curator → Archive Core → threshold → ArchiveRunManager → deterministic ArchiveRunGenerator graph → budgeted ArchiveGenerationQueue → ArchiveEncounterManager room/wave/puzzle state → weighted ArchiveLootRoller tables and personal cache claims → reward gateway → next floor.

Run and member codecs preserve checkpoints, personal claims, shared revives, return destinations, room encounters and retired floor allocations. The death hook cancels player death and queues recovery; reconnect reconciliation returns stranded members. Preserve these paths. Run-member records are rebuilt on floor changes, so new build persistence must not be silently reset by those constructors.

## Findings

- Player combat builds: ModItems registers campaign keys, lore, recovery and utility items. The inspected run-member state has no artifact composition or active loadout. This is the largest missing link between reward and build evolution.
- Encounters: separate rooms already tick independently. HALL/CHOIR puzzles, guardian waves, themed hazards, lesser bosses and native enemy abilities exist. Preserve them.
- Pacing: Ominous repeats ten reinforcement batches; most final bosses multiply health by 4.5 before party/difficulty/reinforcement scaling. These risk repetition and attrition.
- Bosses: Lensward, Sentinel, Cantor, Leech, Wraith, Phoenix, Minotaur and theme-exclusive entities have native behavior. ThemeExclusiveEntity already has windup/active/recovery and wounded escalation; extend readable footprints rather than replacing all AI.
- Terrain: shell/quest protection, crate claims and cache claims are explicit. Reconstruction must address authored sockets, not arbitrary player-selected blocks.
- Client: quest, puzzle and floor-intro HUDs, journal, animated entity models, transition rendering, sound registries and reduced-motion/effect-quality settings exist. Add one coordinated memory presentation layer.
- Performance: enemy queries are room-local, but repeated queries and server particle calls remain. New effects require shared local snapshots, bounded descendants, and batched client cosmetics.
- Assets: the 0.8.0 texture pass is worth retaining. Existing documentation calls some sounds/art placeholders; do not certify ownership or final visual quality without evidence.

## Paper runs before implementation

1. Melee: ordinary sword + Echo Lance → first clear Split Prism → Recall discovery → Ember Script → guard discovery → risky cooldown Overwrite → recalled fan kills a marked group → third-floor climax. Weak equipment still has the starter lance; failed damage must not generate free procs.
2. Ranged: bow + Seeking Glass → Recall → Piercing Index → conductor discovery → safe node routing beats an obstructed room. Recall re-tests collision rather than hitting an enemy through its new cover.
3. Reconstruction: Reconstruct + Mason's Remnant → restore cover → pulse conducted through sockets → Memory Well → stored release. Every room remains solvable without this build; no socket controls mandatory traversal.
4. Cooperative: four personal drafts; guard protects its owner while another player stores projectiles. Attribution cannot steal another build's kill effects. Shared revive keeps discoveries; defeat removes transient powers. A disconnected member cannot be dragged into endless without consent.

Likely boring moments: long reinforcement chains, repeated ordinary loot, and a first floor without a recognizable build. Prioritize early guaranteed choices, complementary actives and shorter expedition pacing. Actual run timing, visual clarity and low-end frame rate remain unverified until measured.

## Implementation audit — 2026-09-05

| Area | Evidence in the working tree | Remaining evidence |
|---|---|---|
| Build state | MemoryBuild/MemoryExpedition codecs are sidecars in ArchiveRunSavedData; old saves omit them | Real old-world migration and reconnect session |
| Input/network | Three integer request fields; server checks current run, membership, ownership, cooldown, draft revision and location | Malicious-client runtime tests |
| Damage | Fabric damage mixins, NeoForge successful-damage events, Forge 1.20.1 final damage event | Cross-mod cancellations and absorption cases on Forge 1.20.1 |
| Composition | Logical shots, bounded delayed impacts, original modifier snapshots, room/floor lineage, once-consumed marked deaths | Full live combat matrix; returned hurt booleans need absorption verification |
| Native enemies | Existing enemy classes and phase machinery remain; expedition boss health multiplier changes | Per-boss fight duration; native attack/socket timing needs playtest tuning |
| Rendering/audio | MemoryEffects uses native particles, capped timelines, amethyst sounds; MemoryGlyphs draws 18 original glyphs | Combat screenshots, lower GUI scales and sound mix |
| Configuration | Existing EFFECT_QUALITY and REDUCED_MOTION affect memory cosmetics | Measured minimal-effects clarity and GPU/frame-time cost |
| Resources | Shared language keys, completion advancement, old-version folder remapping | Localization beyond English |
| Tests | 1,000-seed executable model tests; existing loader GameTests | New engine integration coverage, four-player sessions and 10-minute stress |

Build failure found during integration: 1.20.1 remaps shared `advancement` to `advancements`; a separately added plural override duplicated the completion resource. Removed the redundant override. No custom texture downloads or generated third-party art were introduced.

Existing GameTests exposed an absent waystone palette entry and a parallax panel whose floor was removed by a stair cut. Added the palette entry and removed unsupported surface hazards after connections are carved. Kept the connected-solid assertion intact. The Curator fixture now advances actual server ticks through both 60-tick phase shields and forces its site chunks, rather than treating repeated same-tick tracker calls as elapsed time.

A repeated 1.20.1 Fabric run reused a saved test world with conflicting site registrations. Preserved it as `world-before-living-memories-validation` inside that target's test directory; a fresh world passed all 59 tests. This did not touch player worlds.

## Complete tabletop run scripts (hypotheses, not playtest results)

These scripts are acceptance scenarios, not claims that the generator currently guarantees every named draft or duration. At each floor, traverse entry, combat, branch decision, second combat, authored puzzle, third combat, boss and reward route; some graphs combine or add a room. Estimate 10–15 minutes per floor only until measured.

- **Melee victory:** Floor 1 uses an ordinary sword and Lance. A weak opening draft takes Piercing Index before enemies form useful lines; discovery 2 supplies Guard, discovery 4 Recall. A low-health player skips the optional branch, defeats the first boss and keeps all cooldowns through transition. Floor 2 adds Ember and Shatter, then accepts longer cooldowns to improve Ember. Overcommitting during the trial costs one shared revive: transient attacks disappear but the build remains. The second boss clears the debt. Floor 3 uses Guard recovery to Recall a sword attack toward marked enemies, defeats the native boss plus socket pattern, records completion and extracts. Endless variant chooses continuation at the gateway and repeats floor advancement with discoveries retained.
- **Ranged victory:** Floor 1 combines a bow with Lance, Seeking Glass and Recall. A poorly aimed recalled volley hits a wall and produces no impact effects. An empty third slot is filled by the fourth reward. Floor 2 chooses Reconstruct over another aggressive active, accepts a damage-received drawback and must avoid the spawned ranged echo while claiming no premature trial reward. Piercing plus Returning Thread helps a lined-up pack but a blocked return ends. Floor 3 deliberately delays a shot until the native boss's recovery, clears the climax and extracts. An alternate death branch spends remaining revives on the trial and fails the run; no combat history is available after return home.
- **Reconstruction/defense:** Floor 1's early passives support the starter Lance while Reconstruct and Well are being discovered; Mason/Wick/Nail should not appear as ordinary unusable choices. Floor 2 equips Guard, Reconstruct and Well, using ordinary melee attacks to fill the well. Occupied sockets reject reconstruction without consuming cooldown. Restore cover, conduct a shot and complete the optional circuit; leaving its echoes alive cannot grant the rare reward. Skip Overwrite on a weak build. Floor 3 trades Well for Recall in a cleared room, retaining Well's cooldown, then uses cover during the boss footprint and wins. Endless offers eventually exhaust the passive pool and deliver consumables instead of stacking artifact copies.
- **Four players:** Personal drafts create a lance/Recall attacker, defensive guard, reconstructed-node user and Well user. Each can claim the same reward opportunity exactly once, with different deterministic choices. One player disconnects during floor 2: progress credit and pending choices persist, transient effects do not. Another spends a shared revive; nobody loses discoveries. Floor 3 adds socket danger without letting friendly effects overwrite its warning packet budget. After victory three vote for endless, but the disconnected fourth blocks advancement. On reconnect that member either votes or explicitly leaves the party. Only then may remaining members continue. A wipe before the boss returns everyone and disables powers.

Second audit status: the build engine is implemented but is not release-certified. Distinct authored socket roles, the secret reveal's locality, final-boss choreography and complete playthrough timing require further refinement and observed acceptance evidence. The visual layer is code-backed; “epic” and “fun” remain judgments to verify in play.
