#target "InDesign"

(function () {
  // --- 1. Choisir le fichier JSON ---
  var jsonFile = File.openDialog("Choisir un pack JSON Longoka (wordsearch)", "*.json");
  if (!jsonFile) {
    alert("Opération annulée.");
    return;
  }

  jsonFile.open("r");
  var jsonStr = jsonFile.read();
  jsonFile.close();

  if (typeof JSON === "undefined" || !JSON.parse) {
    alert("JSON.parse n'est pas disponible dans cette version d'ExtendScript.");
    return;
  }

  var pack;
  try {
    pack = JSON.parse(jsonStr);
  } catch (e) {
    alert("Erreur de parsing JSON : " + e);
    return;
  }

  if (!pack || !pack.puzzles || !pack.puzzles.length) {
    alert("Pack JSON invalide ou vide.");
    return;
  }

  // --- 2. Création du document ---
  var doc = app.documents.add();
  var dp = doc.documentPreferences;
  // A4 portrait
  dp.pageWidth = "210mm";
  dp.pageHeight = "297mm";
  dp.pagesPerDocument = pack.puzzles.length;

  // Marges simples
  var marginTop = 20;  // mm
  var marginLeft = 20; // mm
  var marginRight = 20;
  var marginBottom = 20;

  // Petite fonction utilitaire pour convertir mm → points
  function mm(n) {
    return n * 2.834645; // 1mm ≈ 2.834645 pt
  }

  // --- 3. Parcours des puzzles ---
  for (var i = 0; i < pack.puzzles.length; i++) {
    var puzzle = pack.puzzles[i];

    var page = doc.pages[i]; // InDesign a déjà pagesPerDocument pages

    // Zones : haut = grille, bas = liste de mots
    var pageBounds = page.bounds; // [y1, x1, y2, x2]
    var top = pageBounds[0] + mm(marginTop);
    var left = pageBounds[1] + mm(marginLeft);
    var right = pageBounds[3] - mm(marginRight);
    var bottom = pageBounds[2] - mm(marginBottom);

    var midY = top + (bottom - top) * 0.45; // 45% pour la grille, 55% pour la liste

    // --- 3.1. Titre de la page ---
    var titleFrame = page.textFrames.add();
    titleFrame.geometricBounds = [
      top - mm(10),
      left,
      top,
      right
    ];
    var puzzleTitle = (pack.title || "Mots mêlés") + " – " + (puzzle.title || puzzle.id || ("Grille " + (i + 1)));
    titleFrame.contents = puzzleTitle;
    var titleStory = titleFrame.parentStory;
    titleStory.pointSize = 16;
    titleStory.leading = 18;
    try {
      titleStory.appliedFont = "Arial Bold";
    } catch (_) { }

    // --- 3.2. Grille (texte monospace) ---
    var gridFrame = page.textFrames.add();
    gridFrame.geometricBounds = [
      top,
      left,
      midY,
      right
    ];

    var gridLines = (puzzle.grid && puzzle.grid.length) ? puzzle.grid : [];
    var gridText = "";
    for (var g = 0; g < gridLines.length; g++) {
      gridText += gridLines[g] + "\r";
    }
    gridFrame.contents = gridText;

    var gridStory = gridFrame.parentStory;
    gridStory.pointSize = 18;
    gridStory.leading = 20;
    try {
      gridStory.appliedFont = "Courier New";
    } catch (_) { }

    // --- 3.3. Liste des mots (entries) ---
    var entriesFrame = page.textFrames.add();
    entriesFrame.geometricBounds = [
      midY + mm(3),
      left,
      bottom,
      right
    ];

    var entries = puzzle.entries || [];
    var listText = "";

    for (var j = 0; j < entries.length; j++) {
      var e = entries[j];
      var base = e.base || e.display || e.slug || "";
      var tr = e.translation || "";
      var pos = e.partOfSpeech || "";
      var extra = e.extraInfo || "";

      var line = base;
      if (pos) {
        line += " [" + pos + "]";
      }
      if (extra) {
        line += " (" + extra + ")";
      }
      if (tr) {
        line += " – " + tr;
      }
      listText += line + "\r";
    }

    if (listText === "") {
      listText = "[Aucune entrée trouvée dans puzzle.entries]";
    }

    entriesFrame.contents = listText;
    var entriesStory = entriesFrame.parentStory;
    entriesStory.pointSize = 11;
    entriesStory.leading = 13;
    try {
      entriesStory.appliedFont = "Arial";
    } catch (_) { }
  }

  alert("Import wordsearch terminé : " + pack.puzzles.length + " grilles importées.");
})();
