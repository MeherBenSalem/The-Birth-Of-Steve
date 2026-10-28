#!/usr/bin/env python3
"""Regenerates the 1.0 data and resources: language entries, the Expeditions
advancement tab, the Memory Key recipe and model, tags and sounds.json entries.

Everything 1.0 adds is written from this file, so the copy, ids and numbers stay
in one place. Run it from the repository root:

    python3 tools/rpg/build_data.py

It merges into shared/common/src/main/resources (and writes the per-version data
bodies 1.21.1 and 1.20.1 need into their overrides). Existing language entries
that 1.0 does not own are left untouched.
"""
from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SHARED = ROOT / 'shared' / 'common' / 'src' / 'main' / 'resources'
OVERRIDE_1211 = ROOT / '1.21.1' / 'overrides' / 'common' / 'src' / 'main' / 'resources'
OVERRIDE_1201 = ROOT / '1.20.1' / 'overrides' / 'common' / 'src' / 'main' / 'resources'
NS = 'tbos'


def write(path: Path, data, indent=2):
    path.parent.mkdir(parents=True, exist_ok=True)
    text = json.dumps(data, indent=indent, ensure_ascii=False) + '\n'
    path.write_text(text, encoding='utf-8')


# ---- Language -------------------------------------------------------------------------------------------

DISTORTIONS = {
    'fortified': ('Fortified', 'Enemies other than wardens and floor bosses have 30%% more health.'),
    'tyrannical': ('Tyrannical', 'Wardens and floor bosses have 40%% more health and deal 15%% more damage.'),
    'bolstering': ('Bolstering', 'When an enemy falls, its allies within 10 blocks grow stronger for 10 seconds, stacking three times.'),
    'volatile': ('Volatile', 'Fallen enemies burst 1.5 seconds later. Watch for the sparks and step away.'),
    'haunted': ('Haunted', 'Every 30 seconds of combat, a Shade slips in beside one of you.'),
    'frailty': ('Frailty', 'Your party has one fewer shared revive.'),
    'hourglass': ('Hourglass', 'Each floor runs on an 8-minute timer. Beat the boss in time for +1 key tier and 25%% more Renown.'),
    'amnesia': ('Amnesia', 'Living Memories drafts offer two choices instead of three.'),
}

INSIGHTS = {
    'steady_hand': ('Steady Hand', 'The first shared revive your party spends each expedition is returned.'),
    'deep_recall': ('Deep Recall', '+10%% Renown banked per rank.'),
    'keen_eye': ('Keen Eye', 'Each rank shifts Relic odds towards the rarer grades.'),
    'archivists_grace': ("Archivist's Grace", '+2 max health per rank inside the Archive.'),
    'bulwark_memory': ('Bulwark Memory', '+1 armor per rank inside the Archive.'),
    'echo_strike': ('Echo Strike', '5%% chance per rank for a hit in the Archive to strike again for half damage.'),
    'vampiric_recall': ('Vampiric Recall', 'Heal 1 health per rank when you slay something in the Archive.'),
    'hourglass_mastery': ('Hourglass Mastery', '+60 seconds on the Hourglass per rank.'),
    'swift_recollection': ('Swift Recollection', 'Speed I for 30 seconds whenever a floor begins.'),
    'waystone_ward': ('Waystone Ward', 'Resistance I for 10 seconds whenever a floor begins.'),
    'second_draft': ('Second Draft', 'Once per floor, rewrite a Living Memories draft (/expedition reroll or the Memory screen).'),
    'lucid_start': ('Lucid Start', 'Begin every expedition with a random Living Memory artifact.'),
    'keymaker': ('Keymaker', 'Keys you Remember gain +1 tier.'),
    'hoarder': ('Hoarder', '+1 Relic every time you Remember.'),
    'party_bond': ('Party Bond', 'Take 5%% less damage in the Archive for each party member within 16 blocks, up to 15%%.'),
    'last_stand': ('Last Stand', 'Once per floor, a blow that would kill you in the Archive leaves you at 1 health.'),
}

