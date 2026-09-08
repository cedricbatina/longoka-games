# Adaptateur P1b × Mufimbi — CORRECTION Founder 2026-07-30 ~18:21

> **STOP** sur la piste « manifeste HTML P1b → WordToFind sans Lexikongo ».

## Canon produit (réaffirmé Founder)

| Source | Rôle |
|--------|------|
| **lexikongo.fr / DB Lexikongo** | **seule** source de mots pour la génération Mufimbi (`BiweeklyPuzzleBatchTool` → `LexikongoWordRepository` / `LexikongoVerbRepository`) |
| **longoka-games** | moteur packs (`wordsearch`, etc.) |
| **packs P1b / HTML cours** | pédagogie Longoka — **pas** un dictionnaire jeux |

## Erreur de session (à ne pas reprendre)

Les fichiers :

- `data/p1b-pilot-lexicon-manifest-2026-07-30.json`
- `data/p1b-pilot-lexicon-approved-2026-07-30.json` (10 entrées)

sont une **extraction HTML** des leçons pilote (14, 15, 345, 20, 344) — **pas** des entrées Lexikongo.  
Les présenter comme « les 10 mots du pack Mufimbi » est **faux**.

Les 10 (pour mémoire, hors pipeline jeux) :  
`baena`, `bamama`, `bunga`, `diambu`, `kituka`, `mbongo`, `yiaku`, `yiame`, `yiandi`, `yiawu`.

## Suite GO1 — FAIT 2026-07-30 ~18:40 (GO Founder)

1. **Généré** via Lexikongo (`BiweeklyPuzzleBatchTool`) :
   ```powershell
   cd D:\works\lectures\longoka-games\backend-java
   mvn -q -DskipTests compile exec:java `
     "-Dexec.mainClass=com.longoka.games.app.BiweeklyPuzzleBatchTool" `
     "-Dexec.args=--cadence weekly --count 1 --meaningLang fr --language kg --type wordsearch --label 20260730-mufimbi-lexikongo-pilot --tier premium"
   ```
   Sortie : `target/packs/20260730-mufimbi-lexikongo-pilot/` · **19** JSON wordsearch (1 grille / profil) · `editionTier=mufimbi` · mots Lexikongo (ex. `Fuanuka` = « Être à la hauteur »).

2. **Dry-run** OK :
   ```powershell
   cd D:\works\lectures\longoka
   $env:GAMES_PACKS_SOURCE = "D:\works\lectures\longoka-games\backend-java\target"
   node scripts/sync-games-packs.mjs --cadence=all --dry-run --only-label=20260730-mufimbi-lexikongo-pilot
   ```
   → **19** fichiers seraient écrits sous `longoka/server/data/games` (aucun write).

3. `pnpm games:sync` (écriture) = **GO Founder séparé**.

Note : `--count 1` = 1 puzzle **par profil** (19 profils kg wordsearch), pas un seul fichier global.

Factory reste hors génération jeux.
