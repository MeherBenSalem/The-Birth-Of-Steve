# The Birth of Steve v0.7.0

### New Features
* **Waystones.** Each Fractured Archive floor generates exactly one Waystone chamber off the start.
* Right-click a dungeon Waystone to save that floor and return to the Overworld without ending the run.
* Right-click any Overworld Waystone — including one you craft — to resume at the last floor you bound.
* Crafted Waystones use Archive Stone, Chronicle Bronze, and Lenswork Crystal. Generated dungeon copies cannot be broken; the ones you place can.

### Improvements
* The Last Curator no longer requires Memory Anchors to expose its core. Crossing 200 or 100 health seals it for three seconds, then it takes damage again.
* Afterimages in Erasure detonate on a timer instead of forcing arena shifts.

### Bug Fixes
* Using a Waystone inside the Fractured Archive now stays in the Overworld. The active run no longer teleports you back to the dungeon checkpoint after you leave.
* Dungeon Waystones no longer resume you onto the same floor instead of sending you home.
* The Last Curator fight no longer crashes the Minecraft 26.1.2 / 26.2 client when the core is exposed or the fight enters Revision.

### Configuration
* None

### Compatibility
* Drop-in update from 0.6.0.
* Shared version **0.7.0** on every shipped target:
  * Fabric + Forge 1.20.1
  * Fabric + NeoForge 1.21.1
  * Fabric + NeoForge 26.1.2
  * Fabric + NeoForge 26.2
* Existing worlds keep Archive runs and site progress. Bind a Waystone on the current floor after updating if you want the new leave/resume path.

### Upgrade Notes
1. Replace the old `tbos-*-0.6.0.jar` with the matching `tbos-*-0.7.0.jar` for both your Minecraft version and your loader.
2. Restart the client or dedicated server.
3. Confirm the log lists **The Birth of Steve 0.7.0**.
4. In an active Archive run, right-click the floor Waystone once to bind, then confirm you remain in the Overworld.