BESTIARY = {
    'phoenix_guardian': 'It tended the Grand Orrery long after the last reader left, and burned its own wings to keep the lamps lit. It does not know the Archive is closed.',
    'parallax_wraith': 'A reader caught between two drafts of the same page. Its plates trail its turn because it is never quite where it was.',
    'meridian_sentinel': "The Archive's clockwork warden keeps its own time in the rings where a head should be. It counts your steps, not your face.",
    'memory_leech': 'It drinks what a room remembers and swells with it. Leave one long enough and the walls forget their own names.',
    'lensward': 'A guardian ground from the same glass as your Lens. It charges its light slowly, and it rarely misses twice.',
    'minotaur': 'Something the Labyrinth wrote to keep itself company. It plants its feet before it swings; corridors are where it wins.',
    'shard_drifter': 'Three plates of a shattered window, orbiting the memory of the view. When the orbit quickens, it is already behind you.',
    'wake_cutter': "It drew the Wake's single line and has cut along it ever since. Watch the blade go back: once the dash starts, nothing stops it.",
    'gallery_moth': 'Drawn to the last light in any room, it takes that light with it when it goes.',
    'null_portrait': 'An empty gilt frame that remembers being looked at. Edge-on, it is harmless. Face-on, it is looking back.',
    'armillary_scout': 'A lens between two rings, sent ahead to measure the dark. The rings spin up before it dives.',
    'gnomon_knight': "The sundial's shadow given armor. The shield rises before the ground answers it.",
    'ash_chorister': 'The Choir burned and this voice kept singing. Its seam gapes wider the closer it comes to splitting in two.',
    'dust_cantorile': 'It leads the Choir of Dust. While its censer swings, every step you take is slower than the one before.',
    'shardling_swarm': "Splinters of the Vault's glass that never stopped falling. One is nothing; the swarm is the room.",
    'prism_stalker': "The Vault's keeper bends light around itself. You will see its reflection before you see it.",
    'shelf_crawler': 'A shelf that learned to move. The false shelves of the Catalogue are its nest.',
    'index_wight': 'It indexed everything in the Hollow Catalogue, then started indexing the readers.',
    'metronome_hound': "It keeps the Labyrinth's tempo. Its bite lands on the beat, so learn the beat.",
    'labyrinth_usher': 'It shows the way, but only one way, and never the way out.',
    'hour_cantor': "The Labyrinth's heart. Its caged pendulum swings tighter as the refrain approaches. Watch the chest, not the bar.",
    'blank_chronist': 'A historian whose pages were wiped clean. It writes on whatever it touches.',
    'hour_hand_wraith': "The Unwritten Hour's last minute, wearing a body. It sweeps the room the way a hand sweeps a dial.",
}

AFFIXES = {
    'keen': ('Keen', '+%s Attack Damage'),
    'quick': ('Quick', '+%s Attack Speed'),
    'thirsting': ('Thirsting', 'Heals %s of the damage you deal'),
    'smoldering': ('Smoldering', '%s chance to set the target ablaze'),
    'echoing': ('Echoing', '%s chance to strike again for 40%%'),
    'archive_breaking': ('Archive-Breaking', '+%s damage to Archive creatures'),
    'reaping': ('Reaping', '+%s damage to targets below 30%% health'),
    'stalwart': ('Stalwart', '+%s Max Health'),
    'plated': ('Plated', '+%s Armor'),
    'tempered': ('Tempered', '+%s Armor Toughness'),
    'fleet': ('Fleet', '+%s Speed'),
    'rooted': ('Rooted', '+%s Knockback Resistance'),
    'mirrored': ('Mirrored', 'Reflects %s of melee damage taken'),
    'warded': ('Warded', 'Gain %s absorption on a kill (every 10 s)'),
    'storied': ('Storied', '+%s Renown when you Remember'),
}

SIGNATURES = {
    'last_hour': ('Last Hour: take 25%% less damage below 30%% health', '%s of the Last Hour'),
    'fracture_step': ('Fracture Step: Speed II for 3 seconds after a kill', 'Fracturestep %s'),
    'choirs_echo': ("Choir's Echo: every fifth hit deals double damage", 'Choirsong %s'),
    'yesterglass': ('Yesterglass: kills add 5 seconds to the Hourglass', 'Yesterglass %s'),
}

RELIC_FLAVOR = [
    'Someone carried this further than you have.',
    'Its edge remembers every hand that held it.',
    'Recovered from a floor that no longer exists.',
    "The Archive filed it under 'unfinished'.",
    'Warm, as if it was only just put down.',
    'A margin note is scratched into the grip.',
    'It hums when the Hourglass runs low.',
    'Catalogued twice, by two different hands.',
    "The last archivist's mark, half worn away.",
    'It was never made. It was remembered.',
    'Bright on one side, ash on the other.',
    'Taken from the Archive. The Archive noticed.',
]

