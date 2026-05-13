#!/usr/bin/env bash
# Canonical pro entrypoint. Delegates to the historical premium-named script.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
exec bash "$SCRIPT_DIR/run-export-all-premium.sh" "$@"
