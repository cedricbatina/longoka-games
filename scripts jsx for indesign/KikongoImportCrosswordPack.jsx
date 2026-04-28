#target "InDesign"

(function () {
  var thisFile = File($.fileName);
  var autonomousBundle = new Folder(thisFile.parent.fsName + "/LongokaGamesBundle");
  if (autonomousBundle.exists) {
    var bundledEntry = File(autonomousBundle.fsName + "/Longoka_Crossword_Book_FromPack.jsx");
    if (bundledEntry.exists) {
      $.global.LG_BASE_FOLDER = autonomousBundle.fsName;
      $.evalFile(bundledEntry);
      return;
    }
  }
  try {
    var helper = File(thisFile.parent + "/LongokaGames_forIndesignFolder.jsx");
    if (helper.exists) $.evalFile(helper);
  } catch (e0) {}

  var forIndesignFolder = null;
  if (typeof longokaGamesResolveForIndesignFolder === "function") {
    forIndesignFolder = longokaGamesResolveForIndesignFolder(thisFile);
  }
  if (!forIndesignFolder || !forIndesignFolder.exists) {
    var p = thisFile.parent;
    var c1 = new Folder(p.parent.fsName + "/for Indesign");
    var c2 = new Folder(p.fsName + "/for Indesign");
    if (c1.exists) forIndesignFolder = c1;
    else if (c2.exists) forIndesignFolder = c2;
  }
  if (!forIndesignFolder || !forIndesignFolder.exists) {
    alert(
      "Longoka — dossier « for Indesign » introuvable.\r\r" +
        "Copiez le dossier « for Indesign » depuis longoka-games (même niveau que « scripts jsx for indesign » dans le dépôt), ou placez-le à côté de ce script, ou renseignez LONGOKA_GAMES_FOR_INDESIGN dans LongokaGames_forIndesignFolder.jsx."
    );
    return;
  }
  var entry = File(forIndesignFolder.fsName + "/Longoka_Crossword_Book_FromPack.jsx");
  if (!entry.exists) {
    alert("Script manquant :\r" + entry.fsName);
    return;
  }
  $.global.LG_BASE_FOLDER = forIndesignFolder.fsName;
  $.evalFile(entry);
})();
