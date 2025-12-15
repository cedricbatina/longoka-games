// Longoka_Import_Crossword_Pack.jsx
// Génère un livre InDesign à partir d'un pack JSON de mots croisés
// (format = longoka.crossword.pack.v1)

(function () {
  // 1) Choisir le fichier JSON du pack
  var jsonFile = File.openDialog(
    "Sélectionne le fichier JSON du pack Crossword (noms / verbes / mixed)",
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

  // 3) Parser le JSON
  var pack;
  try {
    if (typeof JSON !== "undefined" && JSON.parse) {
      pack = JSON.parse(jsonText);
    } else {
      pack = eval("(" + jsonText + ")"); // fallback
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
  // A4 portrait
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

  var puzzles = pack.puzzles;

  // 5) Parcourir les puzzles du pack
  for (var i = 0; i < puzzles.length; i++) {
    var puzzle = puzzles[i];

    var page;
    if (i === 0) {
      page = doc.pages[0];
    } else {
      page = doc.pages.add();
    }

    // Zones de mise en page (simple mais efficace)
    var titleTop = marginTop;
    var titleLeft = marginLeft;
    var titleRight = pageWidth - marginRight;
    var titleBottom = titleTop + 24;

    var gridTop = titleBottom + 10;
    var gridLeft = marginLeft;
    var gridRight = pageWidth - marginRight;
    var gridBottom = gridTop + 220;

    var cluesTop = gridBottom + 10;
    var cluesLeft = marginLeft;
    var cluesRight = pageWidth - marginRight;
    var cluesBottom = pageHeight - marginBottom;

    // 5.1 Cadre de titre
    var titleFrame = page.textFrames.add();
    titleFrame.geometricBounds = [
      titleTop,
      titleLeft,
      titleBottom,
      titleRight
    ];

    var packTitle = pack.title || "Mots croisés";
    var puzzleTitle = puzzle.title || "";
    var finalTitle =
      packTitle + " – " + (puzzleTitle !== "" ? puzzleTitle : (puzzle.id || ""));

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

    var gridLines = [];
    if (puzzle.grid && puzzle.grid.length) {
      for (var r = 0; r < puzzle.grid.length; r++) {
        var rowStr = puzzle.grid[r];
        if (!rowStr) rowStr = "";
        var chars = rowStr.split("");
        for (var k = 0; k < chars.length; k++) {
          if (chars[k] === "#") {
            // case noire → bloc plein pour que ce soit plus joli
            chars[k] = "■";
          }
        }
        var spaced = chars.join(" ");
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
      // si non dispo, on laisse la police par défaut
    }
    gridStory.justification = Justification.CENTER_ALIGN;

    // 5.3 Cadre des définitions (ACROSS / DOWN)
    var cluesFrame = page.textFrames.add();
    cluesFrame.geometricBounds = [
      cluesTop,
      cluesLeft,
      cluesBottom,
      cluesRight
    ];

    var entries = puzzle.entries || [];
    var across = [];
    var down = [];

    // Séparer ACROSS / DOWN et trier par number
    for (var j = 0; j < entries.length; j++) {
      var e = entries[j];
      if (!e) continue;

      var lineNum = e.number || 0;
      var label = e.display || e.answer || "";
      var clue = e.clue || e.translation || "";
      var pos = e.partOfSpeech ? " [" + e.partOfSpeech + "]" : "";
      var phon = e.phonetic ? " /" + e.phonetic + "/" : "";
      var en = e.translationEn ? " (EN: " + e.translationEn + ")" : "";

      var text =
        lineNum +
        ". " +
        label +
        pos +
        phon +
        (clue ? " — " + clue : "") +
        en;

      if ((e.direction || "").toUpperCase() === "DOWN") {
        down.push({ number: lineNum, text: text });
      } else {
        // par défaut on met en ACROSS
        across.push({ number: lineNum, text: text });
      }
    }

    // tri par numéro
    across.sort(function (a, b) {
      return a.number - b.number;
    });
    down.sort(function (a, b) {
      return a.number - b.number;
    });

    var clueLines = [];

    if (across.length > 0) {
      clueLines.push("ACROSS");
      for (var a = 0; a < across.length; a++) {
        clueLines.push(across[a].text);
      }
      clueLines.push(""); // ligne vide entre les blocs
    }

    if (down.length > 0) {
      clueLines.push("DOWN");
      for (var d = 0; d < down.length; d++) {
        clueLines.push(down[d].text);
      }
    }

    if (clueLines.length === 0) {
      clueLines.push("(aucune entrée dans puzzle.entries)");
    }

    cluesFrame.contents = clueLines.join("\r");

    var cluesStory = cluesFrame.parentStory;
    cluesStory.pointSize = 9;
    cluesStory.leading = 11;
    cluesStory.justification = Justification.LEFT_ALIGN;
  }

  alert(
    "Import Crossword terminé : " +
      puzzles.length +
      " grilles créées dans le document InDesign."
  );
})();
