#!/usr/bin/env python3
"""Captures the listing screenshots in marketing/images/screenshots/ from a live 26.1.2 Fabric dev server and client.

Start them first (see marketing/README.md):
  tools/qa/prepare_runs.sh 26.1.2 fabric <options.txt>     # GUI scale 3, menu blur 0, no tutorial
  Xvfb :1 -screen 0 1920x1080x24 &
  QA_LOGS=/tmp/tbos-shots tools/qa/devrun.sh start-server 26.1.2 fabric
  QA_LOGS=/tmp/tbos-shots tools/qa/devrun.sh start-client 26.1.2 fabric
then run the steps in order (each name is one step; no names runs them all):
  python3 marketing/source/capture.py [step...]
    setup insights attunement begin clear-rooms boss memory-draft defeat remember weekly codex relics recollection

The world is "archive_showcase" with seed `remnants-archive` (server.properties). Start from a fresh Codex: with the
server stopped, delete the world's data/tbos/codex/ and data/tbos/records.json, so the Records and the Hall of Records
show only the two Expeditions these steps play (a tier 7 key, then this week's Weekly key).

Everything shown is the mod running normally: keys come from /expedition key and /codex weekly, each run is a real
Expedition begun from the Attunement screen, its rooms are cleared by killing their creatures as the player and then
/tbos dungeon force_clear (so they credit Renown and Bestiary kills exactly as a fight would), and the Relics revealed
are the ones that run banked. Creatures are held still with NoAI for the posed shots. The Archive's rooms are lit only
by their own lamps, so the in-Archive shots use Night Vision; if the floor boss's hall rolls the Darkness room modifier,
which overrides it, play another run for the boss step. Captures are 1920x1080 frames of the X display, saved as JPEG.

Click positions are GUI coordinates at GUI scale 3 (a 640x360 GUI on the 1920x1080 display).
"""
from __future__ import annotations

import json
import sys
import tempfile
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'tools' / 'qa'))
from client import PLAYER, Session  # noqa: E402

OUT = ROOT / 'marketing' / 'images' / 'screenshots'
BOSS_FILE = Path(tempfile.gettempdir()) / 'tbos-capture-boss'  # the floor boss the boss step met, for the Bestiary shot
ARCHIVE = 'tbos:fractured_archive'
SCALE = 3
CODEX_TOP = (360 - 256) // 2  # CodexScreen's panel top in the 640x360 GUI
s: Session


def c(*commands):
    return s.cmd(*commands)


def me(command):
    """A command run as the player, where the mod needs a player source."""
    return s.cmd(f'execute as {PLAYER} at @s run {command}')


def gui(x, y):
    return round(x * SCALE), round(y * SCALE)


def click(x, y, button=1):
    s.click(button, *gui(x, y))


def save(name, hud=True):
    """Captures the screen as a JPEG in screenshots/, with the pointer parked out of frame."""
    s.move(1919, 1079)
    time.sleep(1.0)  # the software renderer needs a few frames to drop a tooltip
    if not hud:
        s.hud(False)
        time.sleep(1.0)
    png = OUT / f'.{name}.png'
    s.shot(png)
    if not hud:
        s.hud(True)
    from PIL import Image
    Image.open(png).convert('RGB').save(OUT / f'{name}.jpg', quality=90, optimize=True)
    png.unlink()
    print('captured', name)


# ---- The run's geometry -------------------------------------------------------------------------------------

def graph():
    """Room centres of the player's current floor, from /tbos dungeon export_graph (instance slot 0)."""
    out = me('tbos dungeon export_graph')
    path = Path(out.split(' to ', 1)[1].strip())
    data = json.loads(path.read_text())
    rooms = {}
    for room in data['rooms']:
        grid = room['placement']['coordinates']
        rooms[room['index']] = (room['category'], -12288 + grid['x'] * 34, 96 + grid['y'] * 16, -12288 + grid['z'] * 34)
    return rooms


def into(x, y, z, yaw=0.0, pitch=0.0):
    return c(f'execute in {ARCHIVE} run tp {PLAYER} {x} {y} {z} {yaw} {pitch}')


def bestiary(bosses=False):
    """Bestiary entity ids in page order, or only the floor bosses'."""
    source = ROOT / 'shared/common/src/main/java/com/nightbeam/tbos/codex/Bestiary.java'
    return [line.split('"')[1] for line in source.read_text().splitlines()
            if 'new Entry("' in line and (not bosses or 'Role.BOSS' in line)]