STRUCTURES = {
    'ancient_city': 'Ancient City', 'bastion_remnant': 'Bastion Remnant', 'buried_treasure': 'Buried Treasure',
    'desert_pyramid': 'Desert Pyramid', 'end_city': 'End City', 'fortress': 'Nether Fortress', 'igloo': 'Igloo',
    'jungle_pyramid': 'Jungle Temple', 'mansion': 'Woodland Mansion', 'mineshaft': 'Mineshaft',
    'mineshaft_mesa': 'Badlands Mineshaft', 'monument': 'Ocean Monument', 'nether_fossil': 'Nether Fossil',
    'ocean_ruin_cold': 'Cold Ocean Ruin', 'ocean_ruin_warm': 'Warm Ocean Ruin', 'pillager_outpost': 'Pillager Outpost',
    'ruined_portal': 'Ruined Portal', 'ruined_portal_desert': 'Ruined Portal', 'ruined_portal_jungle': 'Ruined Portal',
    'ruined_portal_mountain': 'Ruined Portal', 'ruined_portal_nether': 'Ruined Portal', 'ruined_portal_ocean': 'Ruined Portal',
    'ruined_portal_swamp': 'Ruined Portal', 'shipwreck': 'Shipwreck', 'shipwreck_beached': 'Beached Shipwreck',
    'stronghold': 'Stronghold', 'swamp_hut': 'Witch Hut', 'trail_ruins': 'Trail Ruins', 'trial_chambers': 'Trial Chambers',
    'village_desert': 'Desert Village', 'village_plains': 'Plains Village', 'village_savanna': 'Savanna Village',
    'village_snowy': 'Snowy Village', 'village_taiga': 'Taiga Village',
}

# id, parent, icon, frame, title, description
ADVANCEMENTS = [
    ('root', None, 'tbos:memory_key', 'task', 'Remnants: Roguelite Dungeons',
     'Attune a Memory Key on a Waystone, the Curator Gateway, or inside any structure'),
    ('remember', 'root', 'tbos:chronicle_shard', 'task', 'Remember', 'Bank a Recollection and carry it home'),
    ('forget', 'remember', 'minecraft:echo_shard', 'task', 'Forget',
     'Choose to Forget and descend with your Recollection at stake'),
    ('momentum', 'forget', 'minecraft:recovery_compass', 'goal', 'No Turning Back', 'Remember with Momentum ×2.00 or more'),
    ('recollection', 'root', 'tbos:yesterglass_lens', 'task', 'Echo of a Place', 'Open a Recollection inside a structure'),
    ('hourglass', 'remember', 'minecraft:clock', 'goal', 'Against the Sand',
     'Defeat a floor boss before the Hourglass runs out'),
    ('fractured', 'remember', 'minecraft:crying_obsidian', 'challenge', 'Fractured',
     'Remember an expedition at Fracture 15 or higher'),
    ('last_key', 'fractured', 'tbos:memory_key', 'challenge', 'The Last Key', 'Remember a tier 20 expedition'),
    ('remnant_relic', 'remember', 'minecraft:netherite_sword', 'challenge', 'Remnant', 'Recover a Remnant Relic'),
    ('weekly', 'remember', 'minecraft:compass', 'goal', "This Week's Memory", 'Remember a Weekly Recollection'),
    ('fellowship', 'remember', 'minecraft:bell', 'goal', 'Fellowship of the Archive',
     'Remember an expedition with a party of four'),
    ('storied', 'remember', 'minecraft:writable_book', 'goal', 'Storied', 'Reach Codex level 10'),
    ('archivist_eternal', 'storied', 'tbos:archivists_journal', 'challenge', 'Archivist Eternal',
     'Reach the highest Codex level'),
    ('know_the_archive', 'root', 'minecraft:skeleton_skull', 'challenge', 'Know the Archive',
     'Record every creature in the Bestiary'),
]

