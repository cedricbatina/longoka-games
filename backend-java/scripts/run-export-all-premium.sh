#!/usr/bin/env bash
# Genere les packs JSON "premium" du cycle hebdomadaire.
# Sortie: target/packs/<PREMIUM_LABEL>/ (defaut PREMIUM_LABEL=CYCLE_ID-semaine-premium).
# Cadence : uniquement weekly (LONGOKA_CADENCE=biweekly est normalisé côté Java vers weekly).
# Fichiers prefixes kg- / ln-.
#
# Usage:
#   cd backend-java && bash scripts/run-export-all-premium.sh
# Variables optionnelles:
#   CYCLE_ID=20260406  MEANING_LANG=fr  COUNT=48
# Pour FR+EN : deux passes (MEANING_LANG=fr puis en) → dossiers …-premium-fr et …-premium-en.
# Sync Longoka : scripts/sync-games-packs.mjs (incrémental, ne supprime pas les anciens packs déjà commités).
#   TYPES_PER_CYCLE=2  PROFILES_PER_TYPE=2
#   TYPE_POOL="wordsearch crossword memory domino"  LANGUAGES=kg ou LANGUAGES="kg ln"
#   ARROWWORD_PROFILE_SET=full|base  (defaut full — jeux de profils complets comme wordsearch ; base pour CI rapide)
#
# Connexion DB: variables d'environnement (voir DbConfig.java), ex. LEX_KG_DB_HOST, LEX_KG_DB_USER...

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$BACKEND_ROOT"

CYCLE_ID="${CYCLE_ID:-$(date +%Y%m%d)}"
MEANING_LANG="${MEANING_LANG:-fr}"
COUNT="${COUNT:-48}"
# Langues a exporter (espace-separe), ex: LANGUAGES=kg ou LANGUAGES="kg ln"
LANGUAGES="${LANGUAGES:-kg}"
WORDSEARCH_ROWS="${WORDSEARCH_ROWS:-20}"
WORDSEARCH_COLS="${WORDSEARCH_COLS:-20}"
CROSSWORD_ROWS="${CROSSWORD_ROWS:-19}"
CROSSWORD_COLS="${CROSSWORD_COLS:-19}"
ARROWWORD_ROWS="${ARROWWORD_ROWS:-17}"
ARROWWORD_COLS="${ARROWWORD_COLS:-17}"
GRID_ENTRIES="${GRID_ENTRIES:-16}"
MORPHO_ENTRIES="${MORPHO_ENTRIES:-12}"
TYPES_PER_CYCLE="${TYPES_PER_CYCLE:-2}"
PROFILES_PER_TYPE="${PROFILES_PER_TYPE:-2}"
TYPE_POOL="${TYPE_POOL:-wordsearch crossword memory domino}"
ARROWWORD_PROFILE_SET="${ARROWWORD_PROFILE_SET:-full}"

# Un dossier par cycle + langue de sens (FR/EN ne s'écrasent pas). Si le dossier existe déjà : -run2, -run3…
# sauf LONGOKA_EXPORT_OVERWRITE=1 (ré-export volontaire dans le même dossier).
_meaning_suffix="$(printf '%s' "$MEANING_LANG" | tr '[:upper:]' '[:lower:]')"
case "$_meaning_suffix" in
  en) ;;
  fr) _meaning_suffix=fr ;;
  *) _meaning_suffix=fr ;;
esac
# Toujours dériver le dossier de CYCLE_ID + langue de sens (sauf override LONGOKA_PREMIUM_LABEL).
if [ -n "${LONGOKA_PREMIUM_LABEL:-}" ]; then
  PREMIUM_LABEL="${LONGOKA_PREMIUM_LABEL}"
else
  PREMIUM_LABEL="${CYCLE_ID}-semaine-premium-${_meaning_suffix}"
fi
ensure_unique_premium_label() {
  _base="$1"
  _label="$_base"
  _n=2
  while [ -d "$BACKEND_ROOT/target/packs/$_label" ] && [ "${LONGOKA_EXPORT_OVERWRITE:-}" != "1" ]; do
    _label="${_base}-run${_n}"
    _n=$((_n + 1))
  done
  printf '%s' "$_label"
}
PREMIUM_LABEL="$(ensure_unique_premium_label "$PREMIUM_LABEL")"
export PREMIUM_LABEL
echo "PREMIUM_LABEL=$PREMIUM_LABEL (meaningLang=${MEANING_LANG}, overwrite=${LONGOKA_EXPORT_OVERWRITE:-0})"

