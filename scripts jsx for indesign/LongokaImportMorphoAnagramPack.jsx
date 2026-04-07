#target "InDesign"

(function () {
  function mm(n) {
    return n * 2.834645;
  }

  function safe(v, fallback) {
    return (v === null || v === undefined || v === "") ? (fallback || "") : String(v);
  }

  function ensureJson() {
    if (typeof JSON === "undefined" || !JSON.parse) {
      alert("JSON.parse n'est pas disponible dans cette version d'ExtendScript.");
      return false;
    }
    return true;
  }

  function chooseJsonFile() {
    return File.openDialog("Choisir un pack JSON Longoka (anagrammes morphologiques)", "*.json");
  }

  function parseJsonFile(file) {
    file.open("r");
    var content = file.read();
    file.close();
    return JSON.parse(content);
  }

  function validatePack(pack) {
    if (!pack || pack.format !== "longoka.morpho-anagram.pack.v1") {
      throw new Error("Le fichier choisi n'est pas un pack d'anagrammes morphologiques Longoka valide.");
    }
    if (!pack.puzzles || !pack.puzzles.length) {
      throw new Error("Le pack ne contient aucun puzzle.");
    }
  }

  function ensureColor(doc, name, c, m, y, k) {
    var color;
    try {
      color = doc.colors.itemByName(name);
      if (!color.isValid) throw new Error("missing");
    } catch (_) {
      color = doc.colors.add({
        name: name,
        model: ColorModel.PROCESS,
        space: ColorSpace.CMYK,
        colorValue: [c, m, y, k]
      });
    }
    return color;
  }

  function createTextFrame(page, bounds, content, pointSize, leading, fontName, fillColor) {
    var frame = page.textFrames.add();
    frame.geometricBounds = bounds;
    frame.contents = content;
    var story = frame.parentStory;
    story.pointSize = pointSize;
    story.leading = leading;
    if (fillColor) {
      try { frame.fillColor = fillColor; } catch (_) {}
    }
    try {
      if (fontName) story.appliedFont = fontName;
    } catch (_) {}
    return frame;
  }

  function drawPiece(page, x, y, w, h, piece, colors) {
    var rect = page.rectangles.add();
    rect.geometricBounds = [y, x, y + h, x + w];
    rect.strokeColor = colors.pieceBorder;
    rect.strokeWeight = 0.5;
    rect.fillColor = colors.pieceFill;

    createTextFrame(
      page,
      [y + mm(1.3), x + mm(1.5), y + h - mm(1.8), x + w - mm(1.5)],
      safe(piece && piece.text, ""),
      10.5,
      12,
      "Arial Bold",
      null
    );

    createTextFrame(
      page,
      [y + h - mm(7), x + mm(1.5), y + h - mm(1.5), x + w - mm(1.5)],
      safe(piece && piece.role, "") + (piece && piece.points ? ("  " + piece.points + " pts") : ""),
      6.8,
      8,
      "Arial",
      null
    );
  }

  if (!ensureJson()) return;

  var jsonFile = chooseJsonFile();
  if (!jsonFile) {
    alert("Operation annulee.");
    return;
  }

  var pack;
  try {
    pack = parseJsonFile(jsonFile);
    validatePack(pack);
  } catch (e) {
    alert("Erreur: " + e);
    return;
  }

  var doc = app.documents.add();
  var dp = doc.documentPreferences;
  dp.pageWidth = "210mm";
  dp.pageHeight = "297mm";
  dp.pagesPerDocument = pack.puzzles.length;

  var colors = {
    pieceBorder: ensureColor(doc, "LongokaAnagramPieceBorder", 39, 13, 24, 26),
    pieceFill: ensureColor(doc, "LongokaAnagramPieceFill", 3, 2, 7, 0),
    slotBorder: ensureColor(doc, "LongokaAnagramSlotBorder", 28, 10, 14, 18),
    slotFill: ensureColor(doc, "LongokaAnagramSlotFill", 0, 0, 0, 0)
  };

  var marginTop = mm(18);
  var marginLeft = mm(16);
  var marginRight = mm(16);
  var marginBottom = mm(18);

  for (var i = 0; i < pack.puzzles.length; i++) {
    var puzzle = pack.puzzles[i];
    var challenges = puzzle.challenges || [];
    var page = doc.pages[i];
    var bounds = page.bounds;
    var top = bounds[0] + marginTop;
    var left = bounds[1] + marginLeft;
    var right = bounds[3] - marginRight;
    var bottom = bounds[2] - marginBottom;

    createTextFrame(
      page,
      [top, left, top + mm(11), right],
      safe(pack.title, "Anagrammes morphologiques") + " - " + safe(puzzle.title, safe(puzzle.id, "Puzzle " + (i + 1))),
      16,
      18,
      "Arial Bold",
      null
    );

    createTextFrame(
      page,
      [top + mm(12), left, top + mm(24), right],
      safe(puzzle.theme, "") + "\r" +
      "Relation: " + safe(puzzle.relationType, "") + " | " +
      "Defis: " + safe(puzzle.challengeCount, challenges.length) + " | " +
      "Score total: " + safe(puzzle.totalScore, "") + " | " +
      "Langue: " + safe(pack.language, ""),
      10.5,
      12.5,
      "Arial",
      null
    );

    var cursorY = top + mm(32);
    var rowGap = mm(9);
    var slotW = mm(20);
    var slotH = mm(11);
    var slotGap = mm(2.4);
    var pieceW = mm(24);
    var pieceH = mm(13);
    var pieceGap = mm(2.4);

    for (var c = 0; c < challenges.length; c++) {
      var challenge = challenges[c];
      if (cursorY > bottom - mm(42)) break;

      createTextFrame(
        page,
        [cursorY, left, cursorY + mm(7), right],
        (c + 1) + ". " + safe(challenge.translation, safe(challenge.display, safe(challenge.answer, ""))) +
        " | " + safe(challenge.label, "") + " | " + safe(challenge.score, 0) + " pts",
        9.6,
        11,
        "Arial Bold",
        null
      );

      var slotY = cursorY + mm(8);
      var solutionOrder = challenge.solutionOrder || [];
      for (var s = 0; s < solutionOrder.length; s++) {
        var slotX = left + (s * (slotW + slotGap));
        var slotRect = page.rectangles.add();
        slotRect.geometricBounds = [slotY, slotX, slotY + slotH, slotX + slotW];
        slotRect.strokeColor = colors.slotBorder;
        slotRect.strokeWeight = 0.45;
        slotRect.fillColor = colors.slotFill;
      }

      var pieces = challenge.pieces || [];
      var pieceY = slotY + mm(14);
      for (var p = 0; p < pieces.length; p++) {
        drawPiece(page, left + (p * (pieceW + pieceGap)), pieceY, pieceW, pieceH, pieces[p], colors);
      }

      var metaParts = [];
      if (challenge.translationEn) metaParts.push(challenge.translationEn);
      if (challenge.phonetic) metaParts.push(challenge.phonetic);
      if (challenge.entryRef && challenge.entryRef.extraInfo) metaParts.push(challenge.entryRef.extraInfo);
      if (metaParts.length) {
        createTextFrame(
          page,
          [pieceY + mm(15), left, pieceY + mm(23), right],
          metaParts.join(" - "),
          8.2,
          9.5,
          "Arial",
          null
        );
      }

      cursorY = pieceY + mm(25) + rowGap;
    }

    var notesTop = Math.min(cursorY + mm(2), bottom - mm(46));
    var notes = "Correction\r";
    for (var k = 0; k < challenges.length; k++) {
      var item = challenges[k];
      var pieceMap = {};
      var list = item.pieces || [];
      for (var m = 0; m < list.length; m++) {
        pieceMap[safe(list[m].id, "piece-" + (m + 1))] = list[m];
      }
      var ordered = [];
      var order = item.solutionOrder || [];
      for (var o = 0; o < order.length; o++) {
        var found = pieceMap[order[o]];
        if (found) ordered.push(safe(found.text, ""));
      }
      notes += (k + 1) + ". "
        + safe(item.display, safe(item.answer, ""))
        + " = "
        + ordered.join(" | ")
        + "\r";
    }

    createTextFrame(
      page,
      [notesTop, left, bottom, right],
      notes,
      8.8,
      10.2,
      "Arial",
      null
    );
  }

  alert("Import anagrammes morphologiques termine : " + pack.puzzles.length + " page(s) creee(s).");
})();
