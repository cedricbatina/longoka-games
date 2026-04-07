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
    return File.openDialog("Choisir un pack JSON Longoka (dominos morphologiques)", "*.json");
  }

  function parseJsonFile(file) {
    file.open("r");
    var content = file.read();
    file.close();
    return JSON.parse(content);
  }

  function validatePack(pack) {
    if (!pack || pack.format !== "longoka.morpho-domino.pack.v1") {
      throw new Error("Le fichier choisi n'est pas un pack domino Longoka valide.");
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

  function tileById(puzzle, id) {
    var tiles = puzzle.tiles || [];
    for (var i = 0; i < tiles.length; i++) {
      if (safe(tiles[i].id) === safe(id)) return tiles[i];
    }
    return null;
  }

  function orderedTiles(puzzle) {
    var order = puzzle.solutionOrder || [];
    var out = [];
    var used = {};
    var i;

    for (i = 0; i < order.length; i++) {
      var tile = tileById(puzzle, order[i]);
      if (tile) {
        tile._dominoOrder = i + 1;
        out.push(tile);
        used[safe(tile.id)] = true;
      }
    }

    var tiles = puzzle.tiles || [];
    for (i = 0; i < tiles.length; i++) {
      if (!used[safe(tiles[i].id)]) {
        tiles[i]._dominoOrder = out.length + 1;
        out.push(tiles[i]);
      }
    }

    return out;
  }

  function tileNote(tile) {
    var parts = [];
    var display = safe(tile && tile.meta && tile.meta.display, safe(tile && tile.entryRef && tile.entryRef.singular, ""));
    var translation = safe(tile && tile.entryRef && tile.entryRef.translation, safe(tile && tile.meta && tile.meta.translation, ""));
    var phonetic = safe(tile && tile.entryRef && tile.entryRef.phonetic, "");

    if (display) parts.push(display);
    if (translation) parts.push(translation);
    if (phonetic) parts.push(phonetic);

    return parts.join(" - ");
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

  function createDominoTile(page, doc, tile, x, y, w, h, colors) {
    var pinW = mm(4.5);
    var halfW = (w - pinW) / 2;
    var leftRect = page.rectangles.add();
    leftRect.geometricBounds = [y, x, y + h, x + halfW];
    leftRect.strokeColor = colors.border;
    leftRect.strokeWeight = 0.5;
    leftRect.fillColor = colors.leftFill;

    var pinRect = page.rectangles.add();
    pinRect.geometricBounds = [y, x + halfW, y + h, x + halfW + pinW];
    pinRect.strokeColor = colors.border;
    pinRect.strokeWeight = 0.5;
    pinRect.fillColor = colors.pinFill;

    var rightRect = page.rectangles.add();
    rightRect.geometricBounds = [y, x + halfW + pinW, y + h, x + w];
    rightRect.strokeColor = colors.border;
    rightRect.strokeWeight = 0.5;
    rightRect.fillColor = colors.rightFill;

    var leftContent = safe(tile.left && tile.left.label, "") + "\r" + safe(tile.left && tile.left.display, safe(tile.left && tile.left.value, ""));
    var rightContent = safe(tile.right && tile.right.label, "") + "\r" + safe(tile.right && tile.right.display, safe(tile.right && tile.right.value, ""));

    var leftFrame = createTextFrame(
      page,
      [y + mm(4), x + mm(4), y + h - mm(4), x + halfW - mm(3)],
      leftContent,
      10.5,
      12,
      "Arial",
      colors.leftFill
    );
    var rightFrame = createTextFrame(
      page,
      [y + mm(4), x + halfW + pinW + mm(3), y + h - mm(4), x + w - mm(4)],
      rightContent,
      10.5,
      12,
      "Arial",
      colors.rightFill
    );

    try {
      leftFrame.parentStory.paragraphs[0].pointSize = 8;
      leftFrame.parentStory.paragraphs[0].capitalization = Capitalization.SMALL_CAPS;
      rightFrame.parentStory.paragraphs[0].pointSize = 8;
      rightFrame.parentStory.paragraphs[0].capitalization = Capitalization.SMALL_CAPS;
    } catch (_) {}

    var orderFrame = createTextFrame(
      page,
      [y - mm(2), x + mm(2), y + mm(8), x + mm(16)],
      safe(tile._dominoOrder, ""),
      9,
      10,
      "Arial Bold",
      colors.badgeFill
    );
    try {
      orderFrame.textFramePreferences.verticalJustification = VerticalJustification.CENTER_ALIGN;
      orderFrame.texts[0].justification = Justification.CENTER_ALIGN;
      orderFrame.fillTint = 100;
      orderFrame.strokeColor = colors.badgeFill;
    } catch (_) {}

    return [leftRect, pinRect, rightRect, leftFrame, rightFrame, orderFrame];
  }

  if (!ensureJson()) return;

  var jsonFile = chooseJsonFile();
  if (!jsonFile) {
    alert("Opération annulée.");
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
    border: ensureColor(doc, "LongokaDominoBorder", 45, 10, 25, 45),
    leftFill: ensureColor(doc, "LongokaDominoLeft", 3, 4, 10, 0),
    rightFill: ensureColor(doc, "LongokaDominoRight", 8, 2, 9, 0),
    pinFill: ensureColor(doc, "LongokaDominoPin", 9, 4, 8, 12),
    badgeFill: ensureColor(doc, "LongokaDominoBadge", 82, 28, 53, 16)
  };

  var marginTop = mm(18);
  var marginLeft = mm(16);
  var marginRight = mm(16);
  var marginBottom = mm(18);

  for (var i = 0; i < pack.puzzles.length; i++) {
    var puzzle = pack.puzzles[i];
    var page = doc.pages[i];
    var bounds = page.bounds;
    var top = bounds[0] + marginTop;
    var left = bounds[1] + marginLeft;
    var right = bounds[3] - marginRight;
    var bottom = bounds[2] - marginBottom;

    var ordered = orderedTiles(puzzle);

    createTextFrame(
      page,
      [top, left, top + mm(11), right],
      safe(pack.title, "Dominos morphologiques") + " - " + safe(puzzle.title, safe(puzzle.id, "Puzzle " + (i + 1))),
      16,
      18,
      "Arial Bold",
      null
    );

    createTextFrame(
      page,
      [top + mm(12), left, top + mm(24), right],
      safe(puzzle.theme, "") + "\r" +
      "Relation: " + safe(puzzle.relationType, safe(pack.meta && pack.meta.relationType, "")) + " | " +
      "Tuiles: " + ordered.length + " | " +
      "Langue: " + safe(pack.language, ""),
      10.5,
      12.5,
      "Arial",
      null
    );

    var boardTop = top + mm(30);
    var boardBottom = top + mm(185);
    var boardHeight = boardBottom - boardTop;
    var gap = mm(6);
    var cols = ordered.length > 10 ? 3 : 2;
    var rows = Math.max(1, Math.ceil(ordered.length / cols));
    var tileW = (right - left - ((cols - 1) * gap)) / cols;
    var tileH = Math.min(mm(34), (boardHeight - ((rows - 1) * gap)) / rows);

    for (var t = 0; t < ordered.length; t++) {
      var row = Math.floor(t / cols);
      var col = t % cols;
      var x = left + (col * (tileW + gap));
      var y = boardTop + (row * (tileH + gap));
      createDominoTile(page, doc, ordered[t], x, y, tileW, tileH, colors);
    }

    var notesTop = boardBottom + mm(4);
    var notes = "Références\r";
    for (var n = 0; n < ordered.length; n++) {
      notes += safe(ordered[n]._dominoOrder, (n + 1)) + ". " + tileNote(ordered[n]) + "\r";
    }

    createTextFrame(
      page,
      [notesTop, left, bottom, right],
      notes,
      9.2,
      11.2,
      "Arial",
      null
    );
  }

  alert("Import domino terminé : " + pack.puzzles.length + " puzzle(s) importé(s).");
})();
