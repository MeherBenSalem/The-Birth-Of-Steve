# Market analysis: where 1.0 fits (October 2026)

This is the evidence behind the 1.0 "Remember or Forget" scope and the rename to
**Remnants: Roguelite Dungeons**. Numbers are total downloads from CurseForge and
Modrinth listings and Modrinth's per-version data, read in late October 2026.

## Who owns each category

| Category | Leaders | Shape of the offer |
| --- | --- | --- |
| Dungeon structures | YUNG's Better Dungeons (~158M), When Dungeons Arise (~141M), Dungeon Crawl (89M), Explorify (71M), Dungeons and Taverns (33M) | World generation: find it, loot it once, move on |
| Instanced runs | Vault Hunters 3 (pack, 1.18.2), Mine and Slash (1.20.1 Forge), Dungeon Realm (1.20.1 Forge), Solo Leveling: Reawakening (1.21.1 NeoForge) | Old, or locked to one loader |
| Modern roguelites | MCEndgame, DungeonRPG, Hextech Lib, Grandia, Limitbreak (all under 5K) | Nobody has broken out |
| Classes, spells, combat | Better Combat (84M), Iron's Spells (71M), Pufferfish's Skills (36M), Epic Fight (30M), the RPG Series | Saturated |
| Loot | Artifacts (149M), Apotheosis (136M), Loot Integrations (83M), Lootr (24M) | Apotheosis is Forge/NeoForge only after 1.20.1 |
| Quests and parties | FTB Quests (260M), Open Parties and Claims (24M) | Parties exist, but nothing hands them a dungeon |

RPG modpacks still sit mostly on 1.20.1 (Prominence II, Craft to Exile 2), with
1.21.1 next. 26.x packs are only starting.

## Version gaps

- The ARPG and run stack is locked to Forge and NeoForge. Fabric has no Apotheosis
  after 1.20.1, and no Iron's Spells, Epic Fight, Gateways to Eternity or Mine and
  Slash.
- 26.1 shipped unobfuscated and broke every older mod. 26.2 and 26.3 have almost no
  RPG content, so being there first matters.
- This mod had no 26.3 build. 1.0 adds it.

## The Remnants family's lane

Remnant Bosses (bosses and a dimension), Remnants: RPG Mobs (mob levels, ranks and
traits) and RPG Attribute System (player levels, attributes and Valor) already
cover the character and the enemies. What the family lacks is a **repeatable
activity that tests a build again and again**, with an endgame ladder. That is
this mod's job, so 1.0 adds no class or skill system of its own.

## Gaps, ranked by demand and feasibility

1. **A modern Vault Hunters / Hades loop.** Enter, push your luck, bank or lose,
   come back stronger. The standalone Vault Hunters mod "does not function
   independently", and the alternatives are 1.20.1 Forge or tiny.
   *1.0: Memory Keys, Remember or Forget, the Archive Codex.*
2. **A cross-loader endgame on 26.3.** No numbered difficulty ladder exists on Fabric
   or 26.x. *1.0: Fracture Tiers 1-20 and eight Distortions (a heat system).*
3. **A reason to revisit structures.** Over 400M downloads of one-time structure
   mods sit next to several mods whose only purpose is making vaults reopenable.
   *1.0: Recollections. Use a Memory Key inside any vanilla or modded structure to
   open an instanced echo of it.*
4. **Co-op that is designed for, not tolerated.** Lootr's 24M downloads show that
   players care about fair loot. No dungeon mod scales to party size and hands the
   party its own frames. *1.0: `/party`, party frames, per-member loot and rewards,
   party-scaled encounters.*
5. **Seeded competition.** Generation here is already deterministic. *1.0: the
   Weekly Recollection and the Hall of Records.*
6. **Loot worth the risk.** *1.0: Relics, gear with six rarities and rolled
   affixes, scaled by tier.*
7. **Modpack glue.** *1.0: RPG Attribute System Valor on Remember, and Archive
   creatures excluded from Remnants: RPG Mobs' leveling so tiers stay readable.*

## Naming

| Term | CurseForge results | Modrinth results |
| --- | --- | --- |
| RPG | 1,101 | 426 |
| dungeon | 508 | 393 |
| roguelike | 43 | 20 |
| roguelite | 23 | 4 |

One high-volume term ("Dungeons") paired with a low-competition one ("Roguelite"),
under the family's "Remnants" prefix: **Remnants: Roguelite Dungeons**. "The Birth
of Steve" stays as the in-game prologue campaign.

Names to avoid: anything with "Archivist" (two horror mods use it), "Echoes" on its
own (Echoes of The End, Echoed Remnants), "Remnants of the Past" and "Fractured"
alone. "Yesterglass" returns no results, so the mod can own that word.

The mod id stays `tbos`, so 0.x worlds keep their blocks and items.
