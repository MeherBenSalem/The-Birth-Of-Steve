# Performance

Milestone 1 budgets:

- No recurring world-wide site scan.
- Spatial lookup only after Lens use or relevant chunk/player events.
- At most one 16x16 spike site transition active in the prototype.
- At most 64 shell segments; no per-block display entities.
- Physical phase changes are capped and authored.
- Client particles are bounded by quality tier and terminate with the transition.
- The fallback uses fixed 64-segment topology and per-transition activation flags;
  it constructs no mesh and performs no per-frame collection rebuild. A future
  dynamic mesh must be cached by site definition, rotation, resource revision, and
  quality before it can be approved.

Profiling results are pending a runnable transition.

Current deterministic fallback caps per transition:

- Minimal: at most 128 particles.
- Low: at most 192 particles.
- Medium: at most 384 particles.
- High: at most 768 particles.
- Reduced motion suppresses the secondary moving particles.
- At most eight transition trackers may coexist client-side; the oldest is evicted
  before a ninth is admitted, and server broadcasts are limited to 96 blocks.

The GameTest physical round trip completes with six authored block changes. This is
functional validation, not a client frame-time or allocation profile.