# event: (files, subtitle)
SOUNDS = {
    'expedition.attune': (['expedition/attune'], 'Memory Key resonates'),
    'expedition.distortion': (['expedition/distortion'], 'Distortion shifts'),
    'expedition.begin': (['expedition/begin'], 'Expedition begins'),
    'expedition.recollection': (['expedition/recollection'], 'A place remembers'),
    'expedition.recorded': (['expedition/recorded'], 'Renown recorded'),
    'expedition.key_rise': (['expedition/key_rise'], 'Key brightens'),
    'expedition.remember': (['expedition/remember'], 'Recollection banked'),
    'expedition.forget': (['expedition/forget'], 'Memory released'),
    'expedition.forgotten': (['expedition/forgotten'], 'Recollection forgotten'),
    'expedition.hourglass_tick': (['expedition/hourglass_tick'], 'Hourglass ticks'),
    'expedition.hourglass_end': (['expedition/hourglass_end'], 'Sand runs out'),
    'relic.reveal_common': (['relic/reveal_common'], 'Relic revealed'),
    'relic.reveal_rare': (['relic/reveal_rare'], 'Rare Relic revealed'),
    'relic.reveal_epic': (['relic/reveal_epic'], 'Epic Relic revealed'),
    'relic.reveal_legendary': (['relic/reveal_legendary'], 'Legendary Relic revealed'),
    'relic.reveal_remnant': (['relic/reveal_remnant'], 'Remnant Relic revealed'),
    'codex.open': (['codex/open'], 'Codex opens'),
    'codex.page': (['codex/page'], 'Page turns'),
    'codex.insight': (['codex/insight'], 'Insight learned'),
    'codex.renown': (['codex/renown'], 'Renown rises'),
    'party.invite': (['party/invite'], 'Party invitation'),
    'party.join': (['party/join'], 'Party member joins'),
    'distortion.bolster': (['distortion/bolster'], 'Enemies bolstered'),
    'distortion.volatile_hiss': (['distortion/volatile_hiss'], 'Corpse hisses'),
    'distortion.volatile_burst': (['distortion/volatile_burst'], 'Corpse bursts'),
    'distortion.haunt': (['distortion/haunt'], 'Shade appears'),
    'ui.click': (['ui/click'], None),
}


