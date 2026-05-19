#!/usr/bin/env bash
# Export FR puis EN dans des dossiers distincts (…-premium-fr / …-premium-en).
# Ne réutilise pas un dossier existant sauf LONGOKA_EXPORT_OVERWRITE=1.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$BACKEND_ROOT"

CYCLE_ID="${CYCLE_ID:-$(date +%Y%m%d)}"
export CYCLE_ID

for lang in fr en; do
  echo ""
  echo "=== MEANING_LANG=${lang} ==="
  unset PREMIUM_LABEL
  MEANING_LANG="$lang" bash "$SCRIPT_DIR/run-export-all-premium.sh"
done

echo ""
echo "Terminé. Voir target/packs/ (dossiers *-semaine-premium-fr et *-semaine-premium-en)."
