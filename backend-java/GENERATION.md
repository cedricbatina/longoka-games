# Génération des packs (JSON)

Dépôt **longoka-games** → sortie consommée par l’app **Longoka** (`D:\works\lectures\longoka`).

## Outil principal

| Élément | Détail |
|--------|--------|
| Classe | `com.longoka.games.app.BiweeklyPuzzleBatchTool` |
| Sortie | **`target/packs/<label>/`** (ex. `20260513-semaine-premium`) |
| Cadence | `--cadence weekly` (défaut). `biweekly` → normalisé **`weekly`** dans `meta.exportCadence` |
| Fichiers | `kg-…-wordsearch-pack.v1.json`, `ln-…`, etc. |
| Sens (glosses) | `--meaningLang fr` ou `en` → champ `meaningLanguage` + `meta.book.meaningLanguage` |

Anciens dossiers `target/weekly` / `target/biweekly` : fusionner avec **`scripts/merge-legacy-cadence-target-dirs.ps1`** si besoin.

## Entrées rapides

### Windows (PowerShell, sans Git Bash)

```powershell
cd D:\works\lectures\longoka-games\backend-java

# Un cycle, une langue de sens
.\scripts\run-premium-cycle.ps1 -MeaningLang fr -LanguageMode kg

# FR + EN (recommandé avant sync Longoka)
.\scripts\run-export-fr-en-premium.ps1 -LanguageMode kg

# FR + EN + sync vers Longoka
.\scripts\run-export-fr-en-premium.ps1 -SyncToLongoka
```

### Linux / macOS / GitHub Actions

```bash
cd backend-java
export LEX_KG_DB_HOST=… LEX_KG_DB_USER=… LEX_KG_DB_PASS=… LEX_KG_DB_NAME=…
# optionnel Lingala : LEX_LN_DB_*

bash scripts/run-export-all-pro.sh          # = run-export-all-premium.sh, MEANING_LANG=fr
MEANING_LANG=en bash scripts/run-export-all-pro.sh
```

Variables utiles : `CYCLE_ID`, `MEANING_LANG`, `LANGUAGES=kg` ou `kg ln`, `COUNT=48`, `TYPE_POOL`, `LONGOKA_CADENCE=weekly`.

### Lingala (production)

Tant que `CYCLE_ID` (8 premiers chiffres `yyyyMMdd`) est **&lt; `LONGOKA_LINGALA_UNLOCK_DATE`** (défaut **`20260619`**), l’export **ln** est refusé par `run-export-all-premium.sh`.

La **vitrine publique** Longoka applique en plus un retard (ex. 90 j) via `GAMES_PUBLIC_MIN_AGE_DAYS_LN` sur Vercel.

## Dossiers d’export (ne pas écraser)

| Règle | Détail |
|--------|--------|
| FR / EN | Dossiers séparés : `…-semaine-premium-fr` et `…-semaine-premium-en` |
| Re-run même jour | Si le dossier existe : `…-run2`, `…-run3` (sauf `LONGOKA_EXPORT_OVERWRITE=1`) |
| Historique | Les anciens dossiers sous `target/packs/` restent sur le disque |

Au sync, Longoka ajoute **`-fr` / `-en`** sur le nom de fichier et, en cas de collision dans `server/data/games/`, **`--<nom-du-dossier-cycle>`**.

## Méta consommée par Longoka / InDesign

- `finalizePackBookMeta` : `meta.book` (titre, ISBN, dates publication, etc.)
- `PackMeaningMeta` : `translationsSingularOnly`, `includesPluralGameForms`
- Scripts **`../for Indesign/`** : couvertures, grilles, `LG.meaningLanguageFromPack(pack)`

Config DB : **`config/README.txt`**, surcharge CI via `LEX_KG_DB_*` / `LEX_LN_DB_*`.

ISBN réel : `LONGOKA_BOOK_ISBN` ou `-Dlongoka.book.isbn=978…`

## Sync vers Longoka (local)

```powershell
cd D:\works\lectures\longoka
$env:GAMES_PACKS_SOURCE = "D:\works\lectures\longoka-games\backend-java\target"
node scripts/sync-games-packs.mjs --cadence=all
node scripts/generate-games-bundle.mjs
```

## CI (automatique)

Le workflow vit dans le dépôt **longoka** (pas ici) : clone `longoka-games`, `run-export-all-pro.sh` (FR + EN), sync, commit `server/data/games`.

Voir **`../longoka/.github/GAMES_AUTOMATION.md`**.

## Commande Maven manuelle

```bash
mvn -q -DskipTests compile exec:java \
  -Dexec.mainClass=com.longoka.games.app.BiweeklyPuzzleBatchTool \
  -Dexec.args="--cadence weekly --count 48 --meaningLang fr --language kg --type wordsearch --label 20260513-semaine-premium --tier premium"
```
