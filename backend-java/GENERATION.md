# Génération des packs (JSON)

## Outil principal

- Classe : `com.longoka.games.app.BiweeklyPuzzleBatchTool`
- Sortie : sous **`target/packs/<label>/`** (un seul arbre ; plus de dossiers `weekly/` vs `biweekly/`).
- `--cadence` : **`weekly`** (valeur par défaut). `biweekly` est accepté en alias et **normalisé vers `weekly`** (un seul `meta.exportCadence`). **`meta.scope`** est toujours `longoka-batch`. Les **`packId`** utilisent le segment `-batch-` + date.
- Anciens dossiers **`target/weekly`** / **`target/biweekly`** encore présents sur le disque : **`scripts/merge-legacy-cadence-target-dirs.ps1`** les copie sous **`target/packs/`** (puis suppression manuelle des sources si OK).

## Commande type (Maven)

```bash
cd backend-java
mvn -q -DskipTests compile exec:java ^
  "-Dexec.mainClass=com.longoka.games.app.BiweeklyPuzzleBatchTool" ^
  "-Dexec.args=--cadence weekly --count 24 --rows 12 --cols 12 --maxEntries 10 --meaningLang fr --type wordsearch --label ma-vague"
```

Voir `scripts/run-bihebdo-cycle.ps1` (bihebdo : wordsearch + crossword, option `-IncludeMorphoPacks`) et les scripts `run-premium-cycle*.ps1` pour les gammes complètes.

## Méta consommée par Longoka / InDesign

- `finalizePackBookMeta` dans `BiweeklyPuzzleBatchTool` remplit `meta.book` (titre, ISBN, etc.).
- `PackMeaningMeta` (package `com.longoka.games.meta`) pose :
  - `meta.translationsSingularOnly` (défaut `true`)
  - `meta.includesPluralGameForms` (inféré depuis profils / titres, aligné serveur Longoka)

Les scripts InDesign sous `../for Indesign/` lisent ces champs pour le texte « Sens FR/EN » sur la page Mode d’emploi (`LG.lexiconPolicyTextFromPack`).

## Automatisation (CI + sync vers l’app)

Le dépôt **Longoka** (app / `longoka-premium`) peut exécuter **chaque semaine** l’export complet (`run-export-all-premium.sh`), copier les JSON dans `server/data/games` et pousser un commit. Configuration des secrets GitHub, variables et horaires : **`.github/GAMES_AUTOMATION.md`** dans ce dépôt Longoka (pas dans `longoka-games`).

En local sans CI : Planificateur de tâches Windows ou `cron`, en pointant sur les scripts de ce dossier `scripts/` (Maven + variables `LEX_*_DB_*`).
