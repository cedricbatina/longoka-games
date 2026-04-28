#target indesign

/*
 * Longoka_MorphoAnagram_Book_FromPack.jsx
 * Livre anagrammes morphologiques (meme pipeline que mots croises / mots fleches).
 */

(function () {
    try {
    var thisFile = File($.fileName);
    var baseFolder = (typeof LG_BASE_FOLDER !== "undefined" && LG_BASE_FOLDER)
        ? Folder(LG_BASE_FOLDER)
        : thisFile.parent;

    $.evalFile(File(baseFolder + "/Longoka_Editions_Config.jsx"));
    $.evalFile(File(baseFolder + "/Longoka_Editions_StylesAndMasters.jsx"));

    var jsonFile = File.openDialog("Choisir un pack JSON Longoka anagrammes morphologiques", "JSON:*.json");
    if (!jsonFile) {
        LG.uiCancelled();
        return;
    }

    var pack = LG.readJsonFile(jsonFile);
    LG.validateMorphoAnagramPack(pack);

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

        addFrame(page, LG.pageBox(page, 37, 22, 44, 90), "Cahier premium d'anagrammes morphologiques", LG.STYLES.p.bookSubtitle).paragraphs[0].justification = Justification.LEFT_ALIGN;
        addFrame(page, LG.pageBox(page, 47, 22, 54, 90), LG.volumeLabelFromPack(pack), LG.STYLES.p.bookSubtitle).paragraphs[0].justification = Justification.LEFT_ALIGN;

        var title = addFrame(page, LG.pageBox(page, 58, 22, 92, 92), compactTitle(LG.bookTitleFromPack(pack) || "Anagrammes morphologiques"), LG.STYLES.p.bookTitle);
        title.paragraphs[0].justification = Justification.LEFT_ALIGN;

        var subline = addFrame(page, LG.pageBox(page, 96, 22, 105, 92), LG.languageLabel(pack.language) + "  |  " + LG.modeLabelFromPack(pack), LG.STYLES.p.bookSubtitle);
        subline.paragraphs[0].justification = Justification.LEFT_ALIGN;

        addFrame(page, LG.pageBox(page, 110, 22, 131, 90), buildCoverLead(pack), LG.STYLES.p.body).paragraphs[0].justification = Justification.LEFT_ALIGN;

        var bigNo = addFrame(page, LG.pageBox(page, 40, 98, 79, 129), LG.volumeNumberFromPack(pack), LG.STYLES.p.bookTitle);
        bigNo.paragraphs[0].justification = Justification.RIGHT_ALIGN;
        bigNo.parentStory.fillColor = doc.swatches.itemByName(LG.SWATCHES.paper);
        bigNo.texts[0].pointSize = 55;
        bigNo.texts[0].leading = 52;
        bigNo.texts[0].tracking = -40;

        var code = addFrame(page, LG.pageBox(page, 82, 101, 91, 129), LG.bookCodeFromPack(pack), LG.STYLES.p.footer);
        code.parentStory.fillColor = doc.colors.itemByName(LG.SWATCHES.longokaAccent);

        drawStatsPanel(page, LG.pageBox(page, 99, 99, 188, 130), [
            "Pages jeu",
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
        txt.push(LG.bookTitleFromPack(pack) || "Anagrammes morphologiques");
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
        addFrame(page, LG.pageBox(page, 38, 22, 51, 132), "Reconstitue chaque forme cible en ordonnant les pieces dans les emplacements. Les corrections en fin de volume donnent le detail lexical complet.", LG.STYLES.p.body);
        drawStepCard(page, LG.pageBox(page, 58, 22, 98, 132), "1. Lire", "Repere la consigne et la forme a reconstruire.", LG.SWATCHES.longokaPanel);
        drawStepCard(page, LG.pageBox(page, 104, 22, 144, 132), "2. Composer", "Glisse mentalement (ou sur epreuve) les segments dans l'ordre logique.", LG.SWATCHES.longokaPanel);
        drawStepCard(page, LG.pageBox(page, 150, 22, 190, 132), "3. Verifier", "Compare avec la correction : traductions FR/EN, phonetique, notes.", LG.SWATCHES.longokaPanel);

        var tail = [];
        var lexNote = LG.lexiconPolicyTextFromPack(pack);
        if (lexNote) {
            tail.push(lexNote);
            tail.push("");
        }
        tail.push("Pack : " + (pack.packId || "") + "  |  Defis : " + LG.puzzleCountFromPack(pack));
        addFrame(page, LG.pageBox(page, 172, 22, 205, 132), tail.join("\r"), LG.STYLES.p.instruction);
    }

    function addPuzzlePages(doc, pack) {
        var i;
        for (i = 0; i < pack.puzzles.length; i++) {
            var page = doc.pages.add(LocationOptions.AT_END);
            LG.applyPageMargins(page);
            page.appliedMaster = doc.masterSpreads.itemByName(LG.MASTERS.puzzle);
            renderPuzzlePage(doc, page, pack.puzzles[i], i + 1, pack);
        }
    }

    function renderPuzzlePage(doc, page, puzzle, puzzleNumber, pack) {
        addFrame(page, LG.pageBox(page, 19, 20, 28, 132), "Defi " + pad2(puzzleNumber) + "  |  " + compactTitle(puzzle.title || pack.title || "Anagrammes"), LG.STYLES.p.puzzleTitle);
        addFrame(page, LG.pageBox(page, 29, 20, 36, 132), buildPuzzleMetaLine(puzzle, pack), LG.STYLES.p.puzzleMeta);

        var challenges = puzzle.challenges || [];
        var cursorY = 42;
        var left = 22;
        var right = 132;
        var bottom = 198;
        var c;

        for (c = 0; c < challenges.length; c++) {
            if (cursorY > bottom - 38) {
                break;
            }
            var challenge = challenges[c];
            var shortHint = truncateOneLine(challengeLinePrimary(challenge), 78);
            addFrame(page, LG.pageBox(page, cursorY, left, cursorY + 7, right), (c + 1) + ". " + shortHint + "  |  " + (challenge.score || 0) + " pts", LG.STYLES.p.wordListHeading);

            var slotY = cursorY + 8;
            var slotW = 19;
            var slotH = 10;
            var slotGap = 2.2;
            var solutionOrder = challenge.solutionOrder || [];
            var s;
            for (s = 0; s < solutionOrder.length; s++) {
                var slotX = left + (s * (slotW + slotGap));
                var slotRect = page.rectangles.add();
                slotRect.itemLayer = doc.layers.itemByName(LG.LAYERS.content);
                slotRect.geometricBounds = LG.pageBox(page, slotY, slotX, slotY + slotH, slotX + slotW);
                slotRect.strokeColor = doc.colors.itemByName(LG.SWATCHES.longokaAccent);
                slotRect.strokeWeight = 0.4;
                slotRect.fillColor = doc.swatches.itemByName(LG.SWATCHES.paper);
            }

            var pieces = challenge.pieces || [];
            var pieceY = slotY + 12;
            var pieceW = 23;
            var pieceH = 12;
            var pieceGap = 2.2;
            var p;
            for (p = 0; p < pieces.length; p++) {
                drawPiece(page, left + (p * (pieceW + pieceGap)), pieceY, pieceW, pieceH, pieces[p], doc);
            }

            cursorY = pieceY + 16 + 6;
        }

        if (cursorY < bottom - 12) {
            addFrame(page, LG.pageBox(page, bottom - 10, left, bottom, right), "Correction detaillee en fin de volume.", LG.STYLES.p.footer);
        }
    }

    /** Une seule ligne courte pour l'en-tete du defi (evite de surcharger la page). */
    function challengeLinePrimary(ch) {
        var t = ch.translation || ch.display || ch.answer || ch.label || "";
        return String(t);
    }

    function truncateOneLine(text, maxLen) {
        var s = String(text || "").replace(/\r|\n/g, " ");
        if (s.length <= maxLen) {
            return s;
        }
        return s.substring(0, Math.max(0, maxLen - 1)) + "...";
    }

    function drawPiece(page, x, y, w, h, piece, docRef) {
        var rect = page.rectangles.add();
        rect.itemLayer = docRef.layers.itemByName(LG.LAYERS.content);
        rect.geometricBounds = LG.pageBox(page, y, x, y + h, x + w);
        rect.strokeColor = docRef.colors.itemByName(LG.SWATCHES.longokaDark);
        rect.strokeWeight = 0.35;
        rect.fillColor = docRef.colors.itemByName(LG.SWATCHES.longokaLight);
        rect.fillTint = 40;

        var txt = String(piece && piece.text ? piece.text : "");
        addFrame(page, LG.pageBox(page, y + 1.2, x + 1.5, y + h - 2, x + w - 1.5), txt, LG.STYLES.p.wordList);
        var role = String(piece && piece.role ? piece.role : "");
        var pts = piece && piece.points ? ("  " + piece.points + " pts") : "";
        if (role || pts) {
            addFrame(page, LG.pageBox(page, y + h - 6.5, x + 1.5, y + h - 1, x + w - 1.5), role + pts, LG.STYLES.p.footer);
        }
    }

    function buildPuzzleMetaLine(puzzle, pack) {
        var parts = [];
        parts.push(LG.languageLabel(puzzle.language || pack.language || ""));
        parts.push(compactTitle(puzzle.theme || ""));
        parts.push("Defis : " + String((puzzle.challenges || []).length));
        if (puzzle.relationType) {
            parts.push(String(puzzle.relationType));
        }
        return parts.join("  |  ");
    }

    function addSolutionsIntroPage(doc, pack) {
        var page = doc.pages.add(LocationOptions.AT_END);
        LG.applyPageMargins(page);
        page.appliedMaster = doc.masterSpreads.itemByName(LG.MASTERS.solutions);

        addFrame(page, LG.pageBox(page, 58, 22, 80, 132), "Corrections", LG.STYLES.p.bookTitle);
        addFrame(page, LG.pageBox(page, 85, 28, 101, 126), "Traductions completes (FR, EN si disponible), phonetique et ordre de solution pour chaque defi.", LG.STYLES.p.bookSubtitle);
        drawChip(page, LG.pageBox(page, 112, 44, 120, 110), LG.volumeLabelFromPack(pack) + "  |  " + LG.bookCodeFromPack(pack), LG.SWATCHES.longokaWarm, 15, LG.SWATCHES.longokaDark);
    }

    function addSolutionsPages(doc, pack) {
        var i;
        for (i = 0; i < pack.puzzles.length; i++) {
            var page = doc.pages.add(LocationOptions.AT_END);
            LG.applyPageMargins(page);
            page.appliedMaster = doc.masterSpreads.itemByName(LG.MASTERS.solutions);
            renderSolutionPage(doc, page, pack.puzzles[i], i + 1, pack);
        }
    }

    function renderSolutionPage(doc, page, puzzle, puzzleNumber, pack) {
        var panel = page.rectangles.add();
        panel.itemLayer = doc.layers.itemByName(LG.LAYERS.solutions);
        panel.geometricBounds = LG.pageBox(page, 22, 20, 203, 132);
        panel.fillColor = doc.colors.itemByName(LG.SWATCHES.longokaPanel);
        panel.strokeColor = doc.colors.itemByName(LG.SWATCHES.longokaAccent);
        panel.strokeWeight = 0.3;

        addFrame(page, LG.pageBox(page, 26, 24, 36, 128), "Correction defi " + pad2(puzzleNumber) + "  |  " + compactTitle(puzzle.title || ""), LG.STYLES.p.solutionTitle);
        addFrame(page, LG.pageBox(page, 38, 24, 198, 128), buildFullAnagramCorrectionText(puzzle, pack), LG.STYLES.p.solutionBody);
    }

    function buildFullAnagramCorrectionText(puzzle, pack) {
        var challenges = puzzle.challenges || [];
        var lines = [];
        var c, ch, k, parts, ordered, pieceMap, list, m, found, order, o;
        for (c = 0; c < challenges.length; c++) {
            ch = challenges[c];
            if (lines.length) {
                lines.push("");
            }
            parts = [];
            parts.push((c + 1) + ". " + String(ch.display || ch.answer || ""));
            var tr = LG.challengeMeaningForBook(ch, pack);
            if (tr) {
                parts.push(tr);
            }
            if (ch.phonetic) {
                parts.push(ch.phonetic);
            }
            if (ch.label) {
                parts.push("Etiquette : " + ch.label);
            }
            pieceMap = {};
            list = ch.pieces || [];
            for (m = 0; m < list.length; m++) {
                pieceMap[String(list[m].id || "piece-" + (m + 1))] = list[m];
            }
            ordered = [];
            order = ch.solutionOrder || [];
            for (o = 0; o < order.length; o++) {
                found = pieceMap[String(order[o])];
                if (found && found.text) {
                    ordered.push(String(found.text));
                }
            }
            if (ordered.length) {
                parts.push("Ordre : " + ordered.join(" | "));
            }
            if (ch.entryRef) {
                var er = ch.entryRef;
                var refMeaning = LG.entryRefMeaningForBook(er, pack);
                var chMeaning = LG.challengeMeaningForBook(ch, pack);
                if (refMeaning && refMeaning !== chMeaning) {
                    parts.push("Ref. : " + refMeaning);
                }
                if (er.extraInfo) {
                    parts.push("[" + er.extraInfo + "]");
                }
            }
            lines.push(parts.join("\r"));
        }
        return lines.join("\r");
    }

    function buildCoverBlurb(pack) {
        var parts = [];
        parts.push("Decoupage morphologique, scores par defi et corrections enrichies.");
        parts.push("Langue : " + LG.languageLabel(pack.language));
        return parts.join(" ");
    }

    function buildCoverLead(pack) {
        return "Pratique de la forme des mots avec une presentation alignee sur les autres cahiers Longoka Games.";
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
        var docRef = page.parent;
        var panel = page.rectangles.add();
        panel.itemLayer = docRef.layers.itemByName(LG.LAYERS.content);
        panel.geometricBounds = bounds;
        panel.fillColor = docRef.colors.itemByName(invert ? LG.SWATCHES.longokaDark : LG.SWATCHES.longokaPanel);
        panel.strokeColor = docRef.swatches.itemByName("None");

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
        var labelColor = invert ? docRef.colors.itemByName(LG.SWATCHES.longokaAccent) : docRef.colors.itemByName(LG.SWATCHES.longokaDark);
        var valueColor = invert ? docRef.swatches.itemByName(LG.SWATCHES.paper) : docRef.colors.itemByName(LG.SWATCHES.longokaDark);

        for (i = 0; i < groups.length; i++) {
            var top = innerTop + (i * blockHeight);
            var label = addFrame(page, [top, innerLeft, top + 5, innerRight], String(groups[i].label).toUpperCase(), LG.STYLES.p.footer);
            label.parentStory.fillColor = labelColor;

            var value = addFrame(page, [top + 5.5, innerLeft, top + 16.5, innerRight], groups[i].value, LG.STYLES.p.solutionTitle);
            value.paragraphs[0].justification = Justification.LEFT_ALIGN;
            value.parentStory.fillColor = valueColor;

            if (i < groups.length - 1) {
                var rule = page.graphicLines.add();
                rule.itemLayer = docRef.layers.itemByName(LG.LAYERS.content);
                rule.paths[0].entirePath = [[innerLeft, top + blockHeight - 1.4], [innerRight, top + blockHeight - 1.4]];
                rule.strokeWeight = 0.35;
                rule.strokeColor = invert ? docRef.colors.itemByName(LG.SWATCHES.longokaAccent) : docRef.colors.itemByName(LG.SWATCHES.longokaDark);
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

    function compactTitle(value) {
        return String(value || "")
            .replace(/_/g, " ")
            .replace(/-/g, " ")
            .replace(/\s+/g, " ")
            .replace(/^\s+|\s+$/g, "");
    }

    function sanitizeFilename(name) {
        return name.replace(/[\\\/:*?"<>|]/g, "-");
    }

    function pad2(value) {
        return value < 10 ? "0" + value : String(value);
    }
    } catch (e) {
        alert("Erreur InDesign (MorphoAnagram Book)\n\n" + String(e && (e.message || e)) + "\n\n" + String($.stack || ""));
    }
})();
