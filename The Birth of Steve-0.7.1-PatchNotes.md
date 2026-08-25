# The Birth of Steve v0.7.1

### New Features
* None

### Improvements
* **Archivist's Journal uses a 3D book model.** Inventory and hand now show the Blockbench tome (with teal accents matching the Journal UI) instead of the flat PNG icon.
* Journal screen title matches the item name (“Archivist's Journal”).
* Fracture Coffer overlays and quest flavor no longer point at a second notes book.

### Bug Fixes
* None

### Removals
* **Last Archivist's Notes (`starter_tome`) removed.** Coffers no longer drop it; the Notes reader screen is gone. Memory Plates still carry the six scene texts.

### Configuration
* None

### Compatibility
* Drop-in update from 0.7.0 for all shipped targets.
* Shared version **0.7.1** on every shipped target:
  * Fabric + Forge 1.20.1
  * Fabric + NeoForge 1.21.1
  * Fabric + NeoForge 26.1.2
  * Fabric + NeoForge 26.2
* Worlds that still contain `tbos:starter_tome` stacks will show an unknown item for those stacks.

### Upgrade Notes
1. Replace the old `tbos-*-0.7.0.jar` with the matching `tbos-*-0.7.1.jar` for both your Minecraft version and your loader.
2. Restart the client or dedicated server.
3. Confirm the log lists **The Birth of Steve 0.7.1**.
4. Confirm the Archivist's Journal shows as a 3D book and that coffers no longer grant Notes.