def language() -> dict:
    lang = {
        'itemGroup.tbos.yesterglass': 'Remnants: Roguelite Dungeons',
        'key.tbos.codex': 'Open Archive Codex',
        'item.tbos.memory_key': 'Memory Key',
        'item.tbos.memory_key.tiered_name': 'Memory Key · Tier %s',
        'item.tbos.memory_key.weekly_name': 'Weekly Memory Key · Tier %s',
        'item.tbos.memory_key.weekly': 'Weekly Recollection · Week %s',
        'item.tbos.memory_key.no_distortions': 'No Distortions',
        'item.tbos.memory_key.distortions': 'Distortions: %s',
        'item.tbos.memory_key.fracture': 'Heat %s · Fracture %s',
        'item.tbos.memory_key.use': 'Use on a Waystone, the Curator Gateway, or inside any structure',
        'item.tbos.memory_key.sealed': 'Distortions sealed. Use it like any Memory Key',

        'expedition.tbos.steady_hand': 'Steady Hand: the Archive returns a spent revive.',
        'expedition.tbos.boss_recorded': 'The floor boss falls. Renown and a sealed Relic join your Recollection.',
        'expedition.tbos.boss_recorded_hourglass': 'The floor boss falls before the sand runs out! Renown, a sealed Relic and +1 key tier join your Recollection.',
        'expedition.tbos.recorded': '+%s Renown recorded',
        'expedition.tbos.no_choice': 'There is nothing to Remember or Forget yet.',
        'expedition.tbos.choice_locked': "Defeat this floor's boss first.",
        'expedition.tbos.member_remembered': '%s Remembered and returned home.',
        'expedition.tbos.voted_forget': '%s chose to Forget (%s/%s).',
        'expedition.tbos.forgotten': 'Your Recollection is forgotten: %s Renown and %s sealed Relics are lost.',
        'expedition.tbos.key_dimmed': 'Your Memory Key dims to tier %s.',
        'expedition.tbos.reroll_unavailable': 'Second Draft is spent for this floor, or there is no draft to rewrite.',
        'expedition.tbos.rerolled': 'Second Draft: the offer is rewritten.',
        'expedition.tbos.key_inside_archive': 'The key has nothing to open in here.',
        'expedition.tbos.key_needs_place': 'The key needs a Waystone, the Curator Gateway, or the memory of a place: stand inside a structure.',
        'expedition.tbos.already_recollected': 'You have already recollected this %s.',
        'expedition.tbos.key_missing': 'Hold the Memory Key to begin.',
        'expedition.tbos.moved_away': 'You moved away from where the key was attuned.',
        'expedition.tbos.launch.already_in_run': 'You are already on an expedition.',
        'expedition.tbos.launch.unavailable': 'The Fractured Archive is unavailable on this server.',
        'expedition.tbos.launch.full': 'Every Archive instance is in use. Try again shortly.',
        'expedition.tbos.launch.failed': 'The Archive could not be built. See the server log.',
        'expedition.tbos.left_behind_busy': 'You are already on another expedition, so the party set out without you.',
        'expedition.tbos.choose_first': 'Remember or Forget before the gateway will open.',
        'expedition.tbos.place.archive': 'Fractured Archive',
        'codex.tbos.insight.last_stand.triggered': 'Last Stand! You hold on with a sliver of life.',
        'codex.tbos.respec_in_run': 'Finish your expedition before you respec.',
        'codex.tbos.weekly.claimed_already': "You already claimed this week's key.",
        'codex.tbos.pending_delivered': '%s rewards banked while you were away have been delivered.',

        'screen.tbos.cancel': 'Cancel',
        'screen.tbos.close': 'Close',
        'screen.tbos.attune.title': 'Attune the Memory Key',
        'screen.tbos.attune.distortions': 'Distortions',
        'screen.tbos.attune.active': 'Active · +1 Heat',
        'screen.tbos.attune.heat': '+1 Heat',
        'screen.tbos.attune.sealed': 'Sealed on a Weekly key',
        'screen.tbos.attune.toggle_hint': 'Click to toggle',
        'screen.tbos.attune.tier': 'Tier',
        'screen.tbos.attune.heat_total': 'Heat',
        'screen.tbos.attune.fracture': 'Fracture',
        'screen.tbos.attune.rewards': 'Rewards',
        'screen.tbos.attune.boss_renown': 'Boss Renown',
        'screen.tbos.attune.hourglass': 'Hourglass',
        'screen.tbos.attune.relic_odds': 'Relic odds',
        'screen.tbos.attune.party': 'Party:',
        'screen.tbos.attune.solo': 'just you',
        'screen.tbos.attune.begin': 'Begin Expedition',
        'screen.tbos.attune.place.weekly': 'Weekly Recollection · Week %s',
        'screen.tbos.attune.place.recollection': 'Recollection of the %s',
        'screen.tbos.attune.place.gateway': 'Archive Expedition · Curator Gateway',
        'screen.tbos.attune.place.waystone': 'Archive Expedition · Waystone',

        'screen.tbos.recollection.title': 'Recollection',
        'screen.tbos.recollection.choose': 'Remember or Forget',
        'screen.tbos.recollection.floor_done': 'Floor %s cleared in the %s',
        'screen.tbos.recollection.renown': '%s Renown',
        'screen.tbos.recollection.momentum': 'Momentum ×%s',
        'screen.tbos.recollection.sealed': '%s sealed Relics',
        'screen.tbos.recollection.next_key': 'Next key: Tier %s',
        'screen.tbos.recollection.no_key': 'No key earned yet',
        'screen.tbos.recollection.remember': 'Remember',
        'screen.tbos.recollection.remember.description': 'Bank everything you carry and return home. Your Codex keeps it forever.',
        'screen.tbos.recollection.forget': 'Forget',
        'screen.tbos.recollection.forget_voted': 'Forgetting…',
        'screen.tbos.recollection.forget.description': 'Descend a floor deeper. Momentum rises to ×%s, but if the party falls, it is all lost.',
        'screen.tbos.recollection.later': 'Decide later',
        'screen.tbos.recollection.remembered': 'Remembered',
        'screen.tbos.recollection.renown_banked': 'Renown banked · Momentum ×%s',
        'screen.tbos.recollection.level_up': 'Codex Level %s reached!',
        'screen.tbos.recollection.key_card': 'Tier %s Key',
        'screen.tbos.recollection.skip': 'Reveal all',
        'screen.tbos.recollection.forgotten': 'Forgotten',
        'screen.tbos.recollection.forgotten.lost': '%s Renown and %s sealed Relics slip back into the Archive.',
        'screen.tbos.recollection.forgotten.hint': "Your Codex keeps everything you already Remembered. The key's holder carries a dimmed key home.",

        'screen.tbos.codex.title': 'Archive Codex',
        'screen.tbos.codex.subtitle': 'Everything the Archive remembers of you',
        'screen.tbos.codex.loading': 'Reading the Codex…',
        'screen.tbos.codex.level': 'Codex Level %s / %s',
        'screen.tbos.codex.max': 'Fully storied',
        'screen.tbos.codex.tab.insights': 'Insights',
        'screen.tbos.codex.tab.bestiary': 'Bestiary',
        'screen.tbos.codex.tab.records': 'Records',
        'screen.tbos.codex.tab.weekly': 'Weekly',
        'screen.tbos.codex.unspent': 'Insight to spend: %s',
        'screen.tbos.codex.respec': 'Respec',
        'screen.tbos.codex.rank': 'Rank %s of %s',
        'screen.tbos.codex.learn_hint': 'Click to learn (1 Insight)',
        'screen.tbos.codex.bestiary.count': '%s of %s recorded',
        'screen.tbos.codex.bestiary.sealed': 'This page is sealed. Defeat the creature to record it.',
        'screen.tbos.codex.bestiary.role.campaign': 'Campaign boss',
        'screen.tbos.codex.bestiary.role.original': 'Archive creature',
        'screen.tbos.codex.bestiary.role.warden': 'Chamber bruiser',
        'screen.tbos.codex.bestiary.role.exclusive': 'Floor native',
        'screen.tbos.codex.bestiary.role.boss': 'Floor boss',
        'screen.tbos.codex.bestiary.everywhere': 'every floor',
        'screen.tbos.codex.bestiary.campaign': 'the Grand Orrery',
        'screen.tbos.codex.bestiary.where': '%s · %s',
        'screen.tbos.codex.bestiary.threat': 'Threat',
        'screen.tbos.codex.bestiary.kills': 'Slain: %s',
        'screen.tbos.codex.records.yours': 'Your records',
        'screen.tbos.codex.records.best_tier': 'Highest tier Remembered',
        'screen.tbos.codex.records.best_fracture': 'Highest Fracture',
        'screen.tbos.codex.records.deepest': 'Deepest expedition (floors)',
        'screen.tbos.codex.records.remembered': 'Expeditions Remembered',
        'screen.tbos.codex.records.forgotten': 'Expeditions Forgotten',
        'screen.tbos.codex.records.total_renown': 'Renown banked',
        'screen.tbos.codex.records.hourglass': 'Fastest Hourglass floor',
        'screen.tbos.codex.records.relics': 'Relics recovered',
        'screen.tbos.codex.records.hall': 'Hall of Records',
        'screen.tbos.codex.records.empty': 'No one has Remembered yet.',
        'screen.tbos.codex.records.floor_short': 'Floor %s',
        'screen.tbos.codex.records.tier_short': 'T%s · %s floors',
        'screen.tbos.codex.weekly.title': 'Week %s',
        'screen.tbos.codex.weekly.ends': 'Ends in %s',
        'screen.tbos.codex.weekly.tier': 'Tier %s',
        'screen.tbos.codex.weekly.best': 'Your deepest this week: %s floors',
        'screen.tbos.codex.weekly.claim': "Claim this week's key",
        'screen.tbos.codex.weekly.claimed': 'Key claimed',
        'screen.tbos.codex.weekly.board': "This week's deepest",
        'screen.tbos.codex.weekly.disabled': 'The Weekly Recollection is off on this server.',

        'hud.tbos.expedition.tier': 'Tier %s · Floor %s',
        'hud.tbos.expedition.weekly_tier': 'Weekly T%s · Floor %s',
        'hud.tbos.expedition.hourglass': 'Hourglass %s',
        'hud.tbos.expedition.hourglass_out': 'The sand has run out',
        'hud.tbos.expedition.choose': 'Remember or Forget [%s]',
        'hud.tbos.party.offline': 'Offline',
        'hud.tbos.codex.level_up': 'Codex Level %s',
        'hud.tbos.codex.level_up.sub': 'New Insight to spend · press %s',

        'party.tbos.already_in_party': 'You are already in a party. Use /party leave first.',
        'party.tbos.created': 'Party created. Invite friends with /party invite <player>.',
        'party.tbos.invite_self': 'You cannot invite yourself.',
        'party.tbos.not_leader': 'Only the party leader can do that.',
        'party.tbos.already_member': '%s is already in your party.',
        'party.tbos.full': 'The party is full (%s players).',
        'party.tbos.invited': '%s invites you to their party. Type /party accept within %s seconds.',
        'party.tbos.invite_sent': 'Invitation sent to %s.',
        'party.tbos.no_invite': 'You have no pending party invitations.',
        'party.tbos.joined': '%s joined the party.',
        'party.tbos.you_joined': 'You joined the party.',
        'party.tbos.declined': 'Invitations declined.',
        'party.tbos.not_in_party': 'You are not in a party.',
        'party.tbos.left': '%s left the party.',
        'party.tbos.you_left': 'You left the party.',
        'party.tbos.not_member': '%s is not in your party.',
        'party.tbos.kicked': 'You were removed from the party.',
        'party.tbos.removed': '%s was removed from the party.',
        'party.tbos.disbanded': 'The party has disbanded.',
        'party.tbos.list_header': 'Party (%s/%s):',
        'party.tbos.list_leader': '  ♛ %s (%s)',
        'party.tbos.list_member': '  • %s (%s)',
        'party.tbos.online': 'online',
        'party.tbos.offline': 'offline',
        'party.tbos.new_leader': '%s now leads the party.',

        'command.tbos.codex.renown': 'Granted %s Renown to %s.',
        'command.tbos.codex.reset': "Reset %s's Codex.",
        'command.tbos.expedition.none': 'You are not on an expedition.',
        'command.tbos.expedition.status': 'Tier %s · Heat %s · Floor %s · Momentum ×%s · Recollection: %s Renown, %s sealed Relics',
        'command.tbos.expedition.unknown_rarity': 'Unknown rarity. Use common, uncommon, rare, epic, legendary or remnant.',
        'command.tbos.expedition.unknown_distortion': 'Unknown Distortion: %s',

        'relic.tbos.header': '%s Relic · Item Level %s',
        'relic.tbos.name.plain': '%s %s',
        'relic.tbos.name.epic': '%s %s %s',
    }
    for key, (name, description) in DISTORTIONS.items():
        lang[f'expedition.tbos.distortion.{key}'] = name
        lang[f'expedition.tbos.distortion.{key}.description'] = description
    for key, (name, description) in INSIGHTS.items():
        lang[f'codex.tbos.insight.{key}'] = name
        lang[f'codex.tbos.insight.{key}.description'] = description
    for key, lore in BESTIARY.items():
        lang[f'codex.tbos.bestiary.{key}'] = lore
    for i, rarity in enumerate(['Common', 'Uncommon', 'Rare', 'Epic', 'Legendary', 'Remnant']):
        lang[f'relic.tbos.rarity.{rarity.lower()}'] = rarity
    for key, (prefix, line) in AFFIXES.items():
        lang[f'relic.tbos.prefix.{key}'] = prefix
        lang[f'relic.tbos.affix.{key}'] = line
    for key, (line, name) in SIGNATURES.items():
        lang[f'relic.tbos.signature.{key}'] = line
        lang[f'relic.tbos.name.signature.{key}'] = name
    for i, line in enumerate(RELIC_FLAVOR):
        lang[f'relic.tbos.flavor.{i}'] = line
    for key, noun in {'blade': 'Blade', 'cleaver': 'Cleaver', 'longbow': 'Longbow', 'arbalest': 'Arbalest',
                      'helm': 'Helm', 'cuirass': 'Cuirass', 'greaves': 'Greaves', 'sabatons': 'Sabatons'}.items():
        lang[f'relic.tbos.base.{key}'] = noun
    for key, name in {'wake': 'of the Wake', 'gallery': 'of the Gallery', 'meridian': 'of the Meridian',
                      'choir': 'of the Choir', 'vault': 'of the Vault', 'catalogue': 'of the Catalogue',
                      'labyrinth': 'of the Labyrinth', 'unwritten': 'of the Unwritten Hour'}.items():
        lang[f'relic.tbos.suffix.{key}'] = name
    for key, name in STRUCTURES.items():
        lang[f'recollection.tbos.structure.minecraft.{key}'] = name
    for adv_id, _, _, _, title, description in ADVANCEMENTS:
        lang[f'advancements.tbos.expedition.{adv_id}.title'] = title
        lang[f'advancements.tbos.expedition.{adv_id}.description'] = description
    for event, (_, subtitle) in SOUNDS.items():
        if subtitle:
            lang[f'subtitles.tbos.{event}'] = subtitle
    return lang


