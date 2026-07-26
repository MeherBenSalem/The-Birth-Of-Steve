# Patch notes — 0.2.0-alpha.1

## Endless Archive floors

- Archive runs now begin at **floor 0** and continue through floor 1, floor 2,
  floor 3, and onward for as long as the party survives.
- Clearing a floor no longer starts the old 30-second victory return to the
  Overworld. It immediately queues construction of the next floor.
- Every floor uses a newly derived seed and a structurally distinct procedural
  dungeon graph in a fresh isolated Fractured Archive instance.
- Players move directly from the completed Archive floor into the new Archive
  floor after every room and safe spawn block has finished generating.

## Persistence and cleanup

- The run ID, party, original return locations, inventories, and remaining shared
  revives carry forward.
- Rooms, encounters, checkpoints, cache claims, and floor rewards reset so the
  new layout is fully independent.
- Completed floor layouts are deleted with the existing server-thread,
  block-budgeted cleanup queue. Old instance slots remain reserved until deletion
  completes, preventing another run from entering partially cleaned geometry.
- Fixed the floor-handoff crash caused by cleanup appearing in the same tick that
  generation finished. Teleport now requires a loaded, non-empty entrance floor
  with clear headroom, and old-floor deletion waits until its player cell is empty.
- Floor number and retired cleanup work are stored in versioned SavedData. Server
  restarts resume incomplete floor generation or deletion without sending the
  party to the Overworld.

## Verification

- Added GameTest coverage for floor 0 → 1 → 2 progression, fresh seeds/layouts,
  preserved run state, retired-floor tracking, and codec round trips.
- The release build runs the 1,000-seed Archive simulation through `check`.
