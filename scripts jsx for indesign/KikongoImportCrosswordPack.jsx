#target "InDesign"

(function () {
  // --- 1. Choisir le fichier JSON ---
  var jsonFile = File.openDialog("Choisir un pack JSON Longoka (crossword)", "*.json");
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
  dp.pageWidth = "210mm";
  dp.pageHeight = "297mm";
  dp.pagesPerDocument = pack.puzzles.length;

  var marginTop = 20;
  var marginLeft = 20;
  var marginRight = 20;
  var marginBottom = 20;

  function mm(n) {
    return n * 2.834645;
  }

  // --- 3. Parcours des puzzles ---
  for (var i = 0; i < pack.puzzles.length; i++) {
    var puzzle = pack.puzzles[i];
    var page = doc.pages[i];

    var pageBounds = page.bounds;
    var top = pageBounds[0] + mm(marginTop);
    var left = pageBounds[1] + mm(marginLeft);
    var right = pageBounds[3] - mm(marginRight);
    var bottom = pageBounds[2] - mm(marginBottom);

    var midY = top + (bottom - top) * 0.45;

    // --- 3.1. Titre ---
    var titleFrame = page.textFrames.add();
    titleFrame.geometricBounds = [
      top - mm(10),
      left,
      top,
      right
    ];
    var puzzleTitle = (pack.title || "Mots croisés") + " – " + (puzzle.title || puzzle.id || ("Grille " + (i + 1)));
    titleFrame.contents = puzzleTitle;
    var titleStory = titleFrame.parentStory;
    titleStory.pointSize = 16;
    titleStory.leading = 18;
    try {
      titleStory.appliedFont = "Arial Bold";
    } catch (_) { }

    // --- 3.2. Grille ---
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

    // --- 3.3. Indices ACROSS / DOWN ---
    var entries = puzzle.entries || [];

    var acrossText = "ACROSS\r";
    var downText = "DOWN\r";

    for (var j = 0; j < entries.length; j++) {
      var e = entries[j];
      var num = e.number || 0;
      var dir = e.direction || "";
      var disp = e.display || e.answer || "";
      var clue = e.clue || e.translation || "";
      var pos = e.partOfSpeech || "";
      var extra = e.extraInfo || "";

      var line = num + ". " + disp;
      if (pos) line += " [" + pos + "]";
      if (extra) line += " (" + extra + ")";
      if (clue) line += " – " + clue;

      if (dir === "ACROSS") {
        acrossText += line + "\r";
      } else if (dir === "DOWN") {
        downText += line + "\r";
      } else {
        acrossText += line + "\r"; // fallback
      }
    }

    // 2 colonnes : gauche = ACROSS, droite = DOWN
    var cluesLeftFrame = page.textFrames.add();
    cluesLeftFrame.geometricBounds = [
      midY + mm(3),
      left,
      bottom,
      left + (right - left) * 0.5 - mm(2)
    ];
    cluesLeftFrame.contents = acrossText;

    var cluesRightFrame = page.textFrames.add();
    cluesRightFrame.geometricBounds = [
      midY + mm(3),
      left + (right - left) * 0.5 + mm(2),
      bottom,
      right
    ];
    cluesRightFrame.contents = downText;

    var cluesLeftStory = cluesLeftFrame.parentStory;
    var cluesRightStory = cluesRightFrame.parentStory;
    cluesLeftStory.pointSize = 10.5;
    cluesRightStory.pointSize = 10.5;
    cluesLeftStory.leading = 12;
    cluesRightStory.leading = 12;
    try {
      cluesLeftStory.appliedFont = "Arial";
      cluesRightStory.appliedFont = "Arial";
    } catch (_) { }
  }

  alert("Import crossword terminé : " + pack.puzzles.length + " grilles importées.");
})();
