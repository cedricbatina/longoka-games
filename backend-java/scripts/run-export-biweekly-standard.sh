#!/usr/bin/env bash
# Meme logique que run-bihebdo-cycle.ps1 (sans -IncludeMorphoPacks):
#   semaine 1 : word search, semaine 2 : mots croises — langues kg + ln dans chaque dossier.
#
# Usage: cd backend-java && bash scripts/run-export-biweekly-standard.sh

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$BACKEND_ROOT"

CYCLE_ID="${CYCLE_ID:-$(date +%Y%m%d)}"
MEANING_LANG="${MEANING_LANG:-fr}"
COUNT="${COUNT:-24}"
ROWS="${ROWS:-12}"
COLS="${COLS:-12}"
MAX_ENTRIES="${MAX_ENTRIES:-10}"

W1="${CYCLE_ID}-semaine-1"
W2="${CYCLE_ID}-semaine-2"

MVN=(mvn -q -DskipTests exec:java -Dexec.mainClass=com.longoka.games.app.BiweeklyPuzzleBatchTool)

run() {
  local type="$1" label="$2"
  local args="--count $COUNT --rows $ROWS --cols $COLS --maxEntries $MAX_ENTRIES --meaningLang $MEANING_LANG --type $type --label $label"
  echo ">>> $type -> $label"
  "${MVN[@]}" -Dexec.args="$args"
}

run wordsearch "$W1"
run crossword "$W2"

echo "Termine: target/biweekly/$W1 et target/biweekly/$W2"
