#target indesign

/*
 * Longoka_WordSearch_Book_FromPack.jsx
 * High-end Longoka Games word-search book builder.
 */

(function () {
    try {
    var thisFile = File($.fileName);
    var baseFolder = (typeof LG_BASE_FOLDER !== "undefined" && LG_BASE_FOLDER)
        ? Folder(LG_BASE_FOLDER)
        : thisFile.parent;

    $.evalFile(File(baseFolder + "/Longoka_Editions_Config.jsx"));
    $.evalFile(File(baseFolder + "/Longoka_Editions_StylesAndMasters.jsx"));

    var jsonFile = File.openDialog("Choisir un pack JSON Longoka wordsearch", "JSON:*.json");
    if (!jsonFile) {
        LG.uiCancelled();
        return;
    }

    var pack = LG.readJsonFile(jsonFile);
    LG.validateWordSearchPack(pack);

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
    LG.uiBookCreated(outFile);

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

        drawChip(page, LG.pageBox(page, 20, 23, 28, 66), LG.collectionLabelFromPack(pack), LG.SWATCHES.longokaAccent, 0, LG.SWATCHES.paper);
        drawChip(page, LG.pageBox(page, 20, 99, 28, 129), String(LG.tierLabelFromPack(pack) + " " + LG.difficultyLabelFromPack(pack)).toUpperCase(), LG.SWATCHES.longokaWarm, 8, LG.SWATCHES.longokaDark);

        var eyebrow = addFrame(page, LG.pageBox(page, 37, 22, 44, 90), "Cahier premium de mots meles", LG.STYLES.p.bookSubtitle);
        eyebrow.paragraphs[0].justification = Justification.LEFT_ALIGN;
        var volume = addFrame(page, LG.pageBox(page, 47, 22, 54, 90), LG.volumeLabelFromPack(pack), LG.STYLES.p.bookSubtitle);
        volume.paragraphs[0].justification = Justification.LEFT_ALIGN;

        var title = addFrame(page, LG.pageBox(page, 58, 22, 92, 92), compactTitle(LG.bookTitleFromPack(pack) || "Mots meles"), LG.STYLES.p.bookTitle);
        title.paragraphs[0].justification = Justification.LEFT_ALIGN;

        var subline = addFrame(page, LG.pageBox(page, 96, 22, 105, 92), LG.languageLabel(pack.language) + "  |  " + LG.modeLabelFromPack(pack), LG.STYLES.p.bookSubtitle);
        subline.paragraphs[0].justification = Justification.LEFT_ALIGN;

        var deck = addFrame(page, LG.pageBox(page, 110, 22, 131, 90), buildCoverLead(pack), LG.STYLES.p.body);
        deck.paragraphs[0].justification = Justification.LEFT_ALIGN;

        var bigNo = addFrame(page, LG.pageBox(page, 40, 98, 79, 129), LG.volumeNumberFromPack(pack), LG.STYLES.p.bookTitle);
        bigNo.paragraphs[0].justification = Justification.RIGHT_ALIGN;
        bigNo.parentStory.fillColor = doc.swatches.itemByName(LG.SWATCHES.paper);
        bigNo.texts[0].pointSize = 55;
        bigNo.texts[0].leading = 52;
        bigNo.texts[0].tracking = -40;

        var code = addFrame(page, LG.pageBox(page, 82, 101, 91, 129), LG.bookCodeFromPack(pack), LG.STYLES.p.footer);
        code.parentStory.fillColor = doc.colors.itemByName(LG.SWATCHES.longokaAccent);

        drawStatsPanel(page, LG.pageBox(page, 99, 99, 188, 130), [
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
        panel.geometricBounds = LG.pageBox(page, 150, 22, 203, 92);
        panel.fillColor = doc.colors.itemByName(LG.SWATCHES.longokaWarm);
        panel.fillTint = 25;
        panel.strokeColor = doc.swatches.itemByName("None");

        addFrame(page, LG.pageBox(page, 157, 28, 191, 86), buildCoverBlurb(pack), LG.STYLES.p.body);
    }

    function addCopyrightPage(doc, pack) {
        var page = doc.pages.add(LocationOptions.AT_END);
        LG.applyPageMargins(page);
        page.appliedMaster = doc.masterSpreads.itemByName(LG.MASTERS.copyright);

        var txt = [];
        txt.push(LG.BRAND);
        txt.push(LG.IMPRINT);
        txt.push("");
        txt.push(LG.bookTitleFromPack(pack) || "Mots meles");
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

        addFrame(page, LG.pageBox(page, 38, 28, 182, 124), txt.join("\r"), LG.STYLES.p.copyright);
    }

    function addInstructionsPage(doc, pack) {
        var page = doc.pages.add(LocationOptions.AT_END);
        LG.applyPageMargins(page);
        page.appliedMaster = doc.masterSpreads.itemByName(LG.MASTERS.front);

        addFrame(page, LG.pageBox(page, 24, 22, 36, 132), "Mode d'emploi", LG.STYLES.p.sectionTitle);
        addFrame(page, LG.pageBox(page, 38, 22, 51, 132), "Chaque page doit donner une sensation de progression nette: reperage rapide, balayage rigoureux, memorisation finale.", LG.STYLES.p.body);
        drawStepCard(page, LG.pageBox(page, 58, 22, 98, 132), "1. Reperer", "Cherche d'abord les lettres rares et les mots les plus courts.", LG.SWATCHES.longokaPanel);
        drawStepCard(page, LG.pageBox(page, 104, 22, 144, 132), "2. Balayer", "Parcours la grille ligne par ligne, puis colonne par colonne.", LG.SWATCHES.longokaPanel);
        drawStepCard(page, LG.pageBox(page, 150, 22, 190, 132), "3. Memoriser", LG.meaningLanguageFromPack(pack) === "en"
            ? "Relis la traduction anglaise et la classe nominale quand elle existe."
            : "Relis la traduction francaise et la classe nominale quand elle existe.", LG.SWATCHES.longokaPanel);

        var footer = [];
        footer.push("Pack : " + (pack.packId || ""));
        footer.push("Nombre de grilles : " + LG.puzzleCountFromPack(pack));
        footer.push("Theme : " + LG.modeLabelFromPack(pack));
        addFrame(page, LG.pageBox(page, 192, 22, 205, 132), footer.join("  |  "), LG.STYLES.p.footer);
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
        addFrame(page, LG.pageBox(page, 19, 20, 28, 132), "Grille " + pad2(puzzleNumber) + "  |  " + compactTitle(puzzle.title || pack.title || "Mots meles"), LG.STYLES.p.puzzleTitle);
        addFrame(page, LG.pageBox(page, 29, 20, 35, 132), buildMetaLine(puzzle, false), LG.STYLES.p.puzzleMeta);

        drawChip(page, LG.pageBox(page, 38, 20, 45, 51), String(LG.wordCountFromPuzzle(puzzle)) + " mots", LG.SWATCHES.longokaAccent, 10, LG.SWATCHES.longokaDark);
        drawChip(page, LG.pageBox(page, 38, 54, 45, 83), compactTitle(puzzle.difficulty || "standard"), LG.SWATCHES.longokaWarm, 10, LG.SWATCHES.longokaDark);

        var gridPanel = page.rectangles.add();
        gridPanel.itemLayer = doc.layers.itemByName(LG.LAYERS.content);
        gridPanel.geometricBounds = LG.pageBox(page, 48, 20, 123, 98);
        gridPanel.fillColor = doc.colors.itemByName(LG.SWATCHES.longokaLight);
        gridPanel.strokeColor = doc.colors.itemByName(LG.SWATCHES.longokaAccent);
        gridPanel.strokeWeight = 0.45;

        renderGridTable(doc, page, puzzle, LG.pageBox(page, 52, 24, 119, 94), false);

        drawStatsPanel(page, LG.pageBox(page, 48, 102, 123, 132), [
            "Theme",
            compactTitle(puzzle.theme || ""),
            "",
            "Classes",
            buildClassesLine(puzzle),
            "",
            "Format",
            (puzzle.rows || 0) + "x" + (puzzle.cols || 0)
        ], false);

        var lexPanel = page.rectangles.add();
        lexPanel.itemLayer = doc.layers.itemByName(LG.LAYERS.content);
        lexPanel.geometricBounds = LG.pageBox(page, 128, 20, 205, 132);
        lexPanel.fillColor = doc.colors.itemByName(LG.SWATCHES.longokaPanel);
        lexPanel.strokeColor = doc.swatches.itemByName("None");

        addFrame(page, LG.pageBox(page, 133, 26, 140, 126), "Lexique a retrouver", LG.STYLES.p.wordListHeading);
        var wordFrame = addFrame(page, LG.pageBox(page, 142, 26, 198, 126), buildWordList(puzzle.entries || [], pack), LG.STYLES.p.wordList);
        wordFrame.textFramePreferences.textColumnCount = (puzzle.entries || []).length > 24 ? 3 : 2;
        wordFrame.textFramePreferences.textColumnGutter = 4;
    }

    function addSolutionsIntroPage(doc, pack) {
        var page = doc.pages.add(LocationOptions.AT_END);
        LG.applyPageMargins(page);
        page.appliedMaster = doc.masterSpreads.itemByName(LG.MASTERS.solutions);

        addFrame(page, LG.pageBox(page, 58, 22, 80, 132), "Solutions", LG.STYLES.p.bookTitle);
        addFrame(page, LG.pageBox(page, 85, 28, 101, 126), "Version compacte 2 grilles par page, pour verification rapide.", LG.STYLES.p.bookSubtitle);
        drawChip(page, LG.pageBox(page, 112, 44, 120, 110), LG.volumeLabelFromPack(pack) + "  |  " + LG.bookCodeFromPack(pack), LG.SWATCHES.longokaWarm, 15, LG.SWATCHES.longokaDark);
    }

    function addSolutionsPages(doc, pack) {
        var i;
        for (i = 0; i < pack.puzzles.length; i += 2) {
            var page = doc.pages.add(LocationOptions.AT_END);
            LG.applyPageMargins(page);
            page.appliedMaster = doc.masterSpreads.itemByName(LG.MASTERS.solutions);
            renderSolutionBlock(doc, page, pack, pack.puzzles[i], i + 1, LG.pageBox(page, 22, 20, 109, 132));
            if (i + 1 < pack.puzzles.length) {
                renderSolutionBlock(doc, page, pack, pack.puzzles[i + 1], i + 2, LG.pageBox(page, 116, 20, 203, 132));
            }
        }
    }

    function renderSolutionBlock(doc, page, pack, puzzle, puzzleNumber, box) {
        var top = box[0];
        var left = box[1];
        var bottom = box[2];
        var right = box[3];

        var panel = page.rectangles.add();
        panel.itemLayer = doc.layers.itemByName(LG.LAYERS.solutions);
        panel.geometricBounds = box;
        panel.fillColor = doc.colors.itemByName(LG.SWATCHES.longokaPanel);
        panel.strokeColor = doc.colors.itemByName(LG.SWATCHES.longokaAccent);
        panel.strokeWeight = 0.3;

        addFrame(page, [top + 4, left + 4, top + 11, right - 4], "Solution " + pad2(puzzleNumber) + "  |  " + compactTitle(puzzle.theme || puzzle.title || ""), LG.STYLES.p.solutionTitle);

        renderGridTable(doc, page, puzzle, [top + 14, left + 5, top + 58, left + 49], true);

        addFrame(page, [top + 14, left + 54, top + 20, right - 5], buildMetaLine(puzzle, true), LG.STYLES.p.puzzleMeta);

        var words = addFrame(page, [top + 22, left + 54, bottom - 5, right - 5], buildCompactWordList(puzzle.entries || [], pack), LG.STYLES.p.solutionBody);
        words.textFramePreferences.textColumnCount = 1;
    }

    function renderGridTable(doc, page, puzzle, bounds, isSolution) {
        var tf = page.textFrames.add();
        tf.itemLayer = doc.layers.itemByName(isSolution ? LG.LAYERS.solutions : LG.LAYERS.content);
        tf.geometricBounds = bounds;
        tf.contents = "";

        var lines = normalizeGridLines(puzzle);
        var rows = lines.length;
        var cols = rows && lines[0] ? lines[0].length : 0;
        var table = tf.insertionPoints[0].tables.add({ bodyRowCount: rows, columnCount: cols });

        var tableWidth = bounds[3] - bounds[1];
        var tableHeight = bounds[2] - bounds[0];
        var colWidth = tableWidth / cols;
        var rowHeight = tableHeight / rows;
        var typeSize = isSolution ? (cols > 12 ? 6.8 : 7.8) : (cols > 12 ? 8.8 : 10.2);

        table.columns.everyItem().width = colWidth;
        table.rows.everyItem().height = rowHeight;

        var r, c, line;
        for (r = 0; r < rows; r++) {
            line = lines[r];
            for (c = 0; c < cols; c++) {
                var cell = table.rows[r].cells[c];
                cell.contents = line.charAt(c);
                cell.texts[0].justification = Justification.CENTER_ALIGN;
                cell.verticalJustification = VerticalJustification.CENTER_ALIGN;
                cell.topInset = 0;
                cell.bottomInset = 0;
                cell.leftInset = 0;
                cell.rightInset = 0;
                cell.fillColor = doc.swatches.itemByName(LG.SWATCHES.paper);
                LG.setCellStrokeUniform(cell, isSolution ? 0.24 : 0.34);
                cell.strokeColor = doc.colors.itemByName(LG.SWATCHES.longokaDark);
                cell.texts[0].appliedCharacterStyle = doc.characterStyles.itemByName(LG.STYLES.c.gridLetter);
                cell.texts[0].pointSize = typeSize;
            }
        }

        if (isSolution) {
            highlightPlacements(doc, table, puzzle.placements || []);
        }
    }

    function highlightPlacements(doc, table, placements) {
        var i, k, placement, delta, rr, cc;
        for (i = 0; i < placements.length; i++) {
            placement = placements[i];
            delta = directionDelta(placement.direction);
            if (!delta) continue;
            rr = placement.row;
            cc = placement.col;
            for (k = 0; k < placement.length; k++) {
                if (rr >= 0 && rr < table.bodyRowCount && cc >= 0 && cc < table.columnCount) {
                    table.rows[rr].cells[cc].fillColor = doc.colors.itemByName(LG.SWATCHES.longokaWarm);
                    table.rows[rr].cells[cc].fillTint = 40;
                }
                rr += delta.dr;
                cc += delta.dc;
            }
        }
    }

    function directionDelta(direction) {
        switch (direction) {
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

    function buildWordList(entries, pack) {
        var lines = [];
        var sorted = sortEntries(entries);
        var i, e, label, detail, cls, phonetic;
        for (i = 0; i < sorted.length; i++) {
            e = sorted[i];
            label = e.display || e.base || e.word || "";
            cls = e.extraInfo ? " (" + e.extraInfo + ")" : "";
            detail = LG.entryMeaningForBook(e, pack);
            phonetic = e.phonetic ? "  |  " + e.phonetic : "";
            lines.push(label + cls + (detail ? " — " + detail : "") + phonetic);
        }
        return lines.join("\r");
    }

    function buildCompactWordList(entries, pack) {
        var lines = [];
        var sorted = sortEntries(entries);
        var i, e, block;
        for (i = 0; i < sorted.length; i++) {
            e = sorted[i];
            block = formatWordSearchSolutionBlock(e, pack);
            if (block) {
                if (lines.length) {
                    lines.push("");
                }
                lines.push(block);
            }
        }
        return lines.join("\r");
    }

    /** Liste solution: mot + sens selon meta.book.meaningLanguage. */
    function formatWordSearchSolutionBlock(e, pack) {
        var head = e.display || e.base || e.word || "";
        if (!head) {
            return "";
        }
        var out = [head];
        var tr = LG.entryMeaningForBook(e, pack);
        if (tr) {
            out.push(tr);
        }
        if (e.phonetic) {
            out.push(e.phonetic);
        }
        if (e.extraInfo) {
            out.push("[" + e.extraInfo + "]");
        }
        if (e.clue && e.clue !== e.translation && e.clue !== e.translationEn) {
            out.push(String(e.clue));
        }
        return out.join("\r");
    }

    function sortEntries(entries) {
        var copy = [];
        var i;
        for (i = 0; i < entries.length; i++) {
            copy.push(entries[i]);
        }
        copy.sort(function (a, b) {
            var left = String(a.display || a.base || a.word || "").toLowerCase();
            var right = String(b.display || b.base || b.word || "").toLowerCase();
            if (left < right) return -1;
            if (left > right) return 1;
            return 0;
        });
        return copy;
    }

    function buildMetaLine(puzzle, isSolution) {
        var parts = [];
        parts.push(LG.languageLabel(puzzle.language || pack.language || ""));
        parts.push((puzzle.rows || 0) + "x" + (puzzle.cols || 0));
        parts.push(String(LG.wordCountFromPuzzle(puzzle)) + " mots");
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
        parts.push("Un volume plus dense, plus net et plus vendable.");
        parts.push("Grilles progressives, appareil lexical clair et solutions resserrees.");
        parts.push("Langue : " + LG.languageLabel(pack.language));
        return parts.join(" ");
    }

    function buildCoverLead(pack) {
        return "Memoire visuelle, vitesse de reperage et repetition lexicale dans une maquette de cahier premium.";
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
        alert("Erreur InDesign (WordSearch Book)\n\n" + String(e && (e.message || e)) + "\n\n" + String($.stack || ""));
    }
})();
