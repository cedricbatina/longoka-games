#!/usr/bin/env bash
# Version hebdomadaire (weekly) du cycle standard.
#
# Usage: cd backend-java && bash scripts/run-export-weekly-standard.sh
#
# Variables optionnelles:
#   CYCLE_ID=20260406  MEANING_LANG=fr  COUNT=24  ROWS=12  COLS=12  MAX_ENTRIES=10

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

MVN=(mvn -q -DskipTests exec:java -Dexec.mainClass=com.longoka.games.app.BiweeklyPuzzleBatchTool)

run() {
  local type="$1" label="$2"
  local args="--cadence weekly --count $COUNT --rows $ROWS --cols $COLS --maxEntries $MAX_ENTRIES --meaningLang $MEANING_LANG --type $type --label $label"
  echo ">>> $type -> $label"
  "${MVN[@]}" -Dexec.args="$args"
}

# Hebdo: on sort tout sur la même semaine (dossier unique).
run wordsearch "$W1"
run crossword "$W1"

echo "Termine: target/weekly/$W1"

