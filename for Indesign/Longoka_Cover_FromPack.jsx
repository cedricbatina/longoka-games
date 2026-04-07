#target indesign

/*
 * Longoka_Cover_FromPack.jsx
 * Generates a simple, consistent Longoka Games cover from a pack JSON.
 *
 * Notes:
 * - This script creates a new document (front cover only) using LG styles.
 * - For "editorial-grade" covers, use an InDesign template (INDD/IDML) and let a script fill it:
 *   see Longoka_Cover_FromTemplate.jsx.
 */

(function () {
    var thisFile = File($.fileName);
    var baseFolder = (typeof LG_BASE_FOLDER !== "undefined" && LG_BASE_FOLDER)
        ? Folder(LG_BASE_FOLDER)
        : thisFile.parent;

    $.evalFile(File(baseFolder + "/Longoka_Editions_Config.jsx"));
    $.evalFile(File(baseFolder + "/Longoka_Editions_StylesAndMasters.jsx"));

    var jsonFile = File.openDialog("Choisir un pack JSON Longoka (cover)", "JSON:*.json");
    if (!jsonFile) {
        alert("Operation annulee.");
        return;
    }

    var pack = LG.readJsonFile(jsonFile);

    var book = (pack && pack.meta && pack.meta.book) ? pack.meta.book : {};
    var printVariant = String(book.printVariant || "").toLowerCase();
    var isPremium = printVariant.indexOf("premium") >= 0 || String(pack.meta && (pack.meta.editionTier || pack.meta.editionLabel || "")).toLowerCase().indexOf("premium") >= 0;
    var meaningLang = LG.meaningLanguageFromPack(pack);

    // Trim size support (minimal): default 6x9in.
    var pageCfg = {
        width: "6in",
        height: "9in",
        facingPages: false,
        pagesPerDocument: 1,
        top: "0.58in",
        bottom: "0.62in",
        inside: "0.56in",
        outside: "0.56in",
        bleed: "0.125in"
    };

    var trim = String(book.trimSize || "").toLowerCase();
    if (trim === "6x9in" || trim === "6x9") {
        // already default
    }

    var doc = LG.createDocument({ page: pageCfg });
    var page = doc.pages[0];
    page.appliedMaster = doc.masterSpreads.itemByName(LG.MASTERS.front);

    applyCoverPalette(doc, pack, isPremium);
    drawCoverBackground(doc, page, pack, isPremium);

    var titleText = LG.bookTitleFromPack(pack) || (pack.title || "Longoka Games");
    var subText = LG.bookSubtitleFromPack(pack) || buildFallbackSubtitle(pack, meaningLang, isPremium);

    // Title block
    var titleFrame = addFrame(page, [58, 22, 104, 132], compactTitle(titleText), LG.STYLES.p.bookTitle);
    titleFrame.paragraphs[0].justification = Justification.LEFT_ALIGN;

    if (subText) {
        var sub = addFrame(page, [108, 22, 120, 132], String(subText), LG.STYLES.p.bookSubtitle);
        sub.paragraphs[0].justification = Justification.LEFT_ALIGN;
    }

    // Footer / badges
    var badges = [];
    badges.push(LG.languageLabel(pack.language || ""));
    badges.push(meaningLang === "en" ? "EN" : "FR");
    if (book.trimSize) badges.push(String(book.trimSize));
    if (book.printVariant) badges.push(String(book.printVariant));
    badges.push(LG.bookCodeFromPack(pack));

    var footer = addFrame(page, [192, 22, 205, 132], badges.join("  |  "), LG.STYLES.p.footer);
    footer.paragraphs[0].justification = Justification.LEFT_ALIGN;

    // Optional image placement
    var imgPath = String(book.coverImage || "");
    if (imgPath) {
        tryPlaceCoverImage(doc, page, imgPath);
    }

    // Exports
    var baseName = sanitizeFilename(LG.bookCodeFromPack(pack) + "-cover");
    var outPng = File(jsonFile.parent + "/" + baseName + ".png");
    exportCoverPng(doc, outPng);

    // Save .indd next to JSON
    var outIndd = File(jsonFile.parent + "/" + baseName + ".indd");
    doc.save(outIndd);

    alert("Couverture creee :\n- " + outIndd.fsName + "\n- " + outPng.fsName);

    function addFrame(page, bounds, contents, styleName) {
        var frame = page.textFrames.add();
        frame.itemLayer = page.parent.layers.itemByName(LG.LAYERS.content);
        frame.geometricBounds = bounds;
        frame.contents = contents || "";
        try {
            frame.paragraphs.everyItem().appliedParagraphStyle = page.parent.paragraphStyles.itemByName(styleName);
        } catch (e) {}
        return frame;
    }

    function applyCoverPalette(doc, pack, isPremium) {
        // Minimal variant palette (keeps existing swatch names).
        // Premium: darker accent; Standard: lighter.
        try {
            if (isPremium) {
                LG.ensureColor(doc, LG.SWATCHES.longokaAccent, [20, 70, 95, 5]);
                LG.ensureColor(doc, LG.SWATCHES.longokaWarm, [0, 25, 40, 0]);
            } else {
                LG.ensureColor(doc, LG.SWATCHES.longokaAccent, [9, 65, 94, 2]);
                LG.ensureColor(doc, LG.SWATCHES.longokaWarm, [0, 20, 34, 0]);
            }
        } catch (e) {}
    }

    function drawCoverBackground(doc, page, pack, isPremium) {
        // Full-bleed background + a geometric band.
        var bg = page.rectangles.add();
        bg.itemLayer = doc.layers.itemByName(LG.LAYERS.content);
        bg.geometricBounds = [-10, -10, 230, 160]; // generous bleed in mm-like space
        bg.fillColor = doc.colors.itemByName(LG.SWATCHES.longokaLight);
        bg.strokeColor = doc.swatches.itemByName("None");

        var band = page.rectangles.add();
        band.itemLayer = doc.layers.itemByName(LG.LAYERS.content);
        band.geometricBounds = [0, 0, 36, 160];
        band.fillColor = doc.colors.itemByName(isPremium ? LG.SWATCHES.longokaDark : LG.SWATCHES.longokaPanel);
        band.fillTint = isPremium ? 100 : 85;
        band.strokeColor = doc.swatches.itemByName("None");

        var chip = page.rectangles.add();
        chip.itemLayer = doc.layers.itemByName(LG.LAYERS.content);
        chip.geometricBounds = [22, 22, 30, 78];
        chip.fillColor = doc.colors.itemByName(LG.SWATCHES.longokaAccent);
        chip.strokeColor = doc.swatches.itemByName("None");
    }

    function buildFallbackSubtitle(pack, meaningLang, isPremium) {
        var parts = [];
        parts.push(isPremium ? "Edition premium" : "Edition standard");
        parts.push(LG.modeLabelFromPack(pack));
        parts.push("sens " + (meaningLang === "en" ? "anglais" : "francais"));
        return parts.join(" · ");
    }

    function tryPlaceCoverImage(doc, page, imgPath) {
        var f = File(imgPath);
        if (!f.exists) return;
        var frame = page.rectangles.add();
        frame.itemLayer = doc.layers.itemByName(LG.LAYERS.content);
        frame.geometricBounds = [126, 22, 186, 132];
        frame.strokeColor = doc.swatches.itemByName("None");
        frame.fillColor = doc.colors.itemByName(LG.SWATCHES.paper);
        frame.place(f);
        frame.fit(FitOptions.FILL_PROPORTIONALLY);
        frame.fit(FitOptions.CENTER_CONTENT);
    }

    function exportCoverPng(doc, outFile) {
        app.pngExportPreferences.exportResolution = 144;
        app.pngExportPreferences.pngQuality = PNGQualityEnum.HIGH;
        app.pngExportPreferences.pngExportRange = ExportRangeOrAllPages.EXPORT_ALL;
        app.pngExportPreferences.transparentBackground = false;
        doc.exportFile(ExportFormat.PNG_FORMAT, outFile, false);
    }

    function sanitizeFilename(name) {
        return String(name || "longoka-cover")
            .replace(/[^a-z0-9\-_.]+/gi, "-")
            .replace(/-+/g, "-")
            .replace(/^-+|-+$/g, "");
    }

    function compactTitle(text) {
        var s = String(text || "");
        s = s.replace(/\r|\n/g, " ").replace(/\s+/g, " ").replace(/^\s+|\s+$/g, "");
        return s;
    }
})();

