#!/usr/bin/env bash
# Genere tous les packs JSON "premium" pour Kikongo (kg) et Lingala (ln).
# Sortie: target/packs/<PREMIUM_LABEL>/ (defaut PREMIUM_LABEL=CYCLE_ID-semaine-premium).
# Cadence : uniquement weekly (LONGOKA_CADENCE=biweekly est normalisé côté Java vers weekly).
# Fichiers prefixes kg- / ln-.
#
# Usage:
#   cd backend-java && bash scripts/run-export-all-premium.sh
# Variables optionnelles:
#   CYCLE_ID=20260406  MEANING_LANG=fr  COUNT=24
#   ARROWWORD_PROFILE_SET=full|base  (defaut full — jeux de profils complets comme wordsearch ; base pour CI rapide)
#
# Connexion DB: variables d'environnement (voir DbConfig.java), ex. LEX_KG_DB_HOST, LEX_KG_DB_USER...

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$BACKEND_ROOT"

CYCLE_ID="${CYCLE_ID:-$(date +%Y%m%d)}"
MEANING_LANG="${MEANING_LANG:-fr}"
COUNT="${COUNT:-24}"
# Langues a exporter (espace-separe), ex: LANGUAGES=kg ou LANGUAGES="kg ln"
LANGUAGES="${LANGUAGES:-kg ln}"
WORDSEARCH_ROWS="${WORDSEARCH_ROWS:-20}"
WORDSEARCH_COLS="${WORDSEARCH_COLS:-20}"
CROSSWORD_ROWS="${CROSSWORD_ROWS:-19}"
CROSSWORD_COLS="${CROSSWORD_COLS:-19}"
ARROWWORD_ROWS="${ARROWWORD_ROWS:-17}"
ARROWWORD_COLS="${ARROWWORD_COLS:-17}"
GRID_ENTRIES="${GRID_ENTRIES:-16}"
MORPHO_ENTRIES="${MORPHO_ENTRIES:-12}"
PREMIUM_LABEL="${PREMIUM_LABEL:-${CYCLE_ID}-semaine-premium}"
ARROWWORD_PROFILE_SET="${ARROWWORD_PROFILE_SET:-full}"

# Toujours compiler avant exec:java (sinon ClassNotFoundException sur CI sans target/classes).
# En CI : pas de -q pour voir les logs ; -e pour la stack trace Maven si échec.
if [ "${CI:-}" = "true" ]; then
  MVN=(mvn -e -DskipTests compile exec:java -Dexec.mainClass=com.longoka.games.app.BiweeklyPuzzleBatchTool)
else
  MVN=(mvn -q -DskipTests compile exec:java -Dexec.mainClass=com.longoka.games.app.BiweeklyPuzzleBatchTool)
fi

run_batch() {
  local lang="$1"
  local type="$2"
  local label="$3"
  local extra="${4:-}"
  local cadence="${LONGOKA_CADENCE:-weekly}"
  local args="--cadence $cadence --count $COUNT --meaningLang $MEANING_LANG --language $lang --type $type --label $label --tier premium --difficulty expert $extra"
  echo ">>> [$lang] $type -> $label"
  "${MVN[@]}" -Dexec.args="$args"
}

arrowword_extra="--rows $ARROWWORD_ROWS --cols $ARROWWORD_COLS --maxEntries $GRID_ENTRIES"
if [ "$ARROWWORD_PROFILE_SET" = "base" ]; then
  arrowword_extra="$arrowword_extra --profileSet base"
fi

for LANG in $LANGUAGES; do
  run_batch "$LANG" wordsearch "$PREMIUM_LABEL" "--rows $WORDSEARCH_ROWS --cols $WORDSEARCH_COLS --maxEntries $GRID_ENTRIES"
  run_batch "$LANG" crossword "$PREMIUM_LABEL" "--rows $CROSSWORD_ROWS --cols $CROSSWORD_COLS --maxEntries $GRID_ENTRIES"
  run_batch "$LANG" arrowword "$PREMIUM_LABEL" "$arrowword_extra"
  run_batch "$LANG" domino "$PREMIUM_LABEL" "--maxEntries $MORPHO_ENTRIES"
  run_batch "$LANG" memory "$PREMIUM_LABEL" "--maxEntries $MORPHO_ENTRIES"
  run_batch "$LANG" scrabble "$PREMIUM_LABEL" "--maxEntries $MORPHO_ENTRIES"
  run_batch "$LANG" anagram "$PREMIUM_LABEL" "--maxEntries $MORPHO_ENTRIES"
done

echo "Termine. Sortie sous: $BACKEND_ROOT/target/packs/$PREMIUM_LABEL (LONGOKA_CADENCE=${LONGOKA_CADENCE:-weekly})"
