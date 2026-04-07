#target indesign

/*
 * Longoka_Editions_Config.jsx
 * Core configuration and shared helpers for Longoka Games book generation.
 * ExtendScript / Adobe InDesign
 */

var LG = LG || {};

LG.VERSION = "1.2.0";
LG.BRAND = "Editions Longoka";
LG.IMPRINT = "Longoka Games";
LG.WEBSITE = "https://longoka.com";
LG.DEFAULT_LANGUAGE_LABEL = {
    kg: "Kikongo",
    ln: "Lingala"
};

LG.PAGE = {
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

LG.LAYERS = {
    content: "LG_Content",
    guides: "LG_Guides",
    solutions: "LG_Solutions"
};

LG.MASTERS = {
    front: "LG_Master_Front",
    copyright: "LG_Master_Copyright",
    puzzle: "LG_Master_Puzzle",
    solutions: "LG_Master_Solutions"
};

LG.STYLES = {
    p: {
        bookTitle: "LG_BookTitle",
        bookSubtitle: "LG_BookSubtitle",
        sectionTitle: "LG_SectionTitle",
        body: "LG_Body",
        instruction: "LG_Instruction",
        copyright: "LG_Copyright",
        puzzleTitle: "LG_PuzzleTitle",
        puzzleMeta: "LG_PuzzleMeta",
        wordListHeading: "LG_WordListHeading",
        wordList: "LG_WordList",
        footer: "LG_Footer",
        pageNumber: "LG_PageNumber",
        solutionTitle: "LG_SolutionTitle",
        solutionBody: "LG_SolutionBody"
    },
    c: {
        strong: "LG_Strong",
        italic: "LG_Italic",
        language: "LG_Language",
        gridLetter: "LG_GridLetter",
        smallCaps: "LG_SmallCaps"
    }
};

LG.SWATCHES = {
    black: "Black",
    paper: "Paper",
    longokaDark: "LG_Dark",
    longokaAccent: "LG_Accent",
    longokaLight: "LG_Light",
    longokaPanel: "LG_Panel",
    longokaWarm: "LG_Warm"
};

// Cover templates (optional). If provided, scripts can open a template and fill named frames.
// Recommended: create these INDD/IDML files manually in InDesign, then set the paths below.
LG.COVER = {
    templates: {
        standard: "", // e.g. "D:/.../Longoka_Cover_Template_Standard.indd"
        premium: ""   // e.g. "D:/.../Longoka_Cover_Template_Premium.indd"
    },
    frameNames: {
        title: "LG_Cover_Title",
        subtitle: "LG_Cover_Subtitle",
        badges: "LG_Cover_Badges",
        isbn: "LG_Cover_ISBN",
        image: "LG_Cover_Image" // rectangle graphic frame
    }
};

LG.safeGet = function(obj, key, fallback) {
    return (obj && obj.hasOwnProperty(key) && obj[key] !== null && obj[key] !== undefined)
        ? obj[key]
        : fallback;
};

LG.languageLabel = function(code) {
    return LG.DEFAULT_LANGUAGE_LABEL[code] || code || "Langue";
};

LG.ensureLayer = function(doc, name) {
    var layer;
    try {
        layer = doc.layers.itemByName(name);
        var _ = layer.name;
    } catch (e) {
        layer = doc.layers.add({ name: name });
    }
    return layer;
};

LG.ensureColor = function(doc, name, cmyk) {
    try {
        var sw = doc.colors.itemByName(name);
        var _ = sw.name;
        return sw;
    } catch (e) {
        return doc.colors.add({
            name: name,
            model: ColorModel.process,
            space: ColorSpace.cmyk,
            colorValue: cmyk
        });
    }
};

LG.getParagraphStyle = function(doc, name) {
    return doc.paragraphStyles.itemByName(name);
};

LG.getCharacterStyle = function(doc, name) {
    return doc.characterStyles.itemByName(name);
};

LG.mm = function(value) {
    return value + "mm";
};

LG.addTextFrame = function(page, bounds, contents, paraStyleName, layerName) {
    var frame = page.textFrames.add();
    if (layerName) {
        frame.itemLayer = page.parent.layers.itemByName(layerName);
    }
    frame.geometricBounds = bounds;
    frame.contents = contents || "";
    if (paraStyleName && frame.paragraphs.length > 0) {
        try {
            frame.paragraphs.everyItem().appliedParagraphStyle = frame.parent.parent.paragraphStyles.itemByName(paraStyleName);
        } catch (e) {}
    }
    return frame;
};

LG.readJsonFile = function(fileObj) {
    if (!fileObj || !fileObj.exists) {
        throw new Error("JSON introuvable.");
    }
    fileObj.encoding = "UTF-8";
    fileObj.open("r");
    var raw = fileObj.read();
    fileObj.close();
    return LG.parseJson(raw);
};

LG.parseJson = function(raw) {
    var text = String(raw || "");
    text = text.replace(/^\uFEFF/, "");
    if (typeof JSON !== "undefined" && JSON.parse) {
        return JSON.parse(text);
    }
    return eval("(" + text + ")");
};

LG.validateWordSearchPack = function(pack) {
    if (!pack) {
        throw new Error("Pack JSON vide.");
    }
    if (pack.schema !== "longoka.wordsearch.pack.v1") {
        throw new Error("Schema inattendu: " + pack.schema);
    }
    if (!pack.puzzles || !pack.puzzles.length) {
        throw new Error("Le pack ne contient aucun puzzle.");
    }
};

LG.validateCrosswordPack = function(pack) {
    if (!pack) {
        throw new Error("Pack JSON vide.");
    }
    if (pack.format !== "longoka.crossword.pack.v1") {
        throw new Error("Format inattendu: " + pack.format);
    }
    if (!pack.puzzles || !pack.puzzles.length) {
        throw new Error("Le pack ne contient aucun puzzle.");
    }
};

LG.validateArrowwordPack = function(pack) {
    if (!pack) {
        throw new Error("Pack JSON vide.");
    }
    if (pack.format !== "longoka.arrowword.pack.v1") {
        throw new Error("Format inattendu: " + pack.format);
    }
    if (!pack.puzzles || !pack.puzzles.length) {
        throw new Error("Le pack ne contient aucun puzzle.");
    }
    var p0 = pack.puzzles[0];
    if (!p0.cells || !p0.cells.length) {
        throw new Error("Pack arrowword: matrice cells manquante.");
    }
};

LG.validateMorphoAnagramPack = function(pack) {
    if (!pack) {
        throw new Error("Pack JSON vide.");
    }
    if (pack.format !== "longoka.morpho-anagram.pack.v1") {
        throw new Error("Format inattendu: " + pack.format);
    }
    if (!pack.puzzles || !pack.puzzles.length) {
        throw new Error("Le pack ne contient aucun puzzle.");
    }
};

LG.joinWordEntries = function(entries) {
    var lines = [];
    var i, entry, label, translation;
    for (i = 0; i < entries.length; i++) {
        entry = entries[i];
        label = entry.display || entry.base || entry.word || "";
        translation = entry.translation || entry.clue || "";
        lines.push(label + (translation ? " — " + translation : ""));
    }
    return lines.join("\r");
};

LG.formatDirections = function() {
    return "Les mots peuvent apparaitre horizontalement, verticalement ou en diagonale, dans plusieurs sens.";
};

LG.packGameCode = function(pack) {
    var marker = String(pack && (pack.schema || pack.format || "")).toLowerCase();
    if (marker.indexOf("wordsearch") >= 0) return "WS";
    if (marker.indexOf("crossword") >= 0) return "CW";
    if (marker.indexOf("arrowword") >= 0) return "AW";
    if (marker.indexOf("memory") >= 0) return "MM";
    if (marker.indexOf("domino") >= 0) return "DM";
    if (marker.indexOf("scrabble") >= 0) return "SC";
    if (marker.indexOf("anagram") >= 0) return "AN";
    return "BK";
};

LG.sanitizeCodePart = function(value, fallback) {
    var part = String(value || fallback || "STANDARD").toUpperCase();
    part = part.replace(/[^A-Z0-9]+/g, "-");
    part = part.replace(/^-+|-+$/g, "");
    return part || String(fallback || "STANDARD").toUpperCase();
};

LG.bookCodeFromPack = function(pack) {
    if (pack && pack.meta) {
        if (pack.meta.book && pack.meta.book.bookCode) {
            return String(pack.meta.book.bookCode).replace(/^\s+|\s+$/g, "");
        }
        var editorial = LG.safeGet(pack.meta, "bookCode", "") || LG.safeGet(pack.meta, "provisionalId", "");
        if (editorial) {
            return String(editorial).replace(/^\s+|\s+$/g, "");
        }
    }
    var lang = LG.sanitizeCodePart(pack.language || "xx", "XX");
    var game = LG.packGameCode(pack);
    var size = "12X12";
    var mode = "STANDARD";
    if (pack.puzzles && pack.puzzles.length) {
        var p = pack.puzzles[0];
        if (p.rows && p.cols) {
            size = String(p.rows) + "X" + String(p.cols);
        }
        mode = LG.sanitizeCodePart(p.mode || p.theme || "standard", "STANDARD");
    }
    return "LG-" + lang + "-" + game + "-" + mode + "-" + size + "-V1";
};

LG.isbnFromPack = function(pack) {
    if (pack && pack.meta && pack.meta.book && pack.meta.book.isbn) {
        return String(pack.meta.book.isbn).replace(/^\s+|\s+$/g, "");
    }
    if (pack && pack.meta && pack.meta.isbn) {
        return String(pack.meta.isbn).replace(/^\s+|\s+$/g, "");
    }
    return "";
};

LG.publishedAtFromPack = function(pack) {
    if (pack && pack.meta && pack.meta.book) {
        var v = pack.meta.book.publishedAt || pack.meta.book.generatedOn || pack.meta.book.createdAt || "";
        return String(v || "").replace(/^\s+|\s+$/g, "");
    }
    if (pack && pack.meta && (pack.meta.generatedOn || pack.meta.createdAt)) {
        return String(pack.meta.generatedOn || pack.meta.createdAt || "").replace(/^\s+|\s+$/g, "");
    }
    return "";
};

/** Langue des sens / traductions pour l'impression : "fr" (defaut) ou "en" (meta.book.meaningLanguage). */
LG.meaningLanguageFromPack = function(pack) {
    var raw = "";
    if (pack && pack.meta && pack.meta.book) {
        raw = pack.meta.book.meaningLanguage || "";
    }
    if (!raw && pack && pack.meta) {
        raw = pack.meta.meaningLanguage || "";
    }
    if (!raw && pack) {
        raw = pack.meaningLanguage || "";
    }
    raw = String(raw).trim().toLowerCase();
    if (raw.indexOf("en") === 0) {
        return "en";
    }
    return "fr";
};

/** Une seule colonne sens selon la langue du livre (listes et corrections). */
LG.entryMeaningForBook = function(entry, pack) {
    var lang = LG.meaningLanguageFromPack(pack);
    if (lang === "en") {
        return String(entry.translationEn || entry.translation || entry.clue || "").trim();
    }
    return String(entry.translation || entry.clue || "").trim();
};

LG.challengeMeaningForBook = function(ch, pack) {
    var lang = LG.meaningLanguageFromPack(pack);
    if (lang === "en") {
        return String(ch.translationEn || ch.translation || "").trim();
    }
    return String(ch.translation || "").trim();
};

LG.entryRefMeaningForBook = function(er, pack) {
    var lang = LG.meaningLanguageFromPack(pack);
    if (lang === "en") {
        return String(er.translationEn || er.translation || "").trim();
    }
    return String(er.translation || "").trim();
};

LG.bookTitleFromPack = function(pack) {
    var t = "";
    if (pack && pack.meta && pack.meta.book && pack.meta.book.title) {
        t = pack.meta.book.title;
    } else if (pack && pack.title) {
        t = pack.title;
    }
    return String(t || "").replace(/^\s+|\s+$/g, "");
};

LG.bookSubtitleFromPack = function(pack) {
    if (pack && pack.meta && pack.meta.book && pack.meta.book.subtitle) {
        return String(pack.meta.book.subtitle).replace(/^\s+|\s+$/g, "");
    }
    return "";
};

LG.modeLabelFromPack = function(pack) {
    if (pack && pack.meta) {
        var profile = LG.safeGet(pack.meta, "profileLabel", "");
        if (profile) {
            return String(profile).replace(/^\s+|\s+$/g, "");
        }
    }
    var raw = "";
    if (pack && pack.puzzles && pack.puzzles.length) {
        raw = pack.puzzles[0].mode || pack.puzzles[0].theme || "";
    }
    raw = String(raw || "")
        .replace(/_/g, " ")
        .replace(/-/g, " ")
        .replace(/\s+/g, " ")
        .replace(/^\s+|\s+$/g, "");
    return raw || "selection lexicale";
};

LG.tierLabelFromPack = function(pack) {
    var raw = LG.safeGet(pack, "editionTier", "") || LG.safeGet(pack, "tier", "");
    if (!raw && pack && pack.meta) {
        raw = LG.safeGet(pack.meta, "editionTier", "") || LG.safeGet(pack.meta, "editionLabel", "");
    }
    if (!raw && pack && pack.puzzles && pack.puzzles.length) {
        raw = LG.safeGet(pack.puzzles[0], "editionTier", "") || LG.safeGet(pack.puzzles[0], "tier", "");
    }
    raw = String(raw || "premium")
        .replace(/_/g, " ")
        .replace(/-/g, " ")
        .replace(/\s+/g, " ")
        .replace(/^\s+|\s+$/g, "");
    return raw || "premium";
};

LG.difficultyLabelFromPack = function(pack) {
    var raw = LG.safeGet(pack, "difficulty", "");
    if (!raw && pack && pack.meta) {
        raw = LG.safeGet(pack.meta, "difficulty", "");
    }
    if (!raw && pack && pack.puzzles && pack.puzzles.length) {
        raw = pack.puzzles[0].difficulty || "";
    }
    raw = String(raw || "expert")
        .replace(/_/g, " ")
        .replace(/-/g, " ")
        .replace(/\s+/g, " ")
        .replace(/^\s+|\s+$/g, "");
    return raw || "expert";
};

LG.collectionLabelFromPack = function(pack) {
    var code = String(pack && pack.language || "kg").toUpperCase();
    return "Collection Atelier " + code;
};

LG.volumeNumberFromPack = function(pack) {
    var seed = String(pack && pack.packId || "");
    var match = seed.match(/(\d{4})-(\d{2})-(\d{2})/);
    if (match) {
        return match[2] + match[3];
    }
    return "01";
};

LG.volumeLabelFromPack = function(pack) {
    return "Volume " + LG.volumeNumberFromPack(pack);
};

LG.puzzleCountFromPack = function(pack) {
    return (pack && pack.puzzles && pack.puzzles.length) ? pack.puzzles.length : 0;
};

LG.wordCountFromPuzzle = function(puzzle) {
    return (puzzle && puzzle.entries && puzzle.entries.length) ? puzzle.entries.length : 0;
};
