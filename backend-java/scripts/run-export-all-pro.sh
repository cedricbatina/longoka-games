#!/usr/bin/env bash
# Entrée CI / app : export hebdomadaire (weekly-safe, tous types app, pas de croisés).
# Pour l'export premium manuel (rotation 2 types, profils classes) : run-export-all-premium.sh

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
exec bash "$SCRIPT_DIR/run-export-app-weekly.sh" "$@"