if ! printf '%s' "$TYPES_PER_CYCLE" | grep -Eq '^[0-9]+$'; then
  TYPES_PER_CYCLE=2
fi
if ! printf '%s' "$PROFILES_PER_TYPE" | grep -Eq '^[0-9]+$'; then
  PROFILES_PER_TYPE=2
fi

get_lingala_unlock_date() {
  if [ -n "${LONGOKA_LINGALA_UNLOCK_DATE:-}" ]; then
    printf '%s' "$LONGOKA_LINGALA_UNLOCK_DATE"
    return
  fi
  printf '20260619'
}

assert_language_release_window() {
  local lang="$1"
  local raw_cycle="$2"
  local normalized
  normalized="$(printf '%s' "$lang" | tr '[:upper:]' '[:lower:]' | xargs)"
  if [ "$normalized" != "ln" ] && [ "$normalized" != "lingala" ]; then
    return
  fi

  local unlock_raw current_token
  unlock_raw="$(get_lingala_unlock_date)"
  current_token="$(printf '%s' "$raw_cycle" | cut -c1-8)"

  if ! printf '%s' "$unlock_raw" | grep -Eq '^[0-9]{8}$'; then
    echo "LONGOKA_LINGALA_UNLOCK_DATE doit etre au format yyyyMMdd." >&2
    exit 1
  fi

  if ! printf '%s' "$current_token" | grep -Eq '^[0-9]{8}$'; then
    echo "CYCLE_ID doit etre au format yyyyMMdd pour appliquer la fenetre Lingala." >&2
    exit 1
  fi

  if [ "$current_token" -lt "$unlock_raw" ]; then
    echo "La production hebdomadaire Lingala est bloquee jusqu'au ${unlock_raw}. Pour l'instant, la rotation reste sur le kikongo uniquement." >&2
    exit 1
  fi
}

get_rotation_index() {
  local raw="$1"
  local token epoch_ts cycle_ts diff_days
  token="$(printf '%s' "$raw" | cut -c1-8)"
  if ! printf '%s' "$token" | grep -Eq '^[0-9]{8}$'; then
    echo 0
    return
  fi

  cycle_ts="$(date -u -d "${token:0:4}-${token:4:2}-${token:6:2}" +%s 2>/dev/null || true)"
  epoch_ts="$(date -u -d "2026-01-05" +%s 2>/dev/null || true)"
  if [ -z "$cycle_ts" ] || [ -z "$epoch_ts" ]; then
    echo 0
    return
  fi

  diff_days=$(( (cycle_ts - epoch_ts) / 86400 ))
  if [ "$diff_days" -lt 0 ]; then
    diff_days=0
  fi
  echo $(( diff_days / 7 ))
}

get_type_profile_sets() {
  case "$1" in
    wordsearch)
      printf '%s\n' \
        'class-1-singular mixed-verbs-nouns-singular' \
        'nouns-singular verbs-only' \
        'class-lu-tu-lu-zi-lu-ma-singular class-mu-ba-mu-mi-singular' \
        'class-bu-ma-ku-ma-singular mixed-verbs-nouns-singular'
      ;;
    crossword)
      printf '%s\n' \
        'nouns-singular verbs-only' \
        'mixed-verbs-nouns-singular nouns-singular' \
        'class-1-singular nouns-singular' \
        'class-lu-tu-lu-zi-lu-ma-singular verbs-only'
      ;;
    memory)
      printf '%s\n' \
        'class-lu-tu-lu-zi-lu-ma-singular class-mu-ba-mu-mi-singular' \
        'mixed-verbs-nouns-singular class-1-singular' \
        'verbs-only nouns-singular' \
        'class-bu-ma-ku-ma-singular class-1-singular'
      ;;
    domino)
      printf '%s\n' \
        'class-1-singular class-lu-tu-lu-zi-lu-ma-singular' \
        'class-mu-ba-mu-mi-singular class-bu-ma-ku-ma-singular' \
        'radical-sa-verbs class-1-singular'
      ;;
    *)
      return 0
      ;;
  esac
}

