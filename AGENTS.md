# AGENTS.md — longoka-games

Génération de packs jeux (Java) pour Longoka : mots mêlés, anagrammes, dominos, etc.  
Sorties consommées par `longoka` via sync stationery / games.

> Équipe Codex : `D:\works\lectures\docs\team\CODEX-START.md` · `~/.codex/AGENTS.md`  
> App Nuxt : voir `D:\works\lectures\longoka\AGENTS.md`

## Structure

| Dossier | Rôle |
|---------|------|
| `backend-java/` | moteur packs, targets Maven, `target/packs/` |
| `for Indesign/` | assets couvertures / exports |

## Éditions jeux (Longoka)

| Collection | Jeux typiques |
|------------|----------------|
| **Mufimbi** | mots mêlés (`wordsearch`) — pack invitation cours |
| **Mulongoki** | anagrammes, mots croisés |
| **Muvovi** | mots fléchés |
| **Muzonzi** | dominos |

Ne pas confondre **Mufimbi** (collection) avec le **titre** du livre côté Nuxt.

## Scripts côté longoka (repo sibling)

Exécuter depuis `D:\works\lectures\longoka` :

```powershell
pnpm stationery:kikongo:production
pnpm games:sync
pnpm stationery:books:sync
```

Catalogue canonique : `longoka/server/data/stationery-canonical.json`

## Règles

- Diff minimal ; conventions Java existantes dans `backend-java/`.
- Packs datés par batch (`*-semaine-app-fr`, etc.).
- **Git** : commit/push uniquement sur demande de Cédric.
- Journal : `D:\works\lectures\docs\team\sessions\`

## Collaboration

Tâches lourdes génération/sync → **Codex**.  
Intégration UI/API Longoka → **Cursor** ou Cédric+Codex sur repo `longoka`.