def merge_language(lang: dict):
    path = SHARED / 'assets' / NS / 'lang' / 'en_us.json'
    existing = json.loads(path.read_text(encoding='utf-8'))
    existing.update(lang)
    path.write_text(json.dumps(existing, indent=4, ensure_ascii=False) + '\n', encoding='utf-8')


# ---- Data -----------------------------------------------------------------------------------------------

def advancement(adv_id, parent, icon, frame, icon_key='id', background='minecraft:gui/advancements/backgrounds/stone'):
    display = {
        'icon': {icon_key: icon},
        'title': {'translate': f'advancements.tbos.expedition.{adv_id}.title'},
        'description': {'translate': f'advancements.tbos.expedition.{adv_id}.description'},
        'frame': frame,
        'show_toast': parent is not None,
        'announce_to_chat': parent is not None,
        'hidden': False,
    }
    if parent is None:
        display['background'] = background
    data = {'criteria': {'done': {'trigger': 'minecraft:impossible'}}, 'display': display, 'requirements': [['done']]}
    if parent:
        data['parent'] = f'{NS}:expedition/{parent}'
    else:
        data['sends_telemetry_event'] = False
    return data


def recipe(style):
    def ingredient(item):
        return item if style == 'string' else {'item': item}
    return {
        'type': 'minecraft:crafting_shaped',
        'category': 'misc',
        'key': {'A': ingredient('minecraft:amethyst_shard'), 'G': ingredient('minecraft:gold_ingot'),
                'E': ingredient('minecraft:ender_pearl')},
        'pattern': ['AGA', 'GEG', 'AGA'],
        'result': {('item' if style == 'legacy' else 'id'): f'{NS}:memory_key', 'count': 1},
    }


