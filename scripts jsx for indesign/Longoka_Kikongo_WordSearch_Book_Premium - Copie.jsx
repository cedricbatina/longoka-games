#target "InDesign"

/*
Longoka Games — Kikongo Wordsearch (Premium Book Builder)
--------------------------------------------------------
- Input: longoka.wordsearch.pack.v1 JSON
- Output: An InDesign document with:
  * Intro pages (title, how-to, reading tips, nominal classes)
  * 1 puzzle per page (premium layout + table grid)
  * Solutions section at the end (multiple solutions per page) using placements highlighting
*/

(function () {
  // =========================
  // Settings (edit if needed)
  // =========================
  var MAX_PUZZLES = 48;            // default book size
  var SOLUTIONS_PER_PAGE = 4;      // 2 or 4 recommended
  var MAKE_SOLUTIONS = true;

  // Page geometry
  var PAGE_W_MM = 210;
  var PAGE_H_MM = 297;
  var M_TOP_MM = 18;
  var M_LEFT_MM = 18;
  var M_RIGHT_MM = 18;
  var M_BOTTOM_MM = 18;

  // Grid sizing
  var GRID_MM = 120;              // square grid size on puzzle page
  var GRID_TEXT_PT = 12;          // letter size inside grid
  var GRID_STROKE_PT = 0.35;

  // Solutions sizing (4-up default)
  var SOL_GRID_MM = 75;           // square per solution when 4-up
  var SOL_GRID_TEXT_PT = 7.5;

  // Typography
  var FONT_REG = "Minion Pro\tRegular";  // safe default if available
  var FONT_BOLD = "Minion Pro\tBold";
  var FONT_SANS = "Myriad Pro\tRegular";
  var FONT_SANS_BOLD = "Myriad Pro\tBold";

  // Branding
  var SERIES = "Longoka Games";
  var LANG_LABEL = "Kikongo";
  var BOOK_KIND = "Mots mêlés";
  var BOOK_SUBTITLE = "Noms — Volume 1";

  // Kikongo nominal classes (DB-aligned: 9 + 2 sous-classes)
  var KIKONGO_CLASSES = [
    "n-zi",
    "mu-ba",
    "mu-mi",
    "di-ma",
    "ki-bi",
    "bu-ma",
    "lu-tu",
    "lu-zi",
    "lu-ma",
    "ku-ma",
    "fi-bi"
  ];

  // =========================
  // Helpers
  // =========================
  function mm(n) { return n * 2.834645; } // 1mm ≈ 2.834645 pt

  function safeFont(name, fallback) {
    try { app.fonts.itemByName(name).name; return name; }
    catch (e) { return fallback; }
  }

  function getOrCreateColor(doc, name, c, m, y, k) {
    try { var _t = doc.colors.itemByName(name); _t.name; return _t; } catch (_) {}
    try {
      var col = doc.colors.add();
      col.name = name;
      col.space = ColorSpace.CMYK;
      col.model = ColorModel.PROCESS;
      col.colorValue = [c, m, y, k];
      return col;
    } catch (e) {
      return doc.swatches.itemByName("Black");
    }
  }

  function getOrCreateParaStyle(doc, name, props) {
    var ps;
    try { ps = doc.paragraphStyles.itemByName(name); ps.name; }
    catch (e) { ps = doc.paragraphStyles.add({ name: name }); }
    try { for (var k in props) ps.properties[k] = props[k]; } catch (_) {}
    return ps;
  }

  function getOrCreateCharacterStyle(doc, name, props) {
    var cs;
    try { cs = doc.characterStyles.itemByName(name); cs.name; }
    catch (e) { cs = doc.characterStyles.add({ name: name }); }
    try { for (var k in props) if (props[k] !== undefined) cs[k] = props[k]; } catch (_) {}
    return cs;
  }

  function setMargins(doc) {
    for (var i = 0; i < doc.pages.length; i++) {
      var mp = doc.pages[i].marginPreferences;
      mp.top = mm(M_TOP_MM);
      mp.left = mm(M_LEFT_MM);
      mp.right = mm(M_RIGHT_MM);
      mp.bottom = mm(M_BOTTOM_MM);
    }
  }

  function readJsonFromDialog() {
    var jsonFile = File.openDialog("Choisir un pack JSON Longoka (wordsearch)", "*.json");
    if (!jsonFile) return null;

    jsonFile.open("r");
    var jsonStr = jsonFile.read();
    jsonFile.close();

    var data;
    try {
      if (typeof JSON !== "undefined" && JSON.parse) data = JSON.parse(jsonStr);
      else data = eval("(" + jsonStr + ")");
    } catch (e) {
      alert("Erreur de parsing JSON : " + e);
      return null;
    }
    if (!data || !data.puzzles || !data.puzzles.length) {
      alert("Pack JSON invalide ou vide.");
      return null;
    }
    return data;
  }

  function pageContentBounds(page) {
    var b = page.bounds; // [y1,x1,y2,x2]
    return {
      top: b[0] + mm(M_TOP_MM),
      left: b[1] + mm(M_LEFT_MM),
      bottom: b[2] - mm(M_BOTTOM_MM),
      right: b[3] - mm(M_RIGHT_MM)
    };
  }

  function addTextFrame(page, gb, contents, paraStyle) {
    var tf = page.textFrames.add();
    tf.geometricBounds = gb;
    tf.contents = contents || "";
    if (paraStyle) { try { tf.parentStory.appliedParagraphStyle = paraStyle; } catch (_) {} }
    return tf;
  }

  function appendPara(story, text, paraStyle) {
    story.insertionPoints[-1].contents = text + "\r";
    try { story.paragraphs[-1].appliedParagraphStyle = paraStyle; } catch (_) {}
  }

  function normalizeGridLines(puzzle) {
    var lines = [];
    if (puzzle.grid && puzzle.grid.length) {
      for (var i = 0; i < puzzle.grid.length; i++) {
        var s = puzzle.grid[i];
        if (typeof s === "string") lines.push(s.replace(/\s+/g, ""));
      }
    }
    return lines;
  }

  /** Table Cell : ne pas assigner strokeWeight (ID 21+ → erreur 55). Uniquement les bords. */
  function setTableCellStrokeUniform(cell, strokePt) {
    try {
      cell.topEdgeStrokeWeight = strokePt;
      cell.leftEdgeStrokeWeight = strokePt;
      cell.bottomEdgeStrokeWeight = strokePt;
      cell.rightEdgeStrokeWeight = strokePt;
    } catch (e) {}
  }

  function createGridTable(page, gb, rows, cols, gridLines, opts) {
    opts = opts || {};
    var cellSizePt = opts.cellSizePt;
    var fontName = opts.fontName;
    var textPt = opts.textPt;
    var strokePt = opts.strokePt;
    var gridCharStyle = opts.gridCharStyle;

    var tf = page.textFrames.add();
    tf.geometricBounds = gb;
    tf.contents = "";
    var table = tf.insertionPoints[0].tables.add({ bodyRowCount: rows, columnCount: cols });

    for (var c = 0; c < cols; c++) table.columns[c].width = cellSizePt;
    for (var r = 0; r < rows; r++) table.rows[r].height = cellSizePt;

    for (var rr = 0; rr < rows; rr++) {
      for (var cc = 0; cc < cols; cc++) {
        var cell = table.rows[rr].cells[cc];
        setTableCellStrokeUniform(cell, strokePt);
        cell.verticalJustification = VerticalJustification.CENTER_ALIGN;
        var ch = (gridLines && gridLines[rr] && gridLines[rr].length > cc) ? gridLines[rr].charAt(cc) : "";
        cell.contents = ch;
        try {
          if (gridCharStyle) {
            cell.texts[0].appliedCharacterStyle = gridCharStyle;
          } else {
            try { cell.texts[0].appliedFont = fontName; } catch (_) {}
          }
          cell.texts[0].pointSize = textPt;
          cell.texts[0].leading = textPt + 1;
          cell.texts[0].justification = Justification.CENTER_ALIGN;
        } catch (_) {}
      }
    }
    return { textFrame: tf, table: table };
  }

  function directionDelta(dir) {
    switch (dir) {
      case "RIGHT": return { dr: 0, dc: 1 };
      case "LEFT": return { dr: 0, dc: -1 };
      case "DOWN": return { dr: 1, dc: 0 };
      case "UP": return { dr: -1, dc: 0 };
      case "DIAGONAL_DOWN_RIGHT": return { dr: 1, dc: 1 };
      case "DIAGONAL_DOWN_LEFT": return { dr: 1, dc: -1 };
      case "DIAGONAL_UP_RIGHT": return { dr: -1, dc: 1 };
      case "DIAGONAL_UP_LEFT": return { dr: -1, dc: -1 };
      default: return null;
    }
  }

  function highlightSolution(table, placements, highlightColor) {
    if (!placements || !placements.length) return;

    for (var i = 0; i < placements.length; i++) {
      var p = placements[i];
      var d = directionDelta(p.direction);
      if (!d) continue;

      var rr = p.row, cc = p.col;
      for (var k = 0; k < p.length; k++) {
        if (rr >= 0 && rr < table.bodyRowCount && cc >= 0 && cc < table.columnCount) {
          try {
            table.rows[rr].cells[cc].fillColor = highlightColor;
            table.rows[rr].cells[cc].fillTint = 35;
          } catch (_) {}
        }
        rr += d.dr;
        cc += d.dc;
      }
    }
  }

  function sortedEntries(entries) {
    if (!entries || !entries.length) return [];
    var a = [];
    for (var i = 0; i < entries.length; i++) a.push(entries[i]);
    a.sort(function (x, y) {
      var ax = (x.display || x.base || "").toLowerCase();
      var ay = (y.display || y.base || "").toLowerCase();
      return ax < ay ? -1 : (ax > ay ? 1 : 0);
    });
    return a;
  }

  // =========================
  // Build document
  // =========================
  var pack = readJsonFromDialog();
  if (!pack) return;

  var puzzleCount = pack.puzzles.length;
  if (puzzleCount > MAX_PUZZLES) puzzleCount = MAX_PUZZLES;

  FONT_REG = safeFont(FONT_REG, "Times New Roman\tRegular");
  FONT_BOLD = safeFont(FONT_BOLD, "Times New Roman\tBold");
  FONT_SANS = safeFont(FONT_SANS, "Arial\tRegular");
  FONT_SANS_BOLD = safeFont(FONT_SANS_BOLD, "Arial\tBold");

  var doc = app.documents.add();
  var dp = doc.documentPreferences;
  dp.pageWidth = PAGE_W_MM + "mm";
  dp.pageHeight = PAGE_H_MM + "mm";
  dp.facingPages = true;
  dp.pagesPerDocument = 1;

  // Colors
  var colGray = getOrCreateColor(doc, "LongokaGray", 0, 0, 0, 70);
  var colHighlight = getOrCreateColor(doc, "SolutionHighlight", 0, 0, 0, 20);

  // Styles
  var psTitle = getOrCreateParaStyle(doc, "LG_Title", { appliedFont: FONT_SANS_BOLD, pointSize: 28, leading: 30, justification: Justification.CENTER_ALIGN });
  var psSub = getOrCreateParaStyle(doc, "LG_Subtitle", { appliedFont: FONT_SANS, pointSize: 14, leading: 18, justification: Justification.CENTER_ALIGN });
  var psH1 = getOrCreateParaStyle(doc, "LG_H1", { appliedFont: FONT_SANS_BOLD, pointSize: 20, leading: 24, spaceAfter: 6 });
  var psBody = getOrCreateParaStyle(doc, "LG_Body", { appliedFont: FONT_REG, pointSize: 11, leading: 14 });
  var psSmall = getOrCreateParaStyle(doc, "LG_Small", { appliedFont: FONT_REG, pointSize: 9.5, leading: 12 });
  var psPuzzleHdr = getOrCreateParaStyle(doc, "LG_PuzzleHeader", { appliedFont: FONT_SANS_BOLD, pointSize: 16, leading: 18, spaceAfter: 4 });
  var psMeta = getOrCreateParaStyle(doc, "LG_Meta", { appliedFont: FONT_SANS, pointSize: 9.5, leading: 12, fillColor: colGray });
  var psWordTerm = getOrCreateParaStyle(doc, "LG_WordTerm", { appliedFont: FONT_SANS_BOLD, pointSize: 10.5, leading: 12 });
  var psWordDef = getOrCreateParaStyle(doc, "LG_WordDef", { appliedFont: FONT_REG, pointSize: 9.8, leading: 12, spaceAfter: 4 });
  var psSolHdr = getOrCreateParaStyle(doc, "LG_SolutionHeader", { appliedFont: FONT_SANS_BOLD, pointSize: 10.5, leading: 12 });

  var csGridLetter = getOrCreateCharacterStyle(doc, "LG_GridLetter", { appliedFont: FONT_SANS_BOLD });

  function newPage() { return doc.pages.add(LocationOptions.AT_END); }
  setMargins(doc);

  // =========================
  // Intro pages
  // =========================
  function buildTitlePage(page) {
    var cb = pageContentBounds(page);
    var centerY = cb.top + (cb.bottom - cb.top) * 0.35;

    addTextFrame(page, [centerY - mm(30), cb.left, centerY, cb.right], SERIES, psTitle);
    addTextFrame(page, [centerY, cb.left, centerY + mm(14), cb.right], LANG_LABEL + " — " + BOOK_KIND, psSub);
    addTextFrame(page, [centerY + mm(16), cb.left, centerY + mm(28), cb.right], BOOK_SUBTITLE, psSub);
    addTextFrame(page, [cb.bottom - mm(30), cb.left, cb.bottom, cb.right], "Généré automatiquement depuis Lexikongo • Longoka Games", psSmall);
  }

  function buildHowToPage(page) {
    var cb = pageContentBounds(page);
    var tf = addTextFrame(page, [cb.top, cb.left, cb.bottom, cb.right], "", psBody);
    var st = tf.parentStory;

    appendPara(st, "Comment jouer", psH1);
    appendPara(st, "1) Cherche les mots de la liste dans la grille. Ils peuvent être horizontaux, verticaux ou en diagonale.", psBody);
    appendPara(st, "2) Entoure ou surligne les lettres une fois le mot trouvé.", psBody);
    appendPara(st, "3) Les mots peuvent apparaître dans les deux sens (selon la grille).", psBody);
    appendPara(st, "4) À la fin du livre, tu trouveras les solutions (mots surlignés).", psBody);

    appendPara(st, "Conseils rapides", psH1);
    appendPara(st, "• Balaye la grille ligne par ligne : repère d’abord les lettres rares (K, V, Z…).", psBody);
    appendPara(st, "• Lis en syllabes simples, sans “e muet”.", psBody);
    appendPara(st, "• La classe nominale (ex. ki-bi) te donne un repère de mémorisation.", psBody);
  }

  function buildReadingTipsPage(page) {
    var cb = pageContentBounds(page);
    var tf = addTextFrame(page, [cb.top, cb.left, cb.bottom, cb.right], "", psBody);
    var st = tf.parentStory;

    appendPara(st, "Lire le kikongo (mini-fiche)", psH1);
    appendPara(st, "• On lit ce qu’on écrit : toutes les lettres se prononcent.", psBody);
    appendPara(st, "• Les mots se forment souvent en syllabes ouvertes.", psBody);
    appendPara(st, "• Le ton peut changer le sens : écoute et répète.", psBody);

    appendPara(st, "Groupes nasaux (mémo)", psH1);
    appendPara(st, "• mb, nd, ng… se prononcent d’un bloc (consonne + nasalisation).", psBody);
  }

  function buildNominalClassesPage(page) {
    var cb = pageContentBounds(page);
    addTextFrame(page, [cb.top, cb.left, cb.top + mm(10), cb.right], "Classes nominales (Kikongo) — Référence (DB)", psH1);

    var tableTop = cb.top + mm(16);
    var tf = page.textFrames.add();
    tf.geometricBounds = [tableTop, cb.left, tableTop + mm(170), cb.right];
    tf.contents = "";

    var rows = KIKONGO_CLASSES.length + 1;
    var cols = 2;
    var t = tf.insertionPoints[0].tables.add({ bodyRowCount: rows, columnCount: cols });

    t.rows[0].cells[0].contents = "Classe";
    t.rows[0].cells[1].contents = "Mémo";
    try {
      t.rows[0].cells[0].texts[0].appliedFont = FONT_SANS_BOLD;
      t.rows[0].cells[1].texts[0].appliedFont = FONT_SANS_BOLD;
    } catch (_) {}
    t.rows[0].height = mm(8);

    var tableW = cb.right - cb.left;
    t.columns[0].width = tableW * 0.25;
    t.columns[1].width = tableW * 0.75;

    for (var r = 1; r < rows; r++) {
      var cls = KIKONGO_CLASSES[r - 1];
      t.rows[r].cells[0].contents = cls;
      t.rows[r].cells[1].contents = "Préfixes singulier/pluriel : repère le préfixe pour mémoriser.";
      t.rows[r].height = mm(8);
      try {
        t.rows[r].cells[0].texts[0].appliedFont = FONT_SANS_BOLD;
        t.rows[r].cells[0].texts[0].pointSize = 10.5;
        t.rows[r].cells[1].texts[0].pointSize = 10;
      } catch (_) {}
      t.rows[r].cells[0].verticalJustification = VerticalJustification.CENTER_ALIGN;
      t.rows[r].cells[1].verticalJustification = VerticalJustification.CENTER_ALIGN;
    }

    addTextFrame(page, [cb.bottom - mm(30), cb.left, cb.bottom, cb.right],
      "Note : dans nos packs Longoka, la classe nominale vient de la base Lexikongo et apparaît à côté des mots.",
      psSmall
    );
  }

  var intro1 = doc.pages[0]; buildTitlePage(intro1);
  var intro2 = newPage(); setMargins(doc); buildHowToPage(intro2);
  var intro3 = newPage(); setMargins(doc); buildReadingTipsPage(intro3);
  var intro4 = newPage(); setMargins(doc); buildNominalClassesPage(intro4);

  // =========================
  // Puzzle pages
  // =========================
  function buildPuzzlePage(page, puzzle, idx1based) {
    var cb = pageContentBounds(page);

    var headerH = mm(12);
    var hdr = addTextFrame(page, [cb.top, cb.left, cb.top + headerH, cb.right], "", psPuzzleHdr);
    hdr.contents = LANG_LABEL + " — " + BOOK_KIND + " • Grille " + (idx1based < 10 ? "0" + idx1based : idx1based);

    var metaY = cb.top + headerH;
    var metaH = mm(6);
    var meta = addTextFrame(page, [metaY, cb.left, metaY + metaH, cb.right], "", psMeta);

    var domains = (puzzle.meta && puzzle.meta.semanticDomains && puzzle.meta.semanticDomains.length) ? puzzle.meta.semanticDomains.join(" • ") : (puzzle.theme || "");
    var classes = (puzzle.meta && puzzle.meta.nominalClasses && puzzle.meta.nominalClasses.length) ? ("Classes : " + puzzle.meta.nominalClasses.join(", ")) : "";
    meta.contents = (domains ? domains : "") + (domains && classes ? "    |    " : "") + (classes ? classes : "");

    var gridTop = metaY + metaH + mm(6);

    var lines = normalizeGridLines(puzzle);
    var rows = Math.max(1, puzzle.rows || (lines && lines.length ? lines.length : 12));
    var cols = Math.max(1, puzzle.cols || (lines && lines[0] ? lines[0].length : 12));

    var availW = cb.right - cb.left;
    var footerH = mm(8);
    var listReserve = mm(28);
    var maxGridBottom = cb.bottom - footerH - listReserve;
    var availH = maxGridBottom - gridTop;
    if (availH < mm(30)) {
      availH = mm(30);
    }

    var cellByW = availW / cols;
    var cellByH = availH / rows;
    var cellSize = cellByW < cellByH ? cellByW : cellByH;

    var capW = mm(GRID_MM) / cols;
    var capH = mm(GRID_MM) / rows;
    var capPreset = capW < capH ? capW : capH;
    if (cellSize > capPreset) {
      cellSize = capPreset;
    }

    var tableW = cellSize * cols;
    var tableH = cellSize * rows;
    var gridLeft = cb.left + (availW - tableW) / 2;
    var gridGb = [gridTop, gridLeft, gridTop + tableH, gridLeft + tableW];

    var listTop = gridGb[2] + mm(8);
    var listGb = [listTop, cb.left, cb.bottom - mm(10), cb.right];

    var sugText = cellSize * 0.52;
    var textPt = GRID_TEXT_PT;
    if (sugText < 6) {
      textPt = 6;
    } else if (sugText < GRID_TEXT_PT) {
      textPt = sugText;
    }

    createGridTable(page, gridGb, rows, cols, lines, {
      cellSizePt: cellSize,
      fontName: FONT_SANS_BOLD,
      textPt: textPt,
      strokePt: GRID_STROKE_PT,
      gridCharStyle: csGridLetter
    });

    var tf = addTextFrame(page, listGb, "", psBody);
    tf.textFramePreferences.textColumnCount = 2;
    tf.textFramePreferences.textColumnGutter = mm(6);

    var st = tf.parentStory;
    appendPara(st, "Mots à trouver", psH1);

    var entries = sortedEntries(puzzle.entries || []);
    for (var i = 0; i < entries.length; i++) {
      var e = entries[i] || {};
      var display = e.display || e.base || "";
      var cls = e.extraInfo ? (" (" + e.extraInfo + ")") : "";
      appendPara(st, display + cls, psWordTerm);

      var fr = e.translation || "";
      var en = e.translationEn ? ("EN : " + e.translationEn) : "";
      var ph = e.phonetic ? ("Phon. : " + e.phonetic) : "";
      var line = fr;
      if (en) line += (line ? " • " : "") + en;
      if (ph) line += (line ? " • " : "") + ph;
      appendPara(st, line, psWordDef);
    }

    var footer = addTextFrame(page, [cb.bottom - mm(8), cb.left, cb.bottom, cb.right], "", psSmall);
    footer.contents = SERIES + " • " + LANG_LABEL + " • " + BOOK_SUBTITLE + "    —    Page " + page.name;
    try { footer.parentStory.justification = Justification.CENTER_ALIGN; } catch (_) {}
  }

  for (var i = 0; i < puzzleCount; i++) {
    var page = newPage(); setMargins(doc);
    buildPuzzlePage(page, pack.puzzles[i], i + 1);
  }

  // =========================
  // Solutions
  // =========================
  function buildSolutionsTitlePage(page) {
    var cb = pageContentBounds(page);
    addTextFrame(page, [cb.top + mm(60), cb.left, cb.top + mm(90), cb.right], "SOLUTIONS", psTitle);
    addTextFrame(page, [cb.top + mm(95), cb.left, cb.top + mm(115), cb.right], "Les mots sont surlignés dans les grilles ci-dessous.", psSub);
  }

  function buildSolutionsPage(page, startIndex0, countOnPage) {
    var cb = pageContentBounds(page);
    addTextFrame(page, [cb.top, cb.left, cb.top + mm(10), cb.right], "Solutions", psH1);

    var top = cb.top + mm(14);
    var left = cb.left;
    var gutter = mm(10);
    var labelBlock = mm(22);
    var bottomPad = mm(12);

    var twoByTwo = (countOnPage >= 4);
    var availW = cb.right - cb.left;
    var availH = cb.bottom - top - bottomPad;

    var gridSize;
    if (twoByTwo) {
      var maxW = (availW - gutter) / 2;
      var maxH = (availH - gutter) / 2 - labelBlock;
      gridSize = maxW < maxH ? maxW : maxH;
      if (gridSize > mm(SOL_GRID_MM)) {
        gridSize = mm(SOL_GRID_MM);
      }
    } else {
      var maxW2 = availW;
      var maxH2 = (availH - gutter) / 2 - labelBlock;
      gridSize = maxW2 < maxH2 ? maxW2 : maxH2;
      if (gridSize > mm(110)) {
        gridSize = mm(110);
      }
    }

    var positions = [];
    if (twoByTwo) {
      var x1 = left;
      var x2 = left + gridSize + gutter;
      var y1 = top;
      var y2 = top + gridSize + labelBlock + gutter;
      positions = [{x:x1,y:y1},{x:x2,y:y1},{x:x1,y:y2},{x:x2,y:y2}];
    } else {
      var yA = top;
      var yB = top + gridSize + labelBlock + gutter;
      positions = [{x:left,y:yA},{x:left,y:yB}];
    }

    for (var i = 0; i < countOnPage; i++) {
      var puzzleIndex = startIndex0 + i;
      var puzzle = pack.puzzles[puzzleIndex];
      var pos = positions[i];

      var label = "S" + ((puzzleIndex + 1) < 10 ? "0" + (puzzleIndex + 1) : (puzzleIndex + 1));
      addTextFrame(page, [pos.y, pos.x, pos.y + mm(8), pos.x + gridSize], label + " — " + (puzzle.id || ""), psSolHdr);

      var gridTop = pos.y + mm(10);
      var lines = normalizeGridLines(puzzle);
      var rows = Math.max(1, puzzle.rows || (lines && lines.length ? lines.length : 12));
      var cols = Math.max(1, puzzle.cols || (lines && lines[0] ? lines[0].length : 12));
      var cellSize = gridSize / cols;
      if (gridSize / rows < cellSize) {
        cellSize = gridSize / rows;
      }
      var tableW = cellSize * cols;
      var tableH = cellSize * rows;
      var gx0 = pos.x + (gridSize - tableW) / 2;
      var gy0 = gridTop + (gridSize - tableH) / 2;
      var gridGb = [gy0, gx0, gy0 + tableH, gx0 + tableW];

      var solText = SOL_GRID_TEXT_PT;
      var sugSol = cellSize * 0.52;
      if (sugSol < 5) {
        solText = 5;
      } else if (sugSol < SOL_GRID_TEXT_PT) {
        solText = sugSol;
      }

      var g = createGridTable(page, gridGb, rows, cols, lines, {
        cellSizePt: cellSize,
        fontName: FONT_SANS_BOLD,
        textPt: solText,
        strokePt: 0.25,
        gridCharStyle: csGridLetter
      });
      highlightSolution(g.table, puzzle.placements || [], colHighlight);
    }

    var footer = addTextFrame(page, [cb.bottom - mm(8), cb.left, cb.bottom, cb.right], "", psSmall);
    footer.contents = SERIES + " • " + LANG_LABEL + " • " + BOOK_SUBTITLE + "    —    Solutions";
    try { footer.parentStory.justification = Justification.CENTER_ALIGN; } catch (_) {}
  }

  if (MAKE_SOLUTIONS) {
    var solTitle = newPage(); setMargins(doc);
    buildSolutionsTitlePage(solTitle);

    var per = SOLUTIONS_PER_PAGE;
    var pageCount = Math.ceil(puzzleCount / per);

    for (var p = 0; p < pageCount; p++) {
      var start = p * per;
      var count = per;
      if (start + count > puzzleCount) count = puzzleCount - start;
      var sp = newPage(); setMargins(doc);
      buildSolutionsPage(sp, start, count);
    }
  }

  alert("Terminé ! Document créé avec " + puzzleCount + " grilles" + (MAKE_SOLUTIONS ? " + solutions." : "."));
})();

