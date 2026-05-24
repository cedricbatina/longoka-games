#!/usr/bin/env bash
# Export hebdomadaire pour l'app Longoka (longoka.com/games, cours/*/games).
# - Profils : weekly-safe (fenêtre 150 j → nouns-singular + mixed-verbs-nouns-singular, kikongo kg + lingala ln)
# - Types : BOTH côté Java (mots croisés désactivés dans BiweeklyPuzzleBatchTool)
# - Un pack par type et par langue (weeklyRandom)
#
# Usage (FR puis EN, comme la CI) :
#   CYCLE_ID=20260520 MEANING_LANG=fr LANGUAGES="kg" bash scripts/run-export-app-weekly.sh
#   CYCLE_ID=20260520 MEANING_LANG=en LANGUAGES="kg" bash scripts/run-export-app-weekly.sh
#
# Lingala : LANGUAGES="kg ln" si LEX_LN_DB_* configuré.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$BACKEND_ROOT"

CYCLE_ID="${CYCLE_ID:-$(date +%Y%m%d)}"
MEANING_LANG="${MEANING_LANG:-fr}"
LANGUAGES="${LANGUAGES:-kg}"
COUNT="${COUNT:-48}"
ROWS="${ROWS:-20}"
COLS="${COLS:-20}"
MAX_ENTRIES="${MAX_ENTRIES:-16}"
TIER="${TIER:-mulongoki}"
DIFFICULTY="${DIFFICULTY:-easy}"
JAVA_TYPE="${JAVA_TYPE:-both}"

_meaning_suffix="$(printf '%s' "$MEANING_LANG" | tr '[:upper:]' '[:lower:]')"
case "$_meaning_suffix" in
  en) ;;
  fr) _meaning_suffix=fr ;;
  *) _meaning_suffix=fr ;;
esac

if [ -n "${LONGOKA_APP_LABEL:-}" ]; then
  APP_LABEL="${LONGOKA_APP_LABEL}"
else
  APP_LABEL="${CYCLE_ID}-semaine-app-${_meaning_suffix}"
fi

_ensure_unique_label() {
  local base="$1"
  local label="$base"
  local n=2
  while [ -d "$BACKEND_ROOT/target/packs/$label" ] && [ "${LONGOKA_EXPORT_OVERWRITE:-}" != "1" ]; do
    label="${base}-run${n}"
    n=$((n + 1))
  done
  printf '%s' "$label"
}

APP_LABEL="$(_ensure_unique_label "$APP_LABEL")"
export APP_LABEL
echo "APP_LABEL=$APP_LABEL (meaningLang=${MEANING_LANG}, languages=${LANGUAGES}, type=${JAVA_TYPE}, overwrite=${LONGOKA_EXPORT_OVERWRITE:-0})"

if [ "${CI:-}" = "true" ]; then
  MVN=(mvn -e -DskipTests compile exec:java -Dexec.mainClass=com.longoka.games.app.BiweeklyPuzzleBatchTool)
else
  MVN=(mvn -q -DskipTests compile exec:java -Dexec.mainClass=com.longoka.games.app.BiweeklyPuzzleBatchTool)
fi

exec_args="--cadence weekly --count ${COUNT} --rows ${ROWS} --cols ${COLS} --maxEntries ${MAX_ENTRIES} --meaningLang ${MEANING_LANG} --language ${LANGUAGES} --type ${JAVA_TYPE} --label ${APP_LABEL} --tier ${TIER} --difficulty ${DIFFICULTY} --profileSet weekly-safe --weeklyRandom true"

echo ">>> app weekly export: ${exec_args}"
"${MVN[@]}" -Dexec.args="${exec_args}"

echo "Done. Output: ${BACKEND_ROOT}/target/packs/${APP_LABEL}"
