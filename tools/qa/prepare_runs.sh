#!/usr/bin/env bash
# Prepares a workspace's dev run directories for QA and the listing captures:
# server.properties (RCON on 25575), eula, the QA player as operator, and client
# options that skip first-run screens.
#   tools/qa/prepare_runs.sh <version> <loader> [options.txt to copy]
set -eu
ROOT=$(cd "$(dirname "$0")/../.." && pwd)
SERVER="$ROOT/$1/$2/runs/server"
CLIENT="$ROOT/$1/$2/runs/client"
mkdir -p "$SERVER" "$CLIENT"
cat > "$SERVER/server.properties" <<'PROPS'
allow-flight=true
broadcast-rcon-to-ops=false
difficulty=normal
enable-rcon=true
enforce-whitelist=false
force-gamemode=false
function-permission-level=2
gamemode=survival
level-name=archive_showcase
level-seed=remnants-archive
max-tick-time=120000
motd=Remnants QA
online-mode=false
op-permission-level=4
rcon.password=qa
rcon.port=25575
simulation-distance=6
spawn-protection=0
view-distance=8
white-list=false
PROPS
echo "eula=true" > "$SERVER/eula.txt"
# UUID of the offline-mode player "Archivist".
cat > "$SERVER/ops.json" <<'OPS'
[{"uuid": "d2d897a6-3fa0-3182-9b87-dfaf07475aff", "name": "Archivist", "level": 4, "bypassesPlayerLimit": false}]
OPS
if [ "$2" = forge ]; then
  mkdir -p "$SERVER/defaultconfigs"
  printf '[server]\n\tadvertiseDedicatedServerToLan = false\n' > "$SERVER/defaultconfigs/forge-server.toml"
fi
if [ -n "${3:-}" ] && [ -f "$3" ]; then cp "$3" "$CLIENT/options.txt"; fi
