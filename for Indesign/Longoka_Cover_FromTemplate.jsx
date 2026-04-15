#target indesign

/*
 * Longoka_Cover_FromTemplate.jsx
 * Opens a cover template (INDD/IDML) and fills it from a pack JSON (meta.book.*).
 *
 * Template contract (recommended):
 * - Text frames (name them exactly):
 *   - LG_Cover_Title
 *   - LG_Cover_Subtitle
 *   - LG_Cover_Badges
 *   - LG_Cover_ISBN
 * - Image frame (rectangle graphic frame):
 *   - LG_Cover_Image
 *
 * The script will:
 * - pick premium/standard template based on meta.book.printVariant
 * - set title/subtitle/badges/isbn
 * - optionally place meta.book.coverImage into LG_Cover_Image if provided
 * - export PNG preview and save an .indd copy next to the JSON
 */

(function () {
    var thisFile = File($.fileName);
    var baseFolder = (typeof LG_BASE_FOLDER !== "undefined" && LG_BASE_FOLDER)
        ? Folder(LG_BASE_FOLDER)
        : thisFile.parent;

    $.evalFile(File(baseFolder + "/Longoka_Editions_Config.jsx"));
    $.evalFile(File(baseFolder + "/Longoka_Editions_StylesAndMasters.jsx"));

    var jsonFile = File.openDialog("Choisir un pack JSON Longoka (cover template)", "JSON:*.json");
    if (!jsonFile) {
        LG.uiCancelled();
        return;
    }

    var pack = LG.readJsonFile(jsonFile);
    var book = (pack && pack.meta && pack.meta.book) ? pack.meta.book : {};

    var printVariant = String(book.printVariant || "").toLowerCase();
    var isPremium = printVariant.indexOf("premium") >= 0
        || String(pack.meta && (pack.meta.editionTier || pack.meta.editionLabel || "")).toLowerCase().indexOf("premium") >= 0;

    var templatePath = isPremium ? (LG.COVER.templates.premium || "") : (LG.COVER.templates.standard || "");
    templatePath = String(templatePath || "").replace(/^\s+|\s+$/g, "");

    if (!templatePath) {
        alert(
            "Chemin de template manquant.\n\n" +
            "Dans Longoka_Editions_Config.jsx, renseigne:\n" +
            "  LG.COVER.templates.standard et/ou LG.COVER.templates.premium\n\n" +
            "Puis relance le script."
        );
        return;
    }

    var templateFile = File(templatePath);
    if (!templateFile.exists) {
        alert("Template introuvable:\n" + templateFile.fsName);
        return;
    }

    var doc = app.open(templateFile, false);

    // Ensure LG styles & swatches exist even in template documents
    try { LG.ensureLayer(doc, LG.LAYERS.content); } catch (e) {}
    try { LG.ensureParagraphStyles(doc); } catch (e) {}
    try { LG.ensureCharacterStyles(doc); } catch (e) {}
    try { LG.ensureColor(doc, LG.SWATCHES.longokaDark, [73, 59, 49, 60]); } catch (e) {}
    try { LG.ensureColor(doc, LG.SWATCHES.longokaAccent, [9, 65, 94, 2]); } catch (e) {}
    try { LG.ensureColor(doc, LG.SWATCHES.longokaLight, [4, 3, 8, 0]); } catch (e) {}
    try { LG.ensureColor(doc, LG.SWATCHES.longokaPanel, [8, 5, 12, 0]); } catch (e) {}
    try { LG.ensureColor(doc, LG.SWATCHES.longokaWarm, [0, 20, 34, 0]); } catch (e) {}

    var title = LG.bookTitleFromPack(pack) || (pack.title || "");
    var subtitle = LG.bookSubtitleFromPack(pack) || "";
    var isbn = LG.isbnFromPack(pack) || "";
    var meaningLang = LG.meaningLanguageFromPack(pack);

    var badges = [];
    badges.push(LG.languageLabel(pack.language || ""));
    badges.push(meaningLang === "en" ? "EN" : "FR");
    if (book.trimSize) badges.push(String(book.trimSize));
    if (book.printVariant) badges.push(String(book.printVariant));
    badges.push(LG.bookCodeFromPack(pack));

    setTextFrame(doc, LG.COVER.frameNames.title, title, LG.STYLES.p.bookTitle);
    setTextFrame(doc, LG.COVER.frameNames.subtitle, subtitle, LG.STYLES.p.bookSubtitle);
    setTextFrame(doc, LG.COVER.frameNames.badges, badges.join("  |  "), LG.STYLES.p.footer);
    setTextFrame(doc, LG.COVER.frameNames.isbn, isbn ? ("ISBN : " + isbn) : "", LG.STYLES.p.footer);

    var imgPath = String(book.coverImage || "").replace(/^\s+|\s+$/g, "");
    if (imgPath) {
        placeImageIntoFrame(doc, LG.COVER.frameNames.image, imgPath);
    }

    var baseName = sanitizeFilename(LG.bookCodeFromPack(pack) + "-cover");
    var outIndd = File(jsonFile.parent + "/" + baseName + ".indd");
    var outPng = File(jsonFile.parent + "/" + baseName + ".png");

    doc.save(outIndd);
    exportCoverPng(doc, outPng);

    alert("Couverture (template) exportee :\n- " + outIndd.fsName + "\n- " + outPng.fsName);

    function setTextFrame(doc, frameName, text, paraStyleName) {
        if (!frameName) return;
        var tf = findPageItemByName(doc, frameName);
        if (!tf || !(tf instanceof TextFrame)) {
            return;
        }
        tf.contents = String(text || "");
        if (paraStyleName && tf.paragraphs.length > 0) {
            try {
                tf.paragraphs.everyItem().appliedParagraphStyle = doc.paragraphStyles.itemByName(paraStyleName);
            } catch (e) {}
        }
    }

    function placeImageIntoFrame(doc, frameName, imgPath) {
        var f = File(imgPath);
        if (!f.exists) return;
        var frame = findPageItemByName(doc, frameName);
        if (!frame) return;
        try {
            frame.place(f);
            frame.fit(FitOptions.FILL_PROPORTIONALLY);
            frame.fit(FitOptions.CENTER_CONTENT);
        } catch (e) {}
    }

    function findPageItemByName(doc, name) {
        var n = String(name || "");
        if (!n) return null;
        // Search across all pageItems (includes text frames, rectangles, groups, etc.)
        var items = doc.pageItems;
        var i;
        for (i = 0; i < items.length; i++) {
            try {
                if (items[i].name === n) return items[i];
            } catch (e) {}
        }
        return null;
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
})();

