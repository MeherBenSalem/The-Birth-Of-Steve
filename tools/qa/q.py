#!/usr/bin/env python3
"""One-shot control of a running QA server and client, for interactive checks.

  python3 tools/qa/q.py cmd "time set noon" "weather clear"   # server console over RCON
  python3 tools/qa/q.py key e                                 # a key press in the game window
  python3 tools/qa/q.py click 3 [x y]                         # a mouse click (1 left, 3 right)
  python3 tools/qa/q.py move x y                              # move the pointer
  python3 tools/qa/q.py chat "/codex"                         # a chat line or command as the player
  python3 tools/qa/q.py hud on|off                            # F1
  python3 tools/qa/q.py shot out.png [x y w h]                # capture the display
"""
from __future__ import annotations

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from client import Session  # noqa: E402


def main(argv):
    if not argv:
        print(__doc__)
        return 1
    verb, args = argv[0], argv[1:]
    s = Session()
    if verb == 'cmd':
        for command in args:
            print(s.cmd(command))
    elif verb == 'key':
        s.key(*args)
    elif verb == 'click':
        button = int(args[0]) if args else 1
        if len(args) >= 3:
            s.click(button, int(args[1]), int(args[2]))
        else:
            s.click(button)
    elif verb == 'move':
        s.move(int(args[0]), int(args[1]))
    elif verb == 'chat':
        s.chat(' '.join(args))
    elif verb == 'hud':
        s.key('F1')
    elif verb == 'shot':
        crop = None
        if len(args) == 5:
            x, y, w, h = map(int, args[1:])
            crop = (x, y, x + w, y + h)
        print(s.shot(args[0], crop))
    else:
        print(__doc__)
        return 1
    return 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
