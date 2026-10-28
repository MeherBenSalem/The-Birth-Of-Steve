# 1.0 "Remember or Forget"

1.0 turns the Fractured Archive into a repeatable endgame. You run it with a party,
choose your difficulty and push your luck. The mod is renamed **Remnants: Roguelite
Dungeons**; *The Birth of Steve* stays as its prologue campaign. Why these features,
and not others, is in [`market-analysis.md`](market-analysis.md).

Every number below is a default from `config/tbos-expeditions.json`.

## The loop

1. Craft or earn a **Memory Key**. Attune it: pick its **Distortions**.
2. Open an **Expedition** by using the key on a Waystone or the Curator Gateway, or
   anywhere inside a structure (a **Recollection**).
3. Clear floors. Each victory adds Renown, Relics and a better key to your
   **Recollection**: the rewards you have not banked yet.
4. At every floor's gateway, choose. **Remember** banks everything and takes you
   home. **Forget** goes deeper, adds Momentum to the whole Recollection, and puts it
   all at risk.
5. Banked Renown levels up your **Archive Codex**. Spend Insight on permanent perks,
   fill the Bestiary, and climb the Hall of Records.

## Memory Keys and Fracture Tiers

`tbos:memory_key` stores a tier from 1 to 20 and a set of Distortions on the stack.
A freshly crafted key is tier 1. The recipe is four Amethyst Shards, four Gold
Ingots and an Ender Pearl, with the pearl in the centre.

Using the key on a block opens the **Attunement** screen. It shows the tier, the
Distortions, the party that will come along, and a preview of the rewards.
**Begin** asks the server to check everything again before the run starts:

| Clicked | Expedition |
| --- | --- |
| Any Waystone (dungeon or crafted) outside the Archive | Archive Expedition |
| The Curator Gateway | Archive Expedition |
| Any block while standing inside a structure | Recollection of that structure |
| Anything else | Refused, with a hint |

Tier scales every enemy in the run:

- **Health:** ×(1 + 0.10·(T−1)), so ×2.9 at tier 20.
- **Damage:** ×(1 + 0.05·(T−1)), so ×1.95 at tier 20.

Bad Omen still turns a run Ominous, independently of the tier.

### Distortions (heat)

Each active Distortion adds one point of **Heat**. Rewards scale with
**Fracture**, which is tier + Heat.

| Distortion | Effect |
| --- | --- |
| **Fortified** | Non-boss enemies +30% health |
| **Tyrannical** | Wardens and floor bosses +40% health and +15% damage |
| **Bolstering** | When an enemy dies, enemies within 10 blocks gain Strength for 10 s, stacking to Strength III |
| **Volatile** | Fallen enemies burst after 1.5 s, dealing 4 + tier/4 damage within 3 blocks. A hiss and sparks warn you; it never breaks blocks |
| **Haunted** | Every 30 s during combat a Shade, a half-health Parallax Wraith, slips in near a random party member |
| **Frailty** | One fewer shared revive |
| **Hourglass** | Each floor has an 8-minute timer. Beat the boss in time and the reward key gains +1 tier and the floor's Renown +25% |
| **Amnesia** | Living Memories drafts offer two choices instead of three |

## The Recollection: Remember or Forget

Each party member has a personal Recollection for the run:

| Source | Renown |
| --- | --- |
| Combat, puzzle or trap room cleared | 15 × R |
| Lesser warden defeated | 40 × R |
| Floor boss defeated | 120 × R, plus one Relic |

R is the reward factor, 1 + 0.12·(T−1) + 0.15·Heat.

- **Remember** at the gateway: bank the Recollection, multiplied by **Momentum**.
  You also receive a Memory Key of tier T + floors cleared (capped at 20, +1 per
  Hourglass beaten), and, with RPG Attribute System installed, Valor equal to a
  quarter of the banked Renown. Solo, or as the last member left, the run ends in
  victory. In a larger party, you leave and the others carry on.
- **Forget**: vote to descend. When every member still in the run has voted, the
  next floor opens and Momentum rises by 0.25. It starts at ×1.0.
- **Failure** (the shared revives run out, or the run is abandoned) forfeits every
  unbanked Recollection. The key's holder gets back a dimmed key, one tier lower.
- Leaving by Waystone banks nothing. The Recollection waits with the run.

Every run is an expedition. Entering through the Curator Gateway without a key
counts as tier 1 with no Distortions.

## Recollections: echoes of any structure

Using a key inside any vanilla or modded structure opens an Expedition seeded from
that structure's position and named after it: *Recollection of the Trial
Chambers*. Each structure can be recollected once per player. Structures in
`#tbos:unrecollectable` are excluded (none by default).

## Parties

`/party create · invite <player> · accept · decline · leave · kick <player> ·
disband · list`. No operator permission is needed. A party holds up to four players,
and invites expire after 60 s. Parties survive restarts.

- Party members within 24 blocks of the key holder join the expedition. Scoreboard
  teams still work at the Curator Gateway.
- **Party frames** on the left of the screen show each member's health, absorption
  and whether they are in the Archive.
