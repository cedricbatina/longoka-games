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
    return File.openDialog("Choisir un pack JSON Longoka (memory match)", "*.json");
  }

  function parseJsonFile(file) {
    file.open("r");
    var content = file.read();
    file.close();
    return JSON.parse(content);
  }

  function validatePack(pack) {
    if (!pack || pack.format !== "longoka.memory-match.pack.v1") {
      throw new Error("Le fichier choisi n'est pas un pack memory-match Longoka valide.");
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

  function createCard(page, card, x, y, w, h, colors, order) {
    var rect = page.rectangles.add();
    rect.geometricBounds = [y, x, y + h, x + w];
    rect.strokeColor = colors.border;
    rect.strokeWeight = 0.6;
    rect.fillColor = card.kind === "translation" ? colors.translationFill : colors.formFill;

    var labelFrame = createTextFrame(
      page,
      [y + mm(3), x + mm(4), y + mm(12), x + w - mm(4)],
      safe(card.label, safe(card.kind, "Carte")),
      8.5,
      10,
      "Arial Bold",
      null
    );

    var valueFrame = createTextFrame(
      page,
      [y + mm(14), x + mm(4), y + h - mm(14), x + w - mm(4)],
      safe(card.display, safe(card.value, "")),
      13,
      15,
      "Arial Bold",
      null
    );

    var meta = [];
    if (card.hint) meta.push(card.hint);
    if (card.entryRef && card.entryRef.phonetic) meta.push(card.entryRef.phonetic);

    if (meta.length) {
      createTextFrame(
        page,
        [y + h - mm(14), x + mm(4), y + h - mm(4), x + w - mm(4)],
        meta.join(" - "),
        8.2,
        9.6,
        "Arial",
        null
      );
    }

    var badge = createTextFrame(
      page,
      [y - mm(2), x + mm(3), y + mm(7), x + mm(20)],
      safe(order, ""),
      8.4,
      9.4,
      "Arial Bold",
      colors.badgeFill
    );

    try {
      badge.textFramePreferences.verticalJustification = VerticalJustification.CENTER_ALIGN;
      badge.texts[0].justification = Justification.CENTER_ALIGN;
      badge.fillTint = 100;
      badge.strokeColor = colors.badgeFill;
    } catch (_) {}

    return [rect, labelFrame, valueFrame, badge];
  }

  function buildPairs(cards) {
    var map = {};
    var order = [];
    var i;

    for (i = 0; i < cards.length; i++) {
      var card = cards[i];
      var pairId = safe(card && card.pairId, "");
      if (!pairId) continue;
      if (!map[pairId]) {
        map[pairId] = {
          pairId: pairId,
          formCard: null,
          translationCard: null
        };
        order.push(pairId);
      }
      if (safe(card.kind, "") === "translation") {
        map[pairId].translationCard = card;
      } else {
        map[pairId].formCard = card;
      }
    }

    var pairs = [];
    for (i = 0; i < order.length; i++) {
      pairs.push(map[order[i]]);
    }
    return pairs;
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
    border: ensureColor(doc, "LongokaMemoryBorder", 42, 10, 25, 48),
    formFill: ensureColor(doc, "LongokaMemoryForm", 3, 4, 8, 0),
    translationFill: ensureColor(doc, "LongokaMemoryTranslation", 9, 2, 8, 0),
    badgeFill: ensureColor(doc, "LongokaMemoryBadge", 82, 28, 53, 16)
  };

  var marginTop = mm(18);
  var marginLeft = mm(16);
  var marginRight = mm(16);
  var marginBottom = mm(18);

  for (var i = 0; i < pack.puzzles.length; i++) {
    var puzzle = pack.puzzles[i];
    var cards = puzzle.cards || [];
    var pairs = buildPairs(cards);
    var page = doc.pages[i];
    var bounds = page.bounds;
    var top = bounds[0] + marginTop;
    var left = bounds[1] + marginLeft;
    var right = bounds[3] - marginRight;
    var bottom = bounds[2] - marginBottom;

    createTextFrame(
      page,
      [top, left, top + mm(11), right],
      safe(pack.title, "Memory match") + " - " + safe(puzzle.title, safe(puzzle.id, "Puzzle " + (i + 1))),
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
      "Paires: " + safe(puzzle.pairCount, pairs.length) + " | " +
      "Langue: " + safe(pack.language, ""),
      10.5,
      12.5,
      "Arial",
      null
    );

    var boardTop = top + mm(30);
    var boardBottom = top + mm(186);
    var boardHeight = boardBottom - boardTop;
    var gap = mm(5);
    var cols = Number(puzzle.cols || 0) || (cards.length >= 24 ? 6 : 4);
    var rows = Number(puzzle.rows || 0) || Math.max(1, Math.ceil(cards.length / cols));
    var cardW = (right - left - ((cols - 1) * gap)) / cols;
    var cardH = Math.min(mm(34), (boardHeight - ((rows - 1) * gap)) / rows);

    for (var c = 0; c < cards.length; c++) {
      var row = Math.floor(c / cols);
      var col = c % cols;
      var x = left + (col * (cardW + gap));
      var y = boardTop + (row * (cardH + gap));
      createCard(page, cards[c], x, y, cardW, cardH, colors, c + 1);
    }

    var notesTop = boardBottom + mm(4);
    var notes = "Paires de correction\r";
    for (var p = 0; p < pairs.length; p++) {
      var pair = pairs[p];
      notes += (p + 1) + ". "
        + safe(pair.formCard && pair.formCard.display, "")
        + " <-> "
        + safe(pair.translationCard && pair.translationCard.display, "")
        + "\r";
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

  alert("Import memory-match termine : " + pack.puzzles.length + " puzzle(s) importes.");
})();
