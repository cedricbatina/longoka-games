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
    return File.openDialog("Choisir un pack JSON Longoka (scrabble-like)", "*.json");
  }

  function parseJsonFile(file) {
    file.open("r");
    var content = file.read();
    file.close();
    return JSON.parse(content);
  }

  function validatePack(pack) {
    if (!pack || pack.format !== "longoka.scrabble-like.pack.v1") {
      throw new Error("Le fichier choisi n'est pas un pack scrabble-like Longoka valide.");
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

  function drawSlot(page, x, y, size, colors, letter) {
    var rect = page.rectangles.add();
    rect.geometricBounds = [y, x, y + size, x + size];
    rect.strokeColor = colors.slotBorder;
    rect.strokeWeight = 0.45;
    rect.fillColor = colors.slotFill;
    if (letter) {
      createTextFrame(
        page,
        [y + mm(1.5), x, y + size - mm(1.5), x + size],
        safe(letter, ""),
        11,
        13,
        "Arial Bold",
        null
      );
    }
    return rect;
  }

  function drawTile(page, x, y, w, h, tile, colors) {
    var rect = page.rectangles.add();
    rect.geometricBounds = [y, x, y + h, x + w];
    rect.strokeColor = colors.tileBorder;
    rect.strokeWeight = 0.5;
    rect.fillColor = colors.tileFill;

    createTextFrame(
      page,
      [y + mm(1.5), x + mm(1.5), y + h - mm(1.5), x + w - mm(1.5)],
      safe(tile && tile.letter, ""),
      13,
      15,
      "Arial Bold",
      null
    );

    createTextFrame(
      page,
      [y + h - mm(7), x + w - mm(10), y + h - mm(1), x + w - mm(1)],
      safe(tile && tile.points, ""),
      7,
      8,
      "Arial Bold",
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
    tileBorder: ensureColor(doc, "LongokaScrabbleTileBorder", 46, 17, 28, 42),
    tileFill: ensureColor(doc, "LongokaScrabbleTileFill", 2, 4, 10, 0),
    slotBorder: ensureColor(doc, "LongokaScrabbleSlotBorder", 28, 10, 14, 18),
    slotFill: ensureColor(doc, "LongokaScrabbleSlotFill", 0, 0, 0, 0),
    badgeFill: ensureColor(doc, "LongokaScrabbleBadge", 82, 28, 53, 16)
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
      safe(pack.title, "Scrabble-like") + " - " + safe(puzzle.title, safe(puzzle.id, "Puzzle " + (i + 1))),
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
    var rowGap = mm(10);
    var slotSize = mm(8.5);
    var slotGap = mm(2.2);
    var tileW = mm(10.5);
    var tileH = mm(11.5);
    var tileGap = mm(2.4);

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
      var normalized = safe(challenge.normalized, "");
      for (var s = 0; s < normalized.length; s++) {
        drawSlot(page, left + (s * (slotSize + slotGap)), slotY, slotSize, colors, "");
      }

      var tileY = slotY + mm(11);
      var rack = challenge.rack || [];
      for (var t = 0; t < rack.length; t++) {
        drawTile(page, left + (t * (tileW + tileGap)), tileY, tileW, tileH, rack[t], colors);
      }

      var metaParts = [];
      if (challenge.translationEn) metaParts.push(challenge.translationEn);
      if (challenge.phonetic) metaParts.push(challenge.phonetic);
      if (challenge.entryRef && challenge.entryRef.extraInfo) metaParts.push(challenge.entryRef.extraInfo);
      if (metaParts.length) {
        createTextFrame(
          page,
          [tileY + mm(13), left, tileY + mm(21), right],
          metaParts.join(" - "),
          8.2,
          9.5,
          "Arial",
          null
        );
      }

      cursorY = tileY + mm(24) + rowGap;
    }

    var notesTop = Math.min(cursorY + mm(2), bottom - mm(46));
    var notes = "Correction\r";
    for (var p = 0; p < challenges.length; p++) {
      var item = challenges[p];
      notes += (p + 1) + ". "
        + safe(item.display, safe(item.answer, ""))
        + " = "
        + safe(item.translation, "")
        + " ("
        + safe(item.score, 0)
        + " pts)\r";
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

  alert("Import scrabble-like termine : " + pack.puzzles.length + " page(s) creee(s).");
})();
