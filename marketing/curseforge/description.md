![Remnants: Roguelite Dungeons 1.0: Remember or Forget](https://raw.githubusercontent.com/MeherBenSalem/The-Birth-Of-Steve/main/marketing/images/banner.png)

## Every ruin has a before

Craft a **Memory Key**, attune it at a Waystone and lead your party into the **Fractured Archive**, a procedural dungeon that builds a new floor every time you go deeper. Every room you clear adds Renown and Relics to your **Recollection**. At each floor's gateway you choose: **Remember** it all and take it home, or **Forget** it and go a floor deeper for a bigger payout, knowing that a wipe loses everything you have not banked. Banked Renown levels your **Archive Codex**, and its Insights make every run after it stronger.

**1.0 "Remember or Forget"** turns *The Birth of Steve* into **Remnants: Roguelite Dungeons**. The authored campaign stays as its prologue. Memory Keys, Distortions, Recollections, Relics, the Archive Codex, the Weekly Recollection and parties are new.

![divider](https://raw.githubusercontent.com/MeherBenSalem/The-Birth-Of-Steve/main/marketing/images/divider.png)

![Expeditions](https://raw.githubusercontent.com/MeherBenSalem/The-Birth-Of-Steve/main/marketing/images/header-expeditions.png)

![Attune a key](https://raw.githubusercontent.com/MeherBenSalem/The-Birth-Of-Steve/main/marketing/images/card-attunement.png)

1. Craft a **Memory Key**: four Amethyst Shards in the corners, four Gold Ingots on the edges and an Ender Pearl in the centre. A crafted key is tier 1.
2. Use it on a **Waystone**, on the **Curator Gateway**, or anywhere inside a structure. The **Attunement** screen opens: the tier, the Distortions, who is coming, the reward factor and the Relic odds.
3. Toggle the Distortions you want, then press **Begin**. Party members within 24 blocks come with you.

Keys go up to **tier 20**. Each tier above 1 gives every enemy in the run 10% more health and 5% more damage, so a tier 20 key means ×2.9 health and ×1.95 damage. Remembering hands back a better key, so the tiers climb as you do.

![Turn up the Heat](https://raw.githubusercontent.com/MeherBenSalem/The-Birth-Of-Steve/main/marketing/images/card-distortions.png)

Each Distortion adds one **Heat**. **Fracture** is the tier plus the Heat, and every point of Heat adds 15% to the Renown you earn.

| Distortion | Effect |
| --- | --- |
| **Fortified** | Enemies other than wardens and floor bosses have 30% more health |
| **Tyrannical** | Wardens and floor bosses have 40% more health and deal 15% more damage |
| **Bolstering** | When an enemy falls, its allies within 10 blocks gain Strength for 10 s, stacking to Strength III |
| **Volatile** | Fallen enemies burst after 1.5 s for 4 + tier/4 damage within 3 blocks. A hiss and sparks warn you, and it never breaks blocks |
| **Haunted** | Every 30 s of combat a Shade, a half-health Parallax Wraith, slips in beside one of you |
| **Frailty** | One fewer shared revive |
| **Hourglass** | 8 minutes per floor. Beat the boss in time for +1 key tier and 25% more Renown |
| **Amnesia** | Living Memories drafts offer two choices instead of three |

![Remember or Forget](https://raw.githubusercontent.com/MeherBenSalem/The-Birth-Of-Steve/main/marketing/images/header-choice.png)

![Remember or Forget](https://raw.githubusercontent.com/MeherBenSalem/The-Birth-Of-Steve/main/marketing/images/card-choice.png)

Every cleared room adds **15 × R** Renown to your personal Recollection, a lesser warden **40 × R** and a floor boss **120 × R** plus a Relic, where R = 1 + 0.12 × (tier − 1) + 0.15 × Heat. When the floor's reward room is clear, the choice opens on its own:

- **Remember** banks your Recollection multiplied by **Momentum** and sends you home with a Memory Key of the tier plus the floors you cleared. Every Hourglass you beat adds one more tier.
- **Forget** is a vote to go deeper. Once everyone still in the run has voted, the next floor opens and Momentum rises by 0.25. It starts at ×1.00, so three Forgets in, everything you bank counts double.
- **Failure** loses it all. When the shared revives run out or the run is abandoned, every unbanked Recollection is gone and the key's holder gets back a key one tier lower.

Banking is a small ceremony: the Renown counts up, the Codex level rolls over and each Relic turns over card by card with its own chime. Press **N** or use `/expedition choose` to bring the choice back if you close it.

![The Fractured Archive](https://raw.githubusercontent.com/MeherBenSalem/The-Birth-Of-Steve/main/marketing/images/header-archive.png)

![Floor after floor](https://raw.githubusercontent.com/MeherBenSalem/The-Birth-Of-Steve/main/marketing/images/card-archive.png)

- **A new floor every time.** The server builds each floor from a fresh seed, from eight cycling floor themes and twenty room templates, and never repeats the previous layout.
- **Creatures that telegraph.** Five original Archive creatures and sixteen theme exclusives, each with its own model, animation, voice and a readable windup for its signature move.
- **Wardens and floor bosses.** One to three lesser wardens guard every floor, and a floor boss waits behind a gate that opens once enough of the floor is clear.
- **Rooms with a twist.** Nine room modifiers, from Darkness to regenerating guardians, plus traps, secret rooms and caches that roll loot for every party member.
- **The Expedition HUD** shows the tier, the Distortions, your Recollection, Momentum and the Hourglass above the hotbar.

![Build as you delve](https://raw.githubusercontent.com/MeherBenSalem/The-Birth-Of-Steve/main/marketing/images/card-memories.png)

**Living Memories** are your build inside the Archive. Every encounter room you clear earns a draft: pick one of three abilities or artifacts from the Archivist's Journal. Six abilities sit on their own keys, from Echo Lance to Memory Well, and twelve artifacts change how they behave, such as Seeking Glass bending your Lances toward enemies or Ember Script setting what they hit alight. The **Second Draft** Insight adds a **Rewrite Draft** button, also available as `/expedition reroll`.

![Recollections](https://raw.githubusercontent.com/MeherBenSalem/The-Birth-Of-Steve/main/marketing/images/header-recollections.png)

![Every ruin has a before](https://raw.githubusercontent.com/MeherBenSalem/The-Birth-Of-Steve/main/marketing/images/card-recollections.png)

- Use a Memory Key anywhere inside a structure, from a village or a stronghold to the Trial Chambers or another mod's dungeon, to open a **Recollection** of that place.
- The Expedition is seeded from that structure and named after it, for example *Recollection of the Plains Village*.
- Each structure can be recollected once per player. Add structures to the `#tbos:unrecollectable` tag to exclude them.

![The Archive Codex](https://raw.githubusercontent.com/MeherBenSalem/The-Birth-Of-Steve/main/marketing/images/header-codex.png)

![Stronger every run](https://raw.githubusercontent.com/MeherBenSalem/The-Birth-Of-Steve/main/marketing/images/card-codex.png)

Open the Codex with **N** (rebindable) or `/codex`. Progress is per player. Banked Renown levels it from 1 to **30**: level L to L + 1 costs 150 + 50 × L Renown, and every level grants one **Insight**. Respec is free outside a run.

| Insight | Ranks | Effect |
| --- | --- | --- |
| Steady Hand | 1 | +1 shared revive per Expedition |
| Deep Recall | 3 | +10% Renown per rank |
| Keen Eye | 3 | Better Relic rarity odds |
| Archivist's Grace | 3 | +2 max health per rank inside the Archive |
| Bulwark Memory | 3 | +1 armor per rank inside the Archive |
| Echo Strike | 3 | 5% per rank: a melee hit strikes again for half damage |
| Vampiric Recall | 2 | Heal 1 health per rank on an Archive kill |
| Hourglass Mastery | 2 | +60 s of Hourglass time per rank |
| Swift Recollection | 1 | Speed I for 30 s when a floor begins |
| Waystone Ward | 1 | Resistance I for 10 s when you arrive on a floor |
| Second Draft | 1 | Rewrite a Living Memories draft once per floor |
| Lucid Start | 1 | Begin each Expedition with a random Living Memory artifact |
| Keymaker | 1 | Remembered keys gain +1 tier |
| Hoarder | 1 | +1 Relic when you Remember |
| Party Bond | 1 | 5% less damage taken per party member within 16 blocks, up to 15% |
| Last Stand | 1 | Once per floor, a lethal blow leaves you at 1 health instead |

![Know what hunts you](https://raw.githubusercontent.com/MeherBenSalem/The-Birth-Of-Steve/main/marketing/images/card-bestiary.png)

The **Bestiary** lists all 23 Archive creatures, eight of them floor bosses. Slay one and its entry opens: your kills, its home floor, its threat and a line of lore. The rest stay sealed. Record them all for an advancement.

![Hall of Records](https://raw.githubusercontent.com/MeherBenSalem/The-Birth-Of-Steve/main/marketing/images/card-records.png)

- **Records** keeps your personal bests: highest tier and Fracture Remembered, deepest Expedition, Renown banked, Relics by rarity and your fastest Hourglass floor.
- The server's **Hall of Records** ranks the top ten by highest tier Remembered, then deepest floor.
- The **Weekly Recollection** gives everyone on the server the same key each week: tier 6 to 10, two sealed Distortions and a fixed seed, all derived from the world seed and the week. Claim it from the Codex. The week's deepest floors are ranked. Weeks start on Monday at 00:00 UTC.

![Relics](https://raw.githubusercontent.com/MeherBenSalem/The-Birth-Of-Steve/main/marketing/images/header-relics.png)

![Loot with a story](https://raw.githubusercontent.com/MeherBenSalem/The-Birth-Of-Steve/main/marketing/images/card-relics.png)

Floor bosses drop **Relics**: swords, axes, bows, crossbows and armor with a rarity, rolled affixes, a generated name and a line of lore. The material follows the tier, iron up to tier 6, diamond up to 13 and netherite beyond, and the item level is 10 + 5 × tier. Higher tiers and the Keen Eye Insight shift the odds toward the rarer grades.

| Rarity | Affixes |
| --- | --- |
| Common | 1 |
| Uncommon | 2 |
| Rare | 2 |
| Epic | 3 |
| Legendary | 4 |
| **Remnant** | 4 plus a signature, from tier 10 |

- **Weapon affixes:** Keen, Quick, Thirsting, Smoldering, Echoing, Archive-Breaking and Reaping.
- **Armor affixes:** Stalwart, Plated, Tempered, Fleet, Rooted, Mirrored, Warded and Storied.
- **Remnant signatures:** Last Hour (25% less damage taken below 30% health), Fracture Step (Speed II for 3 s after a kill), Choir's Echo (every fifth hit deals double damage) and Yesterglass (kills add 5 s to the Hourglass).
- A Rare or better find raises a beam of its colour over you, and an Epic or better one is announced to your party.
- Relics stay ordinary vanilla items with their own name, lore and attribute modifiers, so enchanting, anvils, Mending and inventory mods all keep working.

![Parties](https://raw.githubusercontent.com/MeherBenSalem/The-Birth-Of-Steve/main/marketing/images/header-parties.png)

![Delve together](https://raw.githubusercontent.com/MeherBenSalem/The-Birth-Of-Steve/main/marketing/images/card-parties.png)

- `/party create`, `invite <player>`, `accept`, `decline`, `leave`, `kick <player>`, `disband` and `list`. No operator permission needed.
- Up to **4 players**. Invites expire after 60 s, and parties survive restarts.
- Party members within 24 blocks of the key holder join the Expedition. The run shares a pool of revives, while loot and Recollections stay personal.
- **Party frames** on the left of the screen show each member's health, absorption and whether they are in the Archive.

![Configuration](https://raw.githubusercontent.com/MeherBenSalem/The-Birth-Of-Steve/main/marketing/images/header-config.png)

![Every number is yours](https://raw.githubusercontent.com/MeherBenSalem/The-Birth-Of-Steve/main/marketing/images/card-config.png)

- `config/tbos-expeditions.json`: tier scaling, Distortion strength, Renown rewards, the Hourglass, the Codex curve, Relic materials and odds, the Weekly tier band, party size and the key recipe. Each section starts with an `_about` line saying what it controls.
- `config/tbos-common.json`: the dungeon itself.
- `#tbos:unrecollectable` excludes structures from Recollections.
- Every number on this page is a 1.0.0 default.

![divider](https://raw.githubusercontent.com/MeherBenSalem/The-Birth-Of-Steve/main/marketing/images/divider.png)

![Everything Else](https://raw.githubusercontent.com/MeherBenSalem/The-Birth-Of-Steve/main/marketing/images/header-everything.png)

- **The prologue.** *The Birth of Steve* is still here: Fracture Shrines in every Overworld, the Yesterglass Lens, the Meridian Archive's four puzzle sites and the Last Curator. Defeating the Curator turns the Archive Core into the Curator Gateway.
- **Fourteen advancements** in a new Expeditions tab.
- **27 new sound effects**, a new GUI icon set and a new mod logo.
- **Commands:** `/codex`, `/expedition`, `/expedition choose`, `/expedition reroll` and `/party ...` for everyone. Operators also get `/expedition key`, `weekly_key` and `relic`, and `/codex renown` and `reset`.
- **The Remnants family.** With **RPG Attribute System** installed, Remembering also grants Valor worth a quarter of the banked Renown. With **Remnants: RPG Mobs**, Archive creatures are in `#remnant_mobs:never_leveled`, so their strength comes from the Fracture Tier alone. Neither is required.

![Downloads & Compatibility](https://raw.githubusercontent.com/MeherBenSalem/The-Birth-Of-Steve/main/marketing/images/header-compatibility.png)

| Minecraft | Loaders | Java | Also install |
| --- | --- | --- | --- |
| 1.20.1 | Fabric, Forge | 17+ | Fabric API (Fabric only) |
| 1.21.1 | Fabric, NeoForge | 21+ | Fabric API (Fabric only) |
| 26.1.2 | Fabric, NeoForge | 25+ | Fabric API (Fabric only) |
| 26.2 | Fabric, NeoForge | 25+ | Fabric API (Fabric only) |
| 26.3 | Fabric, NeoForge | 25+ | Fabric API (Fabric only) |

- Download the file with your loader and Minecraft version in its name, for example `tbos-fabric-26.1.2-1.0.0.jar` or `tbos-forge-1.20.1-1.0.0.jar`.
- Install it on **both the server and every client**, at the same version.
- Updating from The Birth of Steve keeps your worlds: the mod id is still `tbos`.
- This is a mod. It does not work as a Paper, Spigot or Folia plugin.

![Questions](https://raw.githubusercontent.com/MeherBenSalem/The-Birth-Of-Steve/main/marketing/images/header-faq.png)

**Do I have to play the campaign first?**
No. Any Waystone works, and a Memory Key used inside any structure opens a Recollection straight away. Craft a key and go. The Curator Gateway opens when you defeat the Last Curator.

**What happens if we die?**
Each run has a pool of shared revives. A death spends one and sends you back to your checkpoint. When the pool runs out the run fails and every unbanked Recollection is lost. Renown and Relics you have already Remembered are safe.

**Is Forget ever worth it?**
Every Forget adds 0.25 Momentum to the whole Recollection, so three floors deep everything you bank counts double. The deeper floors also hand back a better key. The risk is losing all of it.

**Can I change the numbers?**
Yes. Every value on this page lives in `config/tbos-expeditions.json`: tier scaling, Distortion strength, Renown rewards, the Codex curve, Relic odds, the Weekly tier band and party size. The dungeon itself is tuned in `config/tbos-common.json`.

**Does it work with other loot and RPG mods?**
Relics are ordinary vanilla weapons and armor with names, lore and attribute modifiers, so enchanting, anvils, Mending, sorting and tooltip mods keep working. Other mods' structures can be recollected.

**Can I play solo?**
Yes. Parties are optional, and every number is tuned for one to four players.

**Where are the docs?**
[Getting started](https://github.com/MeherBenSalem/The-Birth-Of-Steve/blob/main/docs/getting-started.md) · [Remember or Forget, every rule and default](https://github.com/MeherBenSalem/The-Birth-Of-Steve/blob/main/docs/remember-or-forget.md) · [The Fractured Archive](https://github.com/MeherBenSalem/The-Birth-Of-Steve/blob/main/docs/echoes-of-the-past.md) · [Release notes](https://github.com/MeherBenSalem/The-Birth-Of-Steve/blob/main/patchnotes/1.0.0.md) · [Issues](https://github.com/MeherBenSalem/The-Birth-Of-Steve/issues)

![Gallery](https://raw.githubusercontent.com/MeherBenSalem/The-Birth-Of-Steve/main/marketing/images/header-gallery.png)

![A floor boss hall in the Fractured Archive](https://raw.githubusercontent.com/MeherBenSalem/The-Birth-Of-Steve/main/marketing/images/screenshots/archive-boss.jpg)

![An Archive room](https://raw.githubusercontent.com/MeherBenSalem/The-Birth-Of-Steve/main/marketing/images/screenshots/archive-room.jpg)

![The Expedition HUD during a floor boss fight](https://raw.githubusercontent.com/MeherBenSalem/The-Birth-Of-Steve/main/marketing/images/screenshots/expedition-hud.jpg)

![Attuning a tier 7 key at a Waystone](https://raw.githubusercontent.com/MeherBenSalem/The-Birth-Of-Steve/main/marketing/images/screenshots/attunement.jpg)

![A Recollection of a plains village](https://raw.githubusercontent.com/MeherBenSalem/The-Birth-Of-Steve/main/marketing/images/screenshots/recollection.jpg)

![Remember or Forget](https://raw.githubusercontent.com/MeherBenSalem/The-Birth-Of-Steve/main/marketing/images/screenshots/choice.jpg)

![Remembered: Renown, a Relic and a new key](https://raw.githubusercontent.com/MeherBenSalem/The-Birth-Of-Steve/main/marketing/images/screenshots/banked.jpg)

![A Living Memories draft](https://raw.githubusercontent.com/MeherBenSalem/The-Birth-Of-Steve/main/marketing/images/screenshots/memory-draft.jpg)

![The Archive Codex: Insights](https://raw.githubusercontent.com/MeherBenSalem/The-Birth-Of-Steve/main/marketing/images/screenshots/codex-insights.jpg)

![The Archive Codex: Bestiary](https://raw.githubusercontent.com/MeherBenSalem/The-Birth-Of-Steve/main/marketing/images/screenshots/codex-bestiary.jpg)

![The Archive Codex: Records](https://raw.githubusercontent.com/MeherBenSalem/The-Birth-Of-Steve/main/marketing/images/screenshots/codex-records.jpg)

![The Archive Codex: Weekly](https://raw.githubusercontent.com/MeherBenSalem/The-Birth-Of-Steve/main/marketing/images/screenshots/codex-weekly.jpg)

![A Legendary Relic](https://raw.githubusercontent.com/MeherBenSalem/The-Birth-Of-Steve/main/marketing/images/screenshots/relic-legendary.jpg)

![A Remnant Relic](https://raw.githubusercontent.com/MeherBenSalem/The-Birth-Of-Steve/main/marketing/images/screenshots/relic-remnant.jpg)
