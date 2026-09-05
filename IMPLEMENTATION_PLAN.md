# Living Memories — execution and evidence

## Order

1. Audit and pre-change baseline. Preserve all current content and saved runs.
2. S: durable build state, deterministic drafts, bounded combat pipeline, six actives, twelve artifacts, loader transport and client loadout.
3. S: protected authored sockets and composed reconstruction.
4. A: new-run expedition rules, climax/extraction consent, Overwrite and secrets, boss patterns.
5. B: discovery/HUD/audio/effect hierarchy, accessibility and final polish.
6. Verification across eight jars, supported GameTests, seed simulations, persistence and stress scenarios.

## Release gates

- All shipped builds compile; supported loader GameTests pass. Forge 1.20.1 has a documented mock-player issue, so distinguish infrastructure failures from regressions.
- Test successful-hit attribution, recursion and budgets; repeated/forged requests; immutable rewards; cooldown preservation; old saves; death/revive/exit/reconnect; party consensus.
- At least 1,000 generated seeds satisfy reachability and reward/socket invariants.
- Actual client checks: complete melee/ranged/defensive/reconstruction/co-op runs, 30–45-minute first victory target, five trailer moments, normal/reduced effects.
- Ten-minute maximal-build stress records hardware, p95 tick time (target <50 ms), frame times, network and entity/effect counts.

## Evidence log

- Initial working tree: clean.
- Baseline aggregate build: passed all eight targets before gameplay implementation.
- Initial architecture and paper-run audit: recorded in PROJECT_AUDIT.md.
- Runtime visual, performance and fun claims: not yet verified.

No unexecuted test is a pass. Remaining work must be reported explicitly.

## Current validation checkpoint

- `build/living-memories-build-verified.log`: all eight distributables built successfully (before the latest optional-encounter refinements).
- `build/living-memories-matrix.log`: 26.1.2 NeoForge 61/61 and 1.21.1 NeoForge 59/59; 26.2 exposed an entity-constant rename and was corrected to the existing VanillaCompat constants. The old Fabric run in this aggregate reused stale test data.
- `build/living-memories-120-fresh.log`: clean 1.20.1 Fabric world, 59/59 passed.
- Every common `check` runs MemorySimulation: 1,000 new expedition seeds, graph reachability/non-overlap, socket inset, deterministic claims, cooldown swapping, pending-offer codec round trips and exhausted artifact pools. This does not simulate damage or prove 1,000 fully playable runs.
- Development client starts and loads assets. Visual input is deferred while the user is in another game. No combat frame has been accepted as evidence yet.

Outstanding release gates remain explicit: real attack/absorption/blocked-hit integration; live old-save/reconnect/shared-revive sessions; distinct socket-role review; secret locality; complete melee/ranged/defensive/reconstruction/four-player runs; boss and first-victory durations; a documented-hardware 10-minute performance capture; normal/reduced screenshots and second visual polish pass. Do not mark this update release-ready until those gates have evidence.