select_profiles_for_type() {
  local type="$1"
  local offset="$2"
  local take="$3"
  mapfile -t profile_sets < <(get_type_profile_sets "$type")
  if [ "${#profile_sets[@]}" -eq 0 ]; then
    return 0
  fi

  local index selected_line count=0
  index=$(( offset % ${#profile_sets[@]} ))
  if [ "$index" -lt 0 ]; then
    index=$(( index + ${#profile_sets[@]} ))
  fi
  selected_line="${profile_sets[$index]}"
  for profile in $selected_line; do
    printf '%s\n' "$profile"
    count=$(( count + 1 ))
    if [ "$count" -ge "$take" ]; then
      break
    fi
  done
}

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
  local profiles_csv="${5:-}"
  local cadence="${LONGOKA_CADENCE:-weekly}"
  local profile_args=""
  if [ -n "$profiles_csv" ]; then
    profile_args="--profiles $profiles_csv"
  fi
  local args="--cadence $cadence --count $COUNT --meaningLang $MEANING_LANG --language $lang --type $type --label $label --tier premium --difficulty expert $extra $profile_args"
  echo ">>> [$lang] $type -> $label"
  if [ -n "$profiles_csv" ]; then
    echo "    profiles=$profiles_csv"
  fi
  "${MVN[@]}" -Dexec.args="$args"
}

arrowword_extra="--rows $ARROWWORD_ROWS --cols $ARROWWORD_COLS --maxEntries $GRID_ENTRIES"
if [ "$ARROWWORD_PROFILE_SET" = "base" ]; then
  arrowword_extra="$arrowword_extra --profileSet base"
fi

rotation_index="$(get_rotation_index "$CYCLE_ID")"
read -r -a type_pool_array <<< "$TYPE_POOL"
if [ "${#type_pool_array[@]}" -eq 0 ]; then
  echo "TYPE_POOL est vide." >&2
  exit 1
fi

selected_types=()
types_limit="$TYPES_PER_CYCLE"
if ! printf '%s' "$types_limit" | grep -Eq '^[0-9]+$'; then
  types_limit=2
fi
if [ "$types_limit" -lt 1 ]; then
  types_limit=1
fi
if [ "$types_limit" -gt "${#type_pool_array[@]}" ]; then
  types_limit="${#type_pool_array[@]}"
fi

start_index=$(( rotation_index % ${#type_pool_array[@]} ))
for ((i=0; i<types_limit; i+=1)); do
  selected_types+=("${type_pool_array[$(((start_index + i) % ${#type_pool_array[@]}))]}")
done

echo "Weekly premium rotation: weekIndex=${rotation_index} selectedTypes=${selected_types[*]}"

for LANG in $LANGUAGES; do
  assert_language_release_window "$LANG" "$CYCLE_ID"
  for idx in "${!selected_types[@]}"; do
    type="${selected_types[$idx]}"
    mapfile -t selected_profiles < <(select_profiles_for_type "$type" $((rotation_index + idx)) "$PROFILES_PER_TYPE")
    profiles_csv=""
    if [ "${#selected_profiles[@]}" -gt 0 ]; then
      profiles_csv="$(IFS=, ; printf '%s' "${selected_profiles[*]}")"
    fi

    case "$type" in
      wordsearch)
        run_batch "$LANG" "$type" "$PREMIUM_LABEL" "--rows $WORDSEARCH_ROWS --cols $WORDSEARCH_COLS --maxEntries $GRID_ENTRIES" "$profiles_csv"
        ;;
      crossword)
        run_batch "$LANG" "$type" "$PREMIUM_LABEL" "--rows $CROSSWORD_ROWS --cols $CROSSWORD_COLS --maxEntries $GRID_ENTRIES" "$profiles_csv"
        ;;
      arrowword)
        run_batch "$LANG" "$type" "$PREMIUM_LABEL" "$arrowword_extra" "$profiles_csv"
        ;;
      domino|memory|scrabble|anagram)
        run_batch "$LANG" "$type" "$PREMIUM_LABEL" "--maxEntries $MORPHO_ENTRIES" "$profiles_csv"
        ;;
      *)
        echo "Unsupported premium type in TYPE_POOL: $type" >&2
        exit 1
        ;;
    esac
  done
done

echo "Termine. Sortie sous: $BACKEND_ROOT/target/packs/$PREMIUM_LABEL (LONGOKA_CADENCE=${LONGOKA_CADENCE:-weekly})"
