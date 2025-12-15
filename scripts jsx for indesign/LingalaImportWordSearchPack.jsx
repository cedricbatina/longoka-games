// Longoka_Import_Wordsearch_Pack.jsx
// Génère un livre InDesign à partir d'un pack JSON de mots mêlés
// (schema = longoka.wordsearch.pack.v1)

(function () {
  if (app.documents.length === 0) {
    // ok, on créera un doc nous-mêmes
  }

  // 1) Choisir le fichier JSON du pack
  var jsonFile = File.openDialog(
    "Sélectionne le fichier JSON du pack Wordsearch (noms / verbes / mixed)",
    "*.json",
    false
  );

  if (!jsonFile) {
    alert("Aucun fichier sélectionné. Script annulé.");
    return;
  }

  if (!jsonFile.exists) {
    alert("Le fichier sélectionné n'existe pas.");
    return;
  }

  // 2) Lire le contenu du fichier
  jsonFile.open("r");
  jsonFile.encoding = "UTF-8";
  var jsonText = jsonFile.read();
  jsonFile.close();

  if (!jsonText || jsonText === "") {
    alert("Le fichier JSON est vide ?");
    return;
  }

  // 3) Parser le JSON (InDesign CC a JSON, mais on met un fallback)
  var pack;
  try {
    if (typeof JSON !== "undefined" && JSON.parse) {
      pack = JSON.parse(jsonText);
    } else {
      // fallback moche mais efficace (le fichier vient de toi)
      pack = eval("(" + jsonText + ")");
    }
  } catch (e) {
    alert("Erreur de parsing JSON :\r" + e);
    return;
  }

  if (!pack || !pack.puzzles || !pack.puzzles.length) {
    alert("Le pack ne contient aucune grille (pack.puzzles est vide).");
    return;
  }

  // 4) Créer un nouveau document InDesign
  var doc = app.documents.add();
  var dp = doc.documentPreferences;
  // A4 portrait (optionnel : tu peux commenter ces lignes pour garder le preset)
  dp.pageWidth = "210mm";
  dp.pageHeight = "297mm";
  dp.facingPages = false;

  var mp = doc.marginPreferences;
  var marginTop = mp.top;
  var marginLeft = mp.left;
  var marginRight = mp.right;
  var marginBottom = mp.bottom;

  var pageWidth = dp.pageWidth;
  var pageHeight = dp.pageHeight;

  // 5) Parcourir les puzzles du pack
  var puzzles = pack.puzzles;
  for (var i = 0; i < puzzles.length; i++) {
    var puzzle = puzzles[i];

    // Première page : on utilise doc.pages[0]
    // Les suivantes : doc.pages.add()
    var page;
    if (i === 0) {
      page = doc.pages[0];
    } else {
      page = doc.pages.add();
    }

    // ---- Zones de mise en page approximatives ----
    // On découpe la page en :
    // - bande titre en haut
    // - grande zone pour la grille
    // - zone basse pour la liste des mots

    var titleTop = marginTop;
    var titleLeft = marginLeft;
    var titleRight = pageWidth - marginRight;
    var titleBottom = titleTop + 20; // hauteur du titre (points)

    var gridTop = titleBottom + 10;
    var gridLeft = marginLeft;
    var gridRight = pageWidth - marginRight;
    var gridBottom = gridTop + 220; // hauteur approximative pour la grille

    var listTop = gridBottom + 10;
    var listLeft = marginLeft;
    var listRight = pageWidth - marginRight;
    var listBottom = pageHeight - marginBottom;

    // 5.1 Cadre de titre
    var titleFrame = page.textFrames.add();
    titleFrame.geometricBounds = [
      titleTop,
      titleLeft,
      titleBottom,
      titleRight
    ];

    var packTitle = pack.title || "Mots mêlés";
    var puzzleTitle = puzzle.title || "";
    var finalTitle =
      packTitle + " – " + (puzzleTitle !== "" ? puzzleTitle : puzzle.id || "");

    titleFrame.contents =
      finalTitle +
      "\r(" +
      (puzzle.mode || "mode inconnu") +
      " – " +
      (puzzle.language || "??") +
      ")";

    var titleStory = titleFrame.parentStory;
    titleStory.pointSize = 16;
    titleStory.leading = 18;
    titleStory.justification = Justification.CENTER_ALIGN;

    // 5.2 Cadre de grille
    var gridFrame = page.textFrames.add();
    gridFrame.geometricBounds = [gridTop, gridLeft, gridBottom, gridRight];

    // Construire le texte de la grille :
    // chaque ligne du puzzle.grid est une string "ABCDEFGHIJKL"
    // On insère un espace entre les lettres pour l'aération.
    var gridLines = [];
    if (puzzle.grid && puzzle.grid.length) {
      for (var r = 0; r < puzzle.grid.length; r++) {
        var rowStr = puzzle.grid[r];
        if (!rowStr) rowStr = "";
        var spaced = rowStr.split("").join(" ");
        gridLines.push(spaced);
      }
    }

    gridFrame.contents = gridLines.join("\r");

    var gridStory = gridFrame.parentStory;
    gridStory.pointSize = 18;
    gridStory.leading = 20;
    try {
      gridStory.appliedFont = app.fonts.item("Courier New");
    } catch (eFont) {
      // Si "Courier New" n'existe pas, on laisse le font par défaut
    }
    gridStory.justification = Justification.CENTER_ALIGN;

    // 5.3 Cadre de liste des mots
    var listFrame = page.textFrames.add();
    listFrame.geometricBounds = [listTop, listLeft, listBottom, listRight];

    var entries = puzzle.entries || [];
    var listLines = [];

    for (var j = 0; j < entries.length; j++) {
      var e = entries[j];
      if (!e) continue;

      var label = e.display || e.base || "";
      if (label === "") continue;

      var part = e.partOfSpeech ? " [" + e.partOfSpeech + "]" : "";
      var tr = e.translation ? " — " + e.translation : "";
      listLines.push("• " + label + part + tr);
    }

    if (listLines.length === 0) {
      listLines.push("(aucune entrée trouvée dans puzzle.entries)");
    }

    listFrame.contents = listLines.join("\r");

    var listStory = listFrame.parentStory;
    listStory.pointSize = 10;
    listStory.leading = 12;
    listStory.justification = Justification.LEFT_ALIGN;
  }

  alert(
    "Import terminé : " +
      puzzles.length +
      " grilles créées dans le document InDesign."
  );
})();
