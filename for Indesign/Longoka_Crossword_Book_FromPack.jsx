#target indesign

/*
 * Longoka_Crossword_Book_FromPack.jsx
 * High-end Longoka Games crossword book builder.
 */

(function () {
    try {
    var thisFile = File($.fileName);
    var baseFolder = (typeof LG_BASE_FOLDER !== "undefined" && LG_BASE_FOLDER)
        ? Folder(LG_BASE_FOLDER)
        : thisFile.parent;

    $.evalFile(File(baseFolder + "/Longoka_Editions_Config.jsx"));
    $.evalFile(File(baseFolder + "/Longoka_Editions_StylesAndMasters.jsx"));

    var jsonFile = File.openDialog("Choisir un pack JSON Longoka crossword", "JSON:*.json");
    if (!jsonFile) {
        alert("Operation annulee.");
        return;
    }

    var pack = LG.readJsonFile(jsonFile);
    LG.validateCrosswordPack(pack);

    var doc = LG.createDocument();
    var perfState = LG.beginHeavyScript();
    try {
        buildBook(doc, pack);
    } finally {
        LG.endHeavyScript(perfState);
    }

    var outName = sanitizeFilename(LG.bookCodeFromPack(pack) + "-book.indd");
    var outFile = File(jsonFile.parent + "/" + outName);
    doc.save(outFile);
    alert("Livre cree :\n" + outFile.fsName);

    function buildBook(doc, pack) {
        addTitlePage(doc, pack);
        addCopyrightPage(doc, pack);
        addInstructionsPage(doc, pack);
        addPuzzlePages(doc, pack);
        addSolutionsIntroPage(doc, pack);
        addSolutionsPages(doc, pack);
    }

    function addTitlePage(doc, pack) {
        var page = doc.pages[0];
        page.appliedMaster = doc.masterSpreads.itemByName(LG.MASTERS.front);

        drawChip(page, [20, 23, 28, 66], LG.collectionLabelFromPack(pack), LG.SWATCHES.longokaAccent, 0, LG.SWATCHES.paper);
        drawChip(page, [20, 99, 28, 129], String(LG.tierLabelFromPack(pack) + " " + LG.difficultyLabelFromPack(pack)).toUpperCase(), LG.SWATCHES.longokaWarm, 8, LG.SWATCHES.longokaDark);

        var eyebrow = addFrame(page, [37, 22, 44, 90], "Cahier premium de mots croises", LG.STYLES.p.bookSubtitle);
        eyebrow.paragraphs[0].justification = Justification.LEFT_ALIGN;
        var volume = addFrame(page, [47, 22, 54, 90], LG.volumeLabelFromPack(pack), LG.STYLES.p.bookSubtitle);
        volume.paragraphs[0].justification = Justification.LEFT_ALIGN;

        var title = addFrame(page, [58, 22, 92, 92], compactTitle(LG.bookTitleFromPack(pack) || "Mots croises"), LG.STYLES.p.bookTitle);
        title.paragraphs[0].justification = Justification.LEFT_ALIGN;

        var subline = addFrame(page, [96, 22, 105, 92], LG.languageLabel(pack.language) + "  |  " + LG.modeLabelFromPack(pack), LG.STYLES.p.bookSubtitle);
        subline.paragraphs[0].justification = Justification.LEFT_ALIGN;

        var deck = addFrame(page, [110, 22, 131, 90], buildCoverLead(pack), LG.STYLES.p.body);
        deck.paragraphs[0].justification = Justification.LEFT_ALIGN;

        var bigNo = addFrame(page, [40, 98, 79, 129], LG.volumeNumberFromPack(pack), LG.STYLES.p.bookTitle);
        bigNo.paragraphs[0].justification = Justification.RIGHT_ALIGN;
        bigNo.parentStory.fillColor = doc.swatches.itemByName(LG.SWATCHES.paper);
        bigNo.texts[0].pointSize = 55;
        bigNo.texts[0].leading = 52;
        bigNo.texts[0].tracking = -40;

        var code = addFrame(page, [82, 101, 91, 129], LG.bookCodeFromPack(pack), LG.STYLES.p.footer);
        code.parentStory.fillColor = doc.colors.itemByName(LG.SWATCHES.longokaAccent);

        drawStatsPanel(page, [99, 99, 188, 130], [
            "Grilles",
            String(LG.puzzleCountFromPack(pack)),
            "",
            "Langue",
            LG.languageLabel(pack.language),
            "",
            "Theme",
            LG.modeLabelFromPack(pack)
        ], true);

        var panel = page.rectangles.add();
        panel.itemLayer = doc.layers.itemByName(LG.LAYERS.content);
        panel.geometricBounds = [150, 22, 203, 92];
        panel.fillColor = doc.colors.itemByName(LG.SWATCHES.longokaWarm);
        panel.fillTint = 25;
        panel.strokeColor = doc.swatches.itemByName("None");

        addFrame(page, [157, 28, 191, 86], buildCoverBlurb(pack), LG.STYLES.p.body);
    }

    function addCopyrightPage(doc, pack) {
        var page = doc.pages.add(LocationOptions.AT_END);
        LG.applyPageMargins(page);
        page.appliedMaster = doc.masterSpreads.itemByName(LG.MASTERS.copyright);

        var txt = [];
        txt.push(LG.BRAND);
        txt.push(LG.IMPRINT);
        txt.push("");
        txt.push(LG.bookTitleFromPack(pack) || "Mots croises");
        var bookSub = LG.bookSubtitleFromPack(pack);
        if (bookSub) {
            txt.push(bookSub);
        }
        txt.push(LG.volumeLabelFromPack(pack));
        txt.push("");
        txt.push("Identifiant editorial : " + LG.bookCodeFromPack(pack));
        if (LG.isbnFromPack(pack)) txt.push("ISBN : " + LG.isbnFromPack(pack));
        if (LG.publishedAtFromPack(pack)) txt.push("Date : " + LG.publishedAtFromPack(pack));
        txt.push("Collection : " + LG.collectionLabelFromPack(pack));
        txt.push("Pack : " + (pack.packId || ""));
        txt.push("Langue : " + LG.languageLabel(pack.language));
        txt.push("Source : " + (pack.description || "Longoka lexical packs"));
        txt.push("");
        txt.push("Tous droits reserves.");
        txt.push("Aucune partie de cet ouvrage ne peut etre reproduite sans autorisation ecrite.");
        txt.push("");
        txt.push(LG.WEBSITE);

        addFrame(page, [38, 28, 182, 124], txt.join("\r"), LG.STYLES.p.copyright);
    }

    function addInstructionsPage(doc, pack) {
        var page = doc.pages.add(LocationOptions.AT_END);
        LG.applyPageMargins(page);
        page.appliedMaster = doc.masterSpreads.itemByName(LG.MASTERS.front);

        addFrame(page, [24, 22, 36, 132], "Mode d'emploi", LG.STYLES.p.sectionTitle);
        addFrame(page, [38, 22, 51, 132], "Le rendu doit rester sobre mais ferme: une grille lisible, des indices nets et une correction rapide en fin d'ouvrage.", LG.STYLES.p.body);
        drawStepCard(page, [58, 22, 98, 132], "1. Lire", "Lis les indices et repere les entrees courtes avant les longues.", LG.SWATCHES.longokaPanel);
        drawStepCard(page, [104, 22, 144, 132], "2. Croiser", "Utilise les lettres deja trouvees pour debloquer les autres mots.", LG.SWATCHES.longokaPanel);
        drawStepCard(page, [150, 22, 190, 132], "3. Verifier", "Controle les classes et les traductions pour fixer le vocabulaire.", LG.SWATCHES.longokaPanel);

        var footer = [];
        footer.push("Pack : " + (pack.packId || ""));
        footer.push("Nombre de grilles : " + LG.puzzleCountFromPack(pack));
        footer.push("Theme : " + LG.modeLabelFromPack(pack));
        addFrame(page, [192, 22, 205, 132], footer.join("  |  "), LG.STYLES.p.footer);
    }

    function addPuzzlePages(doc, pack) {
        var i;
        for (i = 0; i < pack.puzzles.length; i++) {
            var page = doc.pages.add(LocationOptions.AT_END);
            LG.applyPageMargins(page);
            page.appliedMaster = doc.masterSpreads.itemByName(LG.MASTERS.puzzle);
            renderPuzzlePage(doc, page, pack.puzzles[i], i + 1);
        }
    }

    function renderPuzzlePage(doc, page, puzzle, puzzleNumber) {
        addFrame(page, [19, 20, 28, 132], "Grille " + pad2(puzzleNumber) + "  |  " + compactTitle(puzzle.title || pack.title || "Mots croises"), LG.STYLES.p.puzzleTitle);
        addFrame(page, [29, 20, 35, 132], buildMetaLine(puzzle, false), LG.STYLES.p.puzzleMeta);

        drawChip(page, [38, 20, 45, 52], String(LG.wordCountFromPuzzle(puzzle)) + " entrees", LG.SWATCHES.longokaAccent, 10, LG.SWATCHES.longokaDark);
        drawChip(page, [38, 55, 45, 83], compactTitle(puzzle.difficulty || "standard"), LG.SWATCHES.longokaWarm, 10, LG.SWATCHES.longokaDark);

        var gridPanel = page.rectangles.add();
        gridPanel.itemLayer = doc.layers.itemByName(LG.LAYERS.content);
        gridPanel.geometricBounds = [48, 20, 116, 88];
        gridPanel.fillColor = doc.colors.itemByName(LG.SWATCHES.longokaLight);
        gridPanel.strokeColor = doc.colors.itemByName(LG.SWATCHES.longokaAccent);
        gridPanel.strokeWeight = 0.45;

        renderCrosswordGrid(doc, page, puzzle, [52, 24, 112, 84], false);

        drawStatsPanel(page, [48, 92, 116, 132], [
            "Theme",
            compactTitle(puzzle.theme || ""),
            "",
            "Classes",
            buildClassesLine(puzzle),
            "",
            "Format",
            (puzzle.rows || 0) + "x" + (puzzle.cols || 0)
        ], false);

        var cluesPanel = page.rectangles.add();
        cluesPanel.itemLayer = doc.layers.itemByName(LG.LAYERS.content);
        cluesPanel.geometricBounds = [122, 20, 205, 132];
        cluesPanel.fillColor = doc.colors.itemByName(LG.SWATCHES.longokaPanel);
        cluesPanel.strokeColor = doc.swatches.itemByName("None");

        var divider = page.graphicLines.add();
        divider.itemLayer = doc.layers.itemByName(LG.LAYERS.content);
        divider.paths[0].entirePath = [[79, 136], [79, 198]];
        divider.strokeWeight = 0.35;
        divider.strokeColor = doc.colors.itemByName(LG.SWATCHES.longokaAccent);
        divider.strokeTint = 40;

        addFrame(page, [127, 26, 134, 76], "Horizontaux", LG.STYLES.p.wordListHeading);
        addFrame(page, [127, 82, 134, 126], "Verticaux", LG.STYLES.p.wordListHeading);
        addFrame(page, [136, 26, 198, 76], buildClueList(puzzle.entries || [], "ACROSS", pack), LG.STYLES.p.wordList);
        addFrame(page, [136, 82, 198, 126], buildClueList(puzzle.entries || [], "DOWN", pack), LG.STYLES.p.wordList);
    }

    function addSolutionsIntroPage(doc, pack) {
        var page = doc.pages.add(LocationOptions.AT_END);
        LG.applyPageMargins(page);
        page.appliedMaster = doc.masterSpreads.itemByName(LG.MASTERS.solutions);

        addFrame(page, [58, 22, 80, 132], "Solutions", LG.STYLES.p.bookTitle);
        addFrame(page, [85, 28, 101, 126], "Version compacte 2 grilles par page, avec reponses regroupees.", LG.STYLES.p.bookSubtitle);
        drawChip(page, [112, 44, 120, 110], LG.volumeLabelFromPack(pack) + "  |  " + LG.bookCodeFromPack(pack), LG.SWATCHES.longokaWarm, 15, LG.SWATCHES.longokaDark);
    }

    function addSolutionsPages(doc, pack) {
        var i;
        for (i = 0; i < pack.puzzles.length; i += 2) {
            var page = doc.pages.add(LocationOptions.AT_END);
            LG.applyPageMargins(page);
            page.appliedMaster = doc.masterSpreads.itemByName(LG.MASTERS.solutions);
            renderSolutionBlock(doc, page, pack, pack.puzzles[i], i + 1, [22, 20, 109, 132]);
            if (i + 1 < pack.puzzles.length) {
                renderSolutionBlock(doc, page, pack, pack.puzzles[i + 1], i + 2, [116, 20, 203, 132]);
            }
        }
    }

    function renderSolutionBlock(doc, page, pack, puzzle, puzzleNumber, box) {
        var top = box[0];
        var left = box[1];
        var bottom = box[2];
        var right = box[3];

        var panel = page.rectangles.add();
        panel.itemLayer = page.parent.layers.itemByName(LG.LAYERS.solutions);
        panel.geometricBounds = box;
        panel.fillColor = page.parent.colors.itemByName(LG.SWATCHES.longokaPanel);
        panel.strokeColor = page.parent.colors.itemByName(LG.SWATCHES.longokaAccent);
        panel.strokeWeight = 0.3;

        addFrame(page, [top + 4, left + 4, top + 11, right - 4], "Solution " + pad2(puzzleNumber) + "  |  " + compactTitle(puzzle.theme || puzzle.title || ""), LG.STYLES.p.solutionTitle);
        renderCrosswordGrid(doc, page, puzzle, [top + 14, left + 5, top + 58, left + 49], true);

        addFrame(page, [top + 14, left + 54, top + 20, right - 5], buildMetaLine(puzzle, true), LG.STYLES.p.puzzleMeta);
        addFrame(page, [top + 22, left + 54, bottom - 5, right - 5], buildCompactAnswerList(puzzle.entries || [], pack), LG.STYLES.p.solutionBody);
    }

    function renderCrosswordGrid(doc, page, puzzle, bounds, isSolution) {
        var tf = page.textFrames.add();
        tf.itemLayer = doc.layers.itemByName(isSolution ? LG.LAYERS.solutions : LG.LAYERS.content);
        tf.geometricBounds = bounds;
        tf.contents = "";

        var lines = normalizeGridLines(puzzle);
        var rows = lines.length;
        var cols = rows && lines[0] ? lines[0].length : 0;
        var startMap = buildStartMap(puzzle.entries || []);
        var table = tf.insertionPoints[0].tables.add({ bodyRowCount: rows, columnCount: cols });

        var tableWidth = bounds[3] - bounds[1];
        var tableHeight = bounds[2] - bounds[0];
        var colWidth = tableWidth / cols;
        var rowHeight = tableHeight / rows;
        var letterSize = isSolution ? (cols > 12 ? 6.6 : 7.6) : 9.2;

        table.columns.everyItem().width = colWidth;
        table.rows.everyItem().height = rowHeight;

        var r, c, line, key, cell, ch, number;
        for (r = 0; r < rows; r++) {
            line = lines[r];
            for (c = 0; c < cols; c++) {
                key = r + "-" + c;
                cell = table.rows[r].cells[c];
                ch = line.charAt(c);
                number = startMap[key];

                cell.topInset = 0.35;
                cell.bottomInset = 0.15;
                cell.leftInset = 0.35;
                cell.rightInset = 0.15;
                cell.strokeWeight = 0.34;
                cell.strokeColor = doc.colors.itemByName(LG.SWATCHES.longokaDark);

                if (ch === "#") {
                    cell.contents = "";
                    cell.fillColor = doc.colors.itemByName(LG.SWATCHES.longokaDark);
                    continue;
                }

                cell.fillColor = doc.swatches.itemByName(LG.SWATCHES.paper);

                if (isSolution) {
                    cell.contents = ch;
                    cell.verticalJustification = VerticalJustification.CENTER_ALIGN;
                    cell.texts[0].justification = Justification.CENTER_ALIGN;
                    cell.texts[0].appliedCharacterStyle = doc.characterStyles.itemByName(LG.STYLES.c.gridLetter);
                    cell.texts[0].pointSize = letterSize;
                } else if (number) {
                    cell.contents = String(number);
                    cell.verticalJustification = VerticalJustification.TOP_ALIGN;
                    cell.texts[0].justification = Justification.LEFT_ALIGN;
                    cell.texts[0].pointSize = 5.2;
                    try { cell.texts[0].appliedFont = "Myriad Pro\tBold"; } catch (eFont) {}
                } else {
                    cell.contents = "";
                }
            }
        }
    }

    function buildStartMap(entries) {
        var map = {};
        var i, entry;
        for (i = 0; i < entries.length; i++) {
            entry = entries[i];
            map[entry.row + "-" + entry.col] = entry.number;
        }
        return map;
    }

    function buildClueList(entries, direction, pack) {
        var sorted = sortEntriesByNumber(entries, direction);
        var lines = [];
        var i, entry, clue, extra;
        for (i = 0; i < sorted.length; i++) {
            entry = sorted[i];
            clue = LG.entryMeaningForBook(entry, pack) || entry.clue || "Indice a completer";
            extra = entry.extraInfo ? " [" + entry.extraInfo + "]" : "";
            lines.push(entry.number + ". " + clue + extra);
        }
        return lines.join("\r");
    }

    function buildCompactAnswerList(entries, pack) {
        var across = sortEntriesByNumber(entries, "ACROSS");
        var down = sortEntriesByNumber(entries, "DOWN");
        var lines = ["Horizontaux"];
        appendAnswerLines(lines, across, pack);
        lines.push("");
        lines.push("Verticaux");
        appendAnswerLines(lines, down, pack);
        return lines.join("\r");
    }

    function appendAnswerLines(lines, entries, pack) {
        var i, entry, block;
        for (i = 0; i < entries.length; i++) {
            entry = entries[i];
            block = formatSolutionEntryBlock(entry, pack);
            if (block) {
                if (lines.length) {
                    lines.push("");
                }
                lines.push(block);
            }
        }
    }

    /** Reponses + sens selon meta.book.meaningLanguage (fin de livre). */
    function formatSolutionEntryBlock(entry, pack) {
        var head = entry.display || entry.answer || "";
        if (!head) {
            return "";
        }
        var out = [];
        out.push(entry.number + ". " + head);
        var tr = LG.entryMeaningForBook(entry, pack);
        if (tr) {
            out.push(tr);
        }
        if (entry.phonetic) {
            out.push(entry.phonetic);
        }
        if (entry.extraInfo) {
            out.push("[" + entry.extraInfo + "]");
        }
        return out.join("\r");
    }

    function sortEntriesByNumber(entries, direction) {
        var filtered = [];
        var i;
        for (i = 0; i < entries.length; i++) {
            if (entries[i].direction === direction) {
                filtered.push(entries[i]);
            }
        }
        filtered.sort(function (a, b) {
            return Number(a.number || 0) - Number(b.number || 0);
        });
        return filtered;
    }

    function buildMetaLine(puzzle, isSolution) {
        var parts = [];
        parts.push(LG.languageLabel(puzzle.language || pack.language || ""));
        parts.push((puzzle.rows || 0) + "x" + (puzzle.cols || 0));
        parts.push(String(LG.wordCountFromPuzzle(puzzle)) + " entrees");
        if (puzzle.difficulty) parts.push(compactTitle(puzzle.difficulty));
        if (buildClassesLine(puzzle)) parts.push(buildClassesLine(puzzle));
        if (isSolution) parts.push("grille resolue");
        return parts.join("  |  ");
    }

    function buildClassesLine(puzzle) {
        if (puzzle.meta && puzzle.meta.nominalClasses && puzzle.meta.nominalClasses.length) {
            return puzzle.meta.nominalClasses.join(", ");
        }
        return "";
    }

    function buildCoverBlurb(pack) {
        var parts = [];
        parts.push("Des grilles croisees plus nettes, plus compactes et plus affirmées.");
        parts.push("Indices clairs, lecture croisee et solutions resserrees.");
        parts.push("Langue : " + LG.languageLabel(pack.language));
        return parts.join(" ");
    }

    function buildCoverLead(pack) {
        return "Un volume de pratique lexicale et logique, avec un habillage editorial plus solide et plus vendable.";
    }

    function drawStepCard(page, bounds, title, body, swatchName) {
        var card = page.rectangles.add();
        card.itemLayer = page.parent.layers.itemByName(LG.LAYERS.content);
        card.geometricBounds = bounds;
        card.fillColor = page.parent.colors.itemByName(swatchName);
        card.strokeColor = page.parent.swatches.itemByName("None");

        addFrame(page, [bounds[0] + 6, bounds[1] + 6, bounds[0] + 14, bounds[3] - 6], title, LG.STYLES.p.wordListHeading);
        addFrame(page, [bounds[0] + 18, bounds[1] + 6, bounds[2] - 6, bounds[3] - 6], body, LG.STYLES.p.body);
    }

    function drawStatsPanel(page, bounds, lines, invert) {
        var doc = page.parent;
        var panel = page.rectangles.add();
        panel.itemLayer = doc.layers.itemByName(LG.LAYERS.content);
        panel.geometricBounds = bounds;
        panel.fillColor = doc.colors.itemByName(invert ? LG.SWATCHES.longokaDark : LG.SWATCHES.longokaPanel);
        panel.strokeColor = doc.swatches.itemByName("None");

        var groups = [];
        var i;
        for (i = 0; i < lines.length - 1; i++) {
            if (lines[i] !== "" && lines[i + 1] !== "") {
                groups.push({ label: lines[i], value: lines[i + 1] });
                i += 1;
            }
        }

        var innerTop = bounds[0] + 6;
        var innerLeft = bounds[1] + 5;
        var innerRight = bounds[3] - 5;
        var blockHeight = (bounds[2] - bounds[0] - 12) / Math.max(groups.length, 1);
        var labelColor = invert ? doc.colors.itemByName(LG.SWATCHES.longokaAccent) : doc.colors.itemByName(LG.SWATCHES.longokaDark);
        var valueColor = invert ? doc.swatches.itemByName(LG.SWATCHES.paper) : doc.colors.itemByName(LG.SWATCHES.longokaDark);

        for (i = 0; i < groups.length; i++) {
            var top = innerTop + (i * blockHeight);
            var label = addFrame(page, [top, innerLeft, top + 5, innerRight], String(groups[i].label).toUpperCase(), LG.STYLES.p.footer);
            label.parentStory.fillColor = labelColor;

            var value = addFrame(page, [top + 5.5, innerLeft, top + 16.5, innerRight], groups[i].value, LG.STYLES.p.solutionTitle);
            value.paragraphs[0].justification = Justification.LEFT_ALIGN;
            value.parentStory.fillColor = valueColor;

            if (i < groups.length - 1) {
                var rule = page.graphicLines.add();
                rule.itemLayer = doc.layers.itemByName(LG.LAYERS.content);
                rule.paths[0].entirePath = [[innerLeft, top + blockHeight - 1.4], [innerRight, top + blockHeight - 1.4]];
                rule.strokeWeight = 0.35;
                rule.strokeColor = invert ? doc.colors.itemByName(LG.SWATCHES.longokaAccent) : doc.colors.itemByName(LG.SWATCHES.longokaDark);
                rule.strokeTint = invert ? 55 : 22;
            }
        }
    }

    function drawChip(page, bounds, label, fillName, tint, textSwatchName) {
        var chip = page.rectangles.add();
        chip.itemLayer = page.parent.layers.itemByName(LG.LAYERS.content);
        chip.geometricBounds = bounds;
        chip.fillColor = page.parent.colors.itemByName(fillName);
        chip.fillTint = tint || 0;
        chip.strokeColor = page.parent.swatches.itemByName("None");

        var frame = addFrame(page, [bounds[0] + 1.2, bounds[1] + 2, bounds[2] - 0.8, bounds[3] - 2], label, LG.STYLES.p.footer);
        frame.parentStory.fillColor = page.parent.swatches.itemByName(textSwatchName || LG.SWATCHES.black);
        frame.texts[0].tracking = 110;
    }

    function compactTitle(value) {
        return String(value || "")
            .replace(/_/g, " ")
            .replace(/-/g, " ")
            .replace(/\s+/g, " ")
            .replace(/^\s+|\s+$/g, "");
    }

    function normalizeGridLines(puzzle) {
        var lines = [];
        var i;
        for (i = 0; i < puzzle.grid.length; i++) {
            lines.push(String(puzzle.grid[i]).replace(/\s+/g, ""));
        }
        return lines;
    }

    function addFrame(page, bounds, text, styleName) {
        var frame = page.textFrames.add();
        frame.itemLayer = page.parent.layers.itemByName(LG.LAYERS.content);
        frame.geometricBounds = bounds;
        frame.contents = text;
        if (styleName && frame.paragraphs.length > 0) {
            frame.paragraphs.everyItem().appliedParagraphStyle = page.parent.paragraphStyles.itemByName(styleName);
        }
        return frame;
    }

    function sanitizeFilename(name) {
        return name.replace(/[\\\/:*?"<>|]/g, "-");
    }

    function pad2(value) {
        return value < 10 ? "0" + value : String(value);
    }
    } catch (e) {
        alert("Erreur InDesign (Crossword Book)\n\n" + String(e && (e.message || e)) + "\n\n" + String($.stack || ""));
    }
})();