def slay(radius, bosses=False):
    """Kills the Archive creatures (or only floor bosses) around the player as the player, so the Codex credits them."""
    c(*(f'execute at {PLAYER} as @e[type={entity},distance=..{radius}] run damage @s 9999 minecraft:player_attack '
        f'by {PLAYER}' for entity in bestiary(bosses)))


SHOTS = {}


def step(fn):
    SHOTS[fn.__name__.replace('_', '-')] = fn
    return fn


# ---- Steps ------------------------------------------------------------------------------------------------

@step
def setup():
    """Daylight, no weather, natural spawns or command chatter, a fresh Codex with Renown to spend, and two keys."""
    c('gamerule advance_time false', 'gamerule advance_weather false', 'gamerule spawn_mobs false',
      'gamerule send_command_feedback false', 'time set 6000',
      'weather clear', f'gamemode survival {PLAYER}', f'effect clear {PLAYER}', f'clear {PLAYER}')
    c(f'codex reset {PLAYER}', f'codex renown {PLAYER} 2600')
    me('expedition key 7 fortified volatile hourglass')
    me('expedition key 8 tyrannical hourglass')


@step
def insights():
    """Spends Insight through the Codex: Second Draft, Deep Recall twice, Keen Eye, Archivist's Grace, Last Stand."""
    s.key('n')
    time.sleep(2.5)
    for index in (10, 1, 1, 2, 3, 15):
        click(189.5 + 87 * (index % 4), CODEX_TOP + 92 + 44 * (index // 4))
        time.sleep(1.2)
    save('codex-insights')
    s.key('Escape')


@step
def attunement():
    """A Waystone in the plains by spawn, and the tier 7 key with Fortified, Volatile and Hourglass."""
    c('setblock 5 108 9 tbos:waystone')
    s.tp(5.5, 108, 5.5, facing=(5.5, 108.6, 9.5))
    s.key('1')
    time.sleep(1.5)
    s.click(3)
    time.sleep(3)
    save('attunement')


@step
def begin():
    click(404, 274)
    time.sleep(12)
    c(f'effect give {PLAYER} night_vision infinite 0 true', f'effect give {PLAYER} resistance infinite 4 true',
      f'effect give {PLAYER} regeneration infinite 2 true')


@step
def clear_rooms():
    """Clears every room except the boss and the reward room, crediting Renown room by room and its kills to the Codex."""
    for index, (category, x, y, z) in graph().items():
        if category in ('starting', 'final_boss', 'exit_reward', 'waystone'):
            continue
        into(x + 0.5, y + 1, z + 0.5)
        time.sleep(3)
        slay(24)
        time.sleep(1)
        print(index, category, me('tbos dungeon force_clear'))
        time.sleep(1.5)
    print(me('expedition status'))


@step
def boss():
    """The floor boss, posed in the middle of its hall, and the Expedition HUD over the fight."""
    rooms = graph()
    _, x, y, z = next(v for v in rooms.values() if v[0] == 'final_boss')
    into(x + 0.5, y + 1, z + 7.5, 180, 0)
    time.sleep(8)
    boss_sel = f'@e[type=!minecraft:player,type=!minecraft:item,distance=..40,limit=1,sort=nearest]'
    me(f'data merge entity {boss_sel} {{NoAI:1b}}')
    BOSS_FILE.write_text(next(entity for entity in bestiary(bosses=True)
                              if 'passed' in me(f'execute if entity @e[type={entity},distance=..40]')))
    me(f'tp {boss_sel} {x + 0.5} {y + 1} {z - 0.5} 0 0')
    s.tp(x + 0.5, y + 1, z + 6.5, facing=(x + 0.5, y + 2.6, z - 0.5))
    time.sleep(3)
    save('expedition-hud')
    c(f'gamemode spectator {PLAYER}')
    s.tp(x + 1.8, y + 0.9, z + 3.6, facing=(x + 0.5, y + 2.9, z - 0.5))
    time.sleep(3)
    save('archive-boss', hud=False)
    c(f'gamemode survival {PLAYER}')
    s.tp(x + 0.5, y + 1, z + 6.5, 180, 0)
    time.sleep(1)


@step
def memory_draft():
    """The Living Memories draft on the Memory screen, with Second Draft's Rewrite button."""
    s.key('k')
    time.sleep(2.5)
    click(452, 116)  # the Draft page
    time.sleep(2)
    save('memory-draft')
    s.key('Escape')


@step
def defeat(capture=True):
    """The floor boss falls to the player, and the reward room's Remember or Forget choice opens."""
    rooms = graph()
    _, x, y, z = next(v for v in rooms.values() if v[0] == 'final_boss')
    into(x + 0.5, y + 1, z + 6.5, 180, 0)
    time.sleep(6)
    slay(40, bosses=True)
    time.sleep(4)
    _, x, y, z = next(v for v in rooms.values() if v[0] == 'exit_reward')
    into(x + 0.5, y + 1, z + 0.5, 90, 0)
    time.sleep(4)
    me('tbos dungeon force_clear')
    time.sleep(5)
    if capture:
        save('choice')


@step
def remember(capture=True):
    click(246, 216)
    time.sleep(14)
    if capture:
        save('banked')
    s.key('Escape')


@step
def weekly():
    """A second Expedition on this week's sealed key, so the Weekly board and the Hall of Records have a run on it."""
    c(f'clear {PLAYER}', f'effect clear {PLAYER}')
    me('codex weekly')  # claims this week's key, as the Weekly tab's button does
    s.tp(5.5, 108, 5.5, facing=(5.5, 108.6, 9.5))
    s.key('1')
    time.sleep(1.5)
    s.click(3)
    time.sleep(3)
    begin()
    clear_rooms()
    defeat(capture=False)
    remember(capture=False)
    c(f'effect clear {PLAYER}')


@step
def codex():
    s.key('n')
    time.sleep(2.5)
    click(276, CODEX_TOP + 43)  # Bestiary
    time.sleep(1.5)
    # Select the floor boss the run just defeated (the boss step noted which one it met).
    order = bestiary()
    if BOSS_FILE.exists() and BOSS_FILE.read_text() in order:
        click(208, CODEX_TOP + 66.5 + 12 * order.index(BOSS_FILE.read_text()))
        time.sleep(1.2)
    save('codex-bestiary')
    click(363, CODEX_TOP + 43)  # Records
    time.sleep(1.5)
    save('codex-records')
    click(450, CODEX_TOP + 43)  # Weekly
    time.sleep(1.5)
    save('codex-weekly')
    s.key('Escape')


@step
def relics():
    """Relic tooltips in the survival inventory: a Legendary and a Remnant, beside an Epic, a Rare and two keys."""
    c(f'gamemode survival {PLAYER}', f'clear {PLAYER}')
    for command in ('expedition relic 12 legendary', 'expedition relic 14 remnant', 'expedition relic 9 epic',
                    'expedition relic 6 rare', 'expedition key 9 bolstering haunted', 'expedition weekly_key'):
        me(command)
    time.sleep(1)
    s.key('e')
    time.sleep(2)
    s.move(*gui(248, 247))
    time.sleep(1.5)
    s.shot(OUT / '.relic.png')
    from PIL import Image
    Image.open(OUT / '.relic.png').convert('RGB').save(OUT / 'relic-legendary.jpg', quality=90, optimize=True)
    (OUT / '.relic.png').unlink()
    s.move(*gui(266, 247))
    time.sleep(1.5)
    s.shot(OUT / '.relic.png')
    Image.open(OUT / '.relic.png').convert('RGB').save(OUT / 'relic-remnant.jpg', quality=90, optimize=True)
    (OUT / '.relic.png').unlink()
    print('captured relic-legendary, relic-remnant')
    s.key('Escape')


@step
def recollection():
    """A Memory Key used at the well of the nearest plains village.

    The key needs the player's feet inside one of the structure's pieces and a block within reach, so the player
    stands on the north edge of the well's piece and looks down at the well."""
    c(f'gamemode creative {PLAYER}')
    c(f'execute in minecraft:overworld run tp {PLAYER} -250.5 66.5 192.6 0 42')
    time.sleep(12)
    s.key('5')
    time.sleep(0.5)
    s.click(3)
    time.sleep(3)
    save('recollection')
    s.key('Escape')


def main(wanted):
    global s
    s = Session()
    OUT.mkdir(parents=True, exist_ok=True)
    for name, fn in SHOTS.items():
        if wanted and name not in wanted:
            continue
        print('==', name)
        fn()


if __name__ == '__main__':
    main(sys.argv[1:])
