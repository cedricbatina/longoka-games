Couvertures Longoka Games (InDesign)
===================================

Objectif: garder des couvertures **cohérentes** (shop / app / impression) tout en permettant une vraie direction artistique.

## Option recommandée (meilleure qualité)

1) Créer 1–2 templates InDesign **à la main**:

- `Longoka_Cover_Template_Standard.indd` (ou `.idml`)
- `Longoka_Cover_Template_Premium.indd` (ou `.idml`)

2) Dans le template, **nommer** les frames (Window → Utilities → Scripts + panneau de calques/objets):

- **Text frames**:
  - `LG_Cover_Title`
  - `LG_Cover_Subtitle`
  - `LG_Cover_Badges`
  - `LG_Cover_ISBN`
- **Image frame** (rectangle):
  - `LG_Cover_Image`

3) Configurer les chemins dans `Longoka_Editions_Config.jsx`:

- `LG.COVER.templates.standard`
- `LG.COVER.templates.premium`

4) Lancer `Longoka_Cover_FromTemplate.jsx`:

- Choisir le pack JSON
- Le script ouvre le bon template (standard/premium), remplit les textes depuis `meta.book.*`,
  place l'image si `meta.book.coverImage` est défini, exporte un PNG preview, et sauve un `.indd` à côté du JSON.

## Option “100% script” (rapide, mais moins premium)

Utiliser `Longoka_Cover_FromPack.jsx`:

- Crée une couverture simple en 6x9 avec palette/badges de base
- Très utile pour maquettes rapides et previews, moins pour une vraie couverture d’éditeur

## Champs JSON utilisés (meta.book)

- `title`, `subtitle`
- `trimSize` (ex: `6x9in`)
- `printVariant` (ex: `standard-softcover`, `premium-softcover`)
- `isbn`
- `meaningLanguage` (`fr` ou `en`)
- `coverImage` (optionnel): chemin vers un `.png`/`.jpg` à placer dans le template