- Loot is already per member: every cache rolls for each member who has not claimed
  it. Recollections are personal.

## The Archive Codex

Open it with **N** (rebindable) or `/codex`. Progress is per player and kept in
`<world>/data/tbos/codex/<uuid>.json`.

- **Renown** levels run from 1 to 30. Level L → L+1 costs 150 + 50·L Renown. Each
  level grants one **Insight**.
- **Insights**: permanent perks, one Insight per rank. Respec is free outside a run.

| Insight | Ranks | Effect |
| --- | --- | --- |
| Steady Hand | 1 | +1 shared revive per expedition |
| Deep Recall | 3 | +10% Renown per rank |
| Keen Eye | 3 | Better Relic rarity odds |
| Archivist's Grace | 3 | +2 max health per rank inside the Archive |
| Bulwark Memory | 3 | +1 armor per rank inside the Archive |
| Echo Strike | 3 | 5% per rank: a melee hit in the Archive strikes again for half damage |
| Vampiric Recall | 2 | Heal 1 health per rank on an Archive kill |
| Hourglass Mastery | 2 | +60 s Hourglass time per rank |
| Swift Recollection | 1 | Speed I for 30 s when a floor begins |
| Waystone Ward | 1 | Resistance I for 10 s on arriving on a floor |
| Second Draft | 1 | Reroll a Living Memories draft once per floor |
| Lucid Start | 1 | Begin each expedition with a random Living Memory artifact |
| Keymaker | 1 | Remembered keys gain +1 tier |
| Hoarder | 1 | +1 Relic when you Remember |
| Party Bond | 1 | 5% less damage taken per party member within 16 blocks (up to 15%) |
| Last Stand | 1 | Once per floor, a lethal blow leaves you at 1 health instead |

- **Bestiary**: every Archive creature you have slain. It shows kills, its home
  floor theme and a line of lore. Creatures you have not slain stay sealed.
- **Records**: your personal bests, and the server's **Hall of Records** (top ten
  by highest tier Remembered, then deepest floor).
- **Weekly Recollection**: one key a week for everyone on the server. Its seed,
  tier (6 to 10) and two Distortions are fixed and derived from the world seed and
  the week number (weeks start Monday 00:00 UTC). Claim it from the Codex. Its
  Distortions cannot be changed. The deepest floor reached is ranked.

## Relics

Relics are vanilla weapons and armor (swords, axes, bows, crossbows, and helmet to
boots), recovered with a rarity and rolled affixes. The base material follows the
tier: iron up to tier 6, diamond up to 13, then netherite. Item level = 10 + 5·T.

| Rarity | Colour | Affixes | Base weight (tier 1) |
| --- | --- | --- | --- |
| Common | grey | 1 | 50 |
| Uncommon | green | 2 | 30 |
| Rare | blue | 2 | 15 |
| Epic | purple | 3 | 4.5 |
| Legendary | orange | 4 | 0.5 |
| Remnant | red | 4 + a signature | 0 (from tier 10) |

Each tier above 1 and each Keen Eye rank shift weight from Common towards the rarer
grades.

Weapon affixes:

- Keen: +attack damage
- Quick: +attack speed
- Thirsting: heal part of the damage you deal
- Smoldering: chance to ignite
- Echoing: chance to strike again
- Archive-Breaking: bonus damage to Archive creatures
- Reaping: bonus damage to targets under 30% health

Armor affixes:

- Stalwart: +max health
- Plated: +armor
- Tempered: +toughness
- Fleet: +speed
- Rooted: +knockback resistance
- Mirrored: reflects melee damage
- Warded: absorption on kill
- Storied: +Renown

Remnant signatures:

- Last Hour: −25% damage taken below 30% health
- Fracture Step: Speed II for 3 s on a kill
- Choir's Echo: every fifth hit deals double damage
- Yesterglass: kills extend the Hourglass by 5 s

Relics are vanilla items with their own name, lore and attribute modifiers, so they
work with every enchantment, anvil, mending and inventory mod. Rarer finds sound a
chime by rarity and raise a coloured beam over you. Epic or better is announced to
your party.

## Remnants family integration

- **RPG Attribute System**: Remember grants Valor. The mod finds it by reflection,
  so it runs fine without it.
- **Remnants: RPG Mobs**: Archive creatures are added to
  `#remnant_mobs:never_leveled`, so their strength comes from the Fracture Tier
  alone.

## Implementation map

New, version-neutral packages under `com.nightbeam.tbos`:

- `expedition/`: keys, Distortions, tier scaling, Recollections, the launcher
  and run observation.
- `codex/`: player profiles, Renown, Insights, Bestiary, records, Weekly.
- `relic/`: rarity, affixes, generation and combat effects.
- `party/`: parties and the `/party` commands.
- `rpg/`: event hub, JSON network channel, persistence.
- `client/rpg/`: Canvas and screen seams, the Codex, Attunement and Recollection
  screens, and the HUD.

Version differences go through the seams. `compat/RpgCompat` covers the server side
and `client/rpg/Canvas` plus `client/rpg/RpgScreen` the client side. Each target
overrides only those files, so the feature code itself is never forked.
