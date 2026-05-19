# Dev Journal — longoka-games

Génération Java des packs JSON (Lexikongo / LexiLingala) consommés par l’app **Longoka** (`../longoka`).

Documentation détaillée : **`backend-java/GENERATION.md`**.

## 2026-05-13b — Flux sans écrasement des dossiers export

- `PREMIUM_LABEL` = `<cycle>-semaine-premium-fr|en` ; si dossier existant → `-run2`, etc.
- Scripts : `run-export-fr-en-premium.sh` / `.ps1` (option `pnpm games:sync` côté Longoka).

## 2026-05-13 — Mulongoki, target/packs, Lingala différé

- **`BiweeklyPuzzleBatchTool`** : sortie sous `target/packs/<label>/`, édition **Mulongoki**, méta livre (`publicationStart`, `publicationEnd`, `archiveAfter`), durcissement vitrine publique.
- **`PackMeaningMeta`** : alignement pluriel / `translationsSingularOnly` avec le serveur Longoka.
- **`run-export-all-premium.sh`** : rotation hebdo, `LONGOKA_LINGALA_UNLOCK_DATE` (défaut `20260619`) pour bloquer la prod Lingala avant la date.
- **`run-export-all-pro.sh`** : point d’entrée CI (délègue au script premium).
- **Script Windows** : `backend-java/scripts/run-export-fr-en-premium.ps1` (export FR + EN, option sync vers Longoka).

## Chaîne vers Longoka

1. Export ici → `backend-java/target/packs/…`
2. Sync → `longoka/scripts/sync-games-packs.mjs` (suffixe `-fr.json` / `-en.json`, enrichissement `meta.book`)
3. CI GitHub sur **longoka-premium** (secrets `LEX_KG_*`, `LEX_LN_*`) — voir `../longoka/.github/GAMES_AUTOMATION.md`