ARCHIVE_CREATURES = [f'{NS}:{key}' for key in BESTIARY]


def render_sounds(sounds: dict) -> str:
    """The file's own layout: one line per sound file, two-space indents."""
    def inline(item):
        return '{ ' + ', '.join(f'"{key}": {json.dumps(value, ensure_ascii=False)}' for key, value in item.items()) + ' }'
    blocks = []
    for event, entry in sounds.items():
        lines = [f'  "{event}": {{', '    "sounds": [']
        files = entry.get('sounds', [])
        for i, item in enumerate(files):
            lines.append('      ' + (inline(item) if isinstance(item, dict) else json.dumps(item)) + (',' if i < len(files) - 1 else ''))
        lines.append('    ]' + (',' if len(entry) > 1 else ''))
        rest = [key for key in entry if key != 'sounds']
        for i, key in enumerate(rest):
            lines.append(f'    "{key}": {json.dumps(entry[key], ensure_ascii=False)}' + (',' if i < len(rest) - 1 else ''))
        lines.append('  }')
        blocks.append('\n'.join(lines))
    return '{\n' + ',\n'.join(blocks) + '\n}\n'


def sounds_json():
    path = SHARED / 'assets' / NS / 'sounds.json'
    existing = json.loads(path.read_text(encoding='utf-8'))
    for event, (files, subtitle) in SOUNDS.items():
        entry = {'sounds': [{'name': f'{NS}:rpg/{f}', 'stream': False} for f in files]}
        if subtitle:
            entry['subtitle'] = f'subtitles.tbos.{event}'
        existing[event] = entry
    path.write_text(render_sounds(existing), encoding='utf-8')


