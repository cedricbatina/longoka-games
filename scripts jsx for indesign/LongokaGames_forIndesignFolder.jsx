/**
 * Longoka Games — résout le dossier « for Indesign » (scripts partagés type *_Book_FromPack.jsx).
 *
 * Si vous copiez seulement des .jsx dans le panneau Scripts InDesign, définissez ci-dessous le
 * chemin absolu vers le dossier « for Indesign » du dépôt longoka-games, par ex. :
 *   var LONGOKA_GAMES_FOR_INDESIGN = "D:/works/lectures/longoka-games/for Indesign";
 *
 * Vous pouvez aussi définir une fois $.global.LONGOKA_GAMES_FOR_INDESIGN (script de démarrage).
 *
 * Si le dossier LongokaGamesBundle (copié à côté des scripts par games:sync) est présent,
 * il est utilisé en priorité — plus besoin du dossier « for Indesign » ailleurs sur le disque.
 */

var LONGOKA_GAMES_FOR_INDESIGN = "";

function longokaGamesResolveForIndesignFolder(scriptFile) {
  var g = "";
  try {
    if ($.global.LONGOKA_GAMES_FOR_INDESIGN) {
      g = String($.global.LONGOKA_GAMES_FOR_INDESIGN).replace(/^\s+|\s+$/g, "");
    }
  } catch (eG) {}
  if (!g && LONGOKA_GAMES_FOR_INDESIGN) {
    g = String(LONGOKA_GAMES_FOR_INDESIGN).replace(/^\s+|\s+$/g, "");
  }
  if (g) {
    var abs = new Folder(g);
    if (abs.exists) return abs;
  }
  var parent = scriptFile.parent;
  var bundle = new Folder(parent.fsName + "/LongokaGamesBundle");
  if (bundle.exists && new File(bundle.fsName + "/Longoka_Editions_Config.jsx").exists) {
    return bundle;
  }
  var repoStyle = new Folder(parent.parent.fsName + "/for Indesign");
  if (repoStyle.exists) return repoStyle;
  var sibling = new Folder(parent.fsName + "/for Indesign");
  if (sibling.exists) return sibling;
  return null;
}