def main():
    merge_language(language())
    sounds_json()
    data = SHARED / 'data' / NS
    for adv_id, parent, icon, frame, _, _ in ADVANCEMENTS:
        write(data / 'advancement' / 'expedition' / f'{adv_id}.json', advancement(adv_id, parent, icon, frame))
        # 1.21.1 reads the background as a texture path; 1.20.1 also spells the icon "item".
        write(OVERRIDE_1211 / 'data' / NS / 'advancement' / 'expedition' / f'{adv_id}.json',
              advancement(adv_id, parent, icon, frame, background='minecraft:textures/gui/advancements/backgrounds/stone.png'))
        write(OVERRIDE_1201 / 'data' / NS / 'advancement' / 'expedition' / f'{adv_id}.json',
              advancement(adv_id, parent, icon, frame, icon_key='item',
                          background='minecraft:textures/gui/advancements/backgrounds/stone.png'))
    write(data / 'recipe' / 'memory_key.json', recipe('string'))
    write(OVERRIDE_1211 / 'data' / NS / 'recipe' / 'memory_key.json', recipe('object'))
    write(OVERRIDE_1201 / 'data' / NS / 'recipe' / 'memory_key.json', recipe('legacy'))
    write(data / 'tags' / 'worldgen' / 'structure' / 'unrecollectable.json', {'replace': False, 'values': []})
    write(SHARED / 'data' / 'remnant_mobs' / 'tags' / 'entity_type' / 'never_leveled.json',
          {'replace': False, 'values': [{'id': creature, 'required': False} for creature in ARCHIVE_CREATURES]})
    assets = SHARED / 'assets' / NS
    write(assets / 'items' / 'memory_key.json', {'model': {'type': 'minecraft:model', 'model': f'{NS}:item/memory_key'}})
    write(assets / 'models' / 'item' / 'memory_key.json',
          {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'{NS}:item/memory_key'}})
    print('1.0 data written')


if __name__ == '__main__':
    main()
