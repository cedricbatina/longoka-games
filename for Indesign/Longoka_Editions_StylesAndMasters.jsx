#target indesign

/*
 * Longoka_Editions_StylesAndMasters.jsx
 * Creates document, swatches, styles and master spreads for Longoka Games books.
 */

if (typeof LG === "undefined") {
    throw new Error("Longoka_Editions_Config.jsx doit etre charge avant StylesAndMasters.");
}

LG.createDocument = function(customConfig) {
    var cfg = customConfig || {};
    var page = cfg.page || LG.PAGE;

    var doc = app.documents.add();
    doc.documentPreferences.properties = {
        facingPages: LG.safeGet(page, "facingPages", false),
        pageWidth: LG.safeGet(page, "width", "6in"),
        pageHeight: LG.safeGet(page, "height", "9in"),
        pagesPerDocument: LG.safeGet(page, "pagesPerDocument", 1),
        documentBleedTopOffset: LG.safeGet(page, "bleed", "0.125in"),
        documentBleedBottomOffset: LG.safeGet(page, "bleed", "0.125in"),
        documentBleedInsideOrLeftOffset: LG.safeGet(page, "bleed", "0.125in"),
        documentBleedOutsideOrRightOffset: LG.safeGet(page, "bleed", "0.125in")
    };

    doc.viewPreferences.horizontalMeasurementUnits = MeasurementUnits.MILLIMETERS;
    doc.viewPreferences.verticalMeasurementUnits = MeasurementUnits.MILLIMETERS;
    doc.zeroPoint = [0, 0];

    LG.ensureLayer(doc, LG.LAYERS.content);
    LG.ensureLayer(doc, LG.LAYERS.guides);
    LG.ensureLayer(doc, LG.LAYERS.solutions);

    LG.ensureColor(doc, LG.SWATCHES.longokaDark, [73, 59, 49, 60]);
    LG.ensureColor(doc, LG.SWATCHES.longokaAccent, [9, 65, 94, 2]);
    LG.ensureColor(doc, LG.SWATCHES.longokaLight, [4, 3, 8, 0]);
    LG.ensureColor(doc, LG.SWATCHES.longokaPanel, [8, 5, 12, 0]);
    LG.ensureColor(doc, LG.SWATCHES.longokaWarm, [0, 20, 34, 0]);

    LG.ensureParagraphStyles(doc);
    LG.ensureCharacterStyles(doc);
    LG.ensureMasterSpreads(doc);
    LG.applyDocumentMargins(doc);

    return doc;
};

LG.applyDocumentMargins = function(doc) {
    var i;
    for (i = 0; i < doc.pages.length; i++) {
        LG.applyPageMargins(doc.pages[i]);
    }
};

LG.applyPageMargins = function(page) {
    page.marginPreferences.properties = {
        top: LG.PAGE.top,
        bottom: LG.PAGE.bottom,
        left: LG.PAGE.inside,
        right: LG.PAGE.outside
    };
};

LG.ensureParagraphStyles = function(doc) {
    function ensure(name, props) {
        try {
            var style = doc.paragraphStyles.itemByName(name);
            var _ = style.name;
            style.properties = props;
            return style;
        } catch (e) {
            props.name = name;
            return doc.paragraphStyles.add(props);
        }
    }

    ensure(LG.STYLES.p.bookTitle, {
        pointSize: 32,
        leading: 34,
        justification: Justification.CENTER_ALIGN,
        spaceAfter: 8,
        fillColor: LG.SWATCHES.longokaDark,
        tracking: -10,
        appliedFont: "Myriad Pro\tBold"
    });

    ensure(LG.STYLES.p.bookSubtitle, {
        pointSize: 9.2,
        leading: 11.2,
        justification: Justification.CENTER_ALIGN,
        spaceAfter: 5,
        fillColor: LG.SWATCHES.longokaAccent,
        capitalization: Capitalization.SMALL_CAPS,
        tracking: 120,
        appliedFont: "Myriad Pro\tSemibold"
    });

    ensure(LG.STYLES.p.sectionTitle, {
        pointSize: 15.8,
        leading: 18,
        spaceAfter: 8,
        fillColor: LG.SWATCHES.longokaDark,
        capitalization: Capitalization.SMALL_CAPS,
        tracking: 90,
        appliedFont: "Myriad Pro\tBold"
    });

    ensure(LG.STYLES.p.body, {
        pointSize: 10.4,
        leading: 13.8,
        fillColor: LG.SWATCHES.black,
        appliedFont: "Minion Pro\tRegular"
    });

    ensure(LG.STYLES.p.instruction, {
        pointSize: 10,
        leading: 13,
        spaceAfter: 4,
        fillColor: LG.SWATCHES.black,
        appliedFont: "Minion Pro\tRegular"
    });

    ensure(LG.STYLES.p.copyright, {
        pointSize: 8.5,
        leading: 11,
        fillColor: LG.SWATCHES.black,
        appliedFont: "Minion Pro\tRegular"
    });

    ensure(LG.STYLES.p.puzzleTitle, {
        pointSize: 15.2,
        leading: 17.5,
        fillColor: LG.SWATCHES.longokaDark,
        tracking: 8,
        appliedFont: "Myriad Pro\tBold",
        spaceAfter: 3
    });

    ensure(LG.STYLES.p.puzzleMeta, {
        pointSize: 8.6,
        leading: 10.2,
        fillColor: LG.SWATCHES.black,
        appliedFont: "Minion Pro\tItalic",
        spaceAfter: 5
    });

    ensure(LG.STYLES.p.wordListHeading, {
        pointSize: 9.3,
        leading: 11,
        fillColor: LG.SWATCHES.longokaDark,
        capitalization: Capitalization.SMALL_CAPS,
        tracking: 85,
        appliedFont: "Myriad Pro\tBold",
        spaceAfter: 3
    });

    ensure(LG.STYLES.p.wordList, {
        pointSize: 8.8,
        leading: 10.6,
        fillColor: LG.SWATCHES.black,
        appliedFont: "Minion Pro\tRegular"
    });

    ensure(LG.STYLES.p.footer, {
        pointSize: 7.4,
        leading: 8.8,
        justification: Justification.CENTER_ALIGN,
        fillColor: LG.SWATCHES.black,
        capitalization: Capitalization.SMALL_CAPS,
        tracking: 100,
        appliedFont: "Myriad Pro\tSemibold"
    });

    ensure(LG.STYLES.p.pageNumber, {
        pointSize: 8,
        leading: 9,
        justification: Justification.CENTER_ALIGN,
        fillColor: LG.SWATCHES.black,
        appliedFont: "Myriad Pro\tBold"
    });

    ensure(LG.STYLES.p.solutionTitle, {
        pointSize: 12.4,
        leading: 14.8,
        fillColor: LG.SWATCHES.longokaDark,
        capitalization: Capitalization.SMALL_CAPS,
        tracking: 45,
        appliedFont: "Myriad Pro\tBold",
        spaceAfter: 4
    });

    ensure(LG.STYLES.p.solutionBody, {
        pointSize: 9,
        leading: 11.2,
        fillColor: LG.SWATCHES.black,
        appliedFont: "Minion Pro\tRegular"
    });
};

LG.ensureCharacterStyles = function(doc) {
    function ensure(name, props) {
        try {
            var style = doc.characterStyles.itemByName(name);
            var _ = style.name;
            style.properties = props;
            return style;
        } catch (e) {
            props.name = name;
            return doc.characterStyles.add(props);
        }
    }

    ensure(LG.STYLES.c.strong, {
        fontStyle: "Bold"
    });

    ensure(LG.STYLES.c.italic, {
        fontStyle: "Italic"
    });

    ensure(LG.STYLES.c.language, {
        capitalization: Capitalization.SMALL_CAPS,
        fillColor: LG.SWATCHES.longokaDark
    });

    ensure(LG.STYLES.c.gridLetter, {
        appliedFont: "Myriad Pro\tBold",
        pointSize: 10,
        tracking: 10
    });

    ensure(LG.STYLES.c.smallCaps, {
        capitalization: Capitalization.SMALL_CAPS
    });
};

LG.ensureMasterSpreads = function(doc) {
    function ensureMaster(name) {
        try {
            var ms = doc.masterSpreads.itemByName(name);
            var _ = ms.name;
            return ms;
        } catch (e) {
            return doc.masterSpreads.add({ namePrefix: name.substr(0, 2), name: name });
        }
    }

    var front = ensureMaster(LG.MASTERS.front);
    var copyright = ensureMaster(LG.MASTERS.copyright);
    var puzzle = ensureMaster(LG.MASTERS.puzzle);
    var solutions = ensureMaster(LG.MASTERS.solutions);

    LG.decorateMasterFront(front);
    LG.decorateMasterCopyright(copyright);
    LG.decorateMasterPuzzle(puzzle);
    LG.decorateMasterSolutions(solutions);
};

LG.clearMasterPageItems = function(master) {
    var page = master.pages[0];
    while (page.pageItems.length > 0) {
        page.pageItems[0].remove();
    }
};

LG.decorateMasterFront = function(master) {
    LG.clearMasterPageItems(master);
    var page = master.pages[0];
    var doc = master.parent;
    var pageWidth = page.bounds[3];
    var pageHeight = page.bounds[2];

    var background = page.rectangles.add();
    background.geometricBounds = [0, 0, pageHeight, pageWidth];
    background.fillColor = doc.colors.itemByName(LG.SWATCHES.longokaLight);
    background.strokeColor = doc.swatches.itemByName("None");

    var accentBand = page.rectangles.add();
    accentBand.geometricBounds = [0, 0, pageHeight, 14];
    accentBand.fillColor = doc.colors.itemByName(LG.SWATCHES.longokaAccent);
    accentBand.strokeColor = doc.swatches.itemByName("None");

    var topRule = page.rectangles.add();
    topRule.geometricBounds = [0, 0, 7, pageWidth];
    topRule.fillColor = doc.colors.itemByName(LG.SWATCHES.longokaAccent);
    topRule.fillTint = 18;
    topRule.strokeColor = doc.swatches.itemByName("None");

    var darkBlock = page.rectangles.add();
    darkBlock.geometricBounds = [0, 94, pageHeight, pageWidth];
    darkBlock.fillColor = doc.colors.itemByName(LG.SWATCHES.longokaDark);
    darkBlock.fillTint = 92;
    darkBlock.strokeColor = doc.swatches.itemByName("None");

    var splitRule = page.rectangles.add();
    splitRule.geometricBounds = [20, 92, pageHeight - 16, 94];
    splitRule.fillColor = doc.colors.itemByName(LG.SWATCHES.longokaAccent);
    splitRule.fillTint = 70;
    splitRule.strokeColor = doc.swatches.itemByName("None");

    var warmPanel = page.rectangles.add();
    warmPanel.geometricBounds = [148, 20, 208, 92];
    warmPanel.fillColor = doc.colors.itemByName(LG.SWATCHES.longokaWarm);
    warmPanel.fillTint = 32;
    warmPanel.strokeColor = doc.colors.itemByName(LG.SWATCHES.longokaAccent);
    warmPanel.strokeWeight = 0.35;

    var footer = page.textFrames.add();
    footer.geometricBounds = [214, 22, 222, 130];
    footer.contents = LG.BRAND + "  |  " + LG.IMPRINT;
    footer.paragraphs[0].appliedParagraphStyle = doc.paragraphStyles.itemByName(LG.STYLES.p.footer);
};

LG.decorateMasterCopyright = function(master) {
    LG.clearMasterPageItems(master);
    var page = master.pages[0];
    var doc = master.parent;

    var band = page.rectangles.add();
    band.geometricBounds = [15, 20, 19, 132];
    band.fillColor = doc.colors.itemByName(LG.SWATCHES.longokaAccent);
    band.strokeColor = doc.swatches.itemByName("None");

    var panel = page.rectangles.add();
    panel.geometricBounds = [28, 20, 196, 132];
    panel.fillColor = doc.colors.itemByName(LG.SWATCHES.longokaPanel);
    panel.strokeColor = doc.colors.itemByName(LG.SWATCHES.longokaAccent);
    panel.strokeWeight = 0.25;
};

LG.decorateMasterPuzzle = function(master) {
    LG.clearMasterPageItems(master);
    var page = master.pages[0];
    var doc = master.parent;

    var topBand = page.rectangles.add();
    topBand.geometricBounds = [0, 0, 16, 152.4];
    topBand.fillColor = doc.colors.itemByName(LG.SWATCHES.longokaDark);
    topBand.strokeColor = doc.swatches.itemByName("None");

    var accentStrip = page.rectangles.add();
    accentStrip.geometricBounds = [16, 20, 19, 132];
    accentStrip.fillColor = doc.colors.itemByName(LG.SWATCHES.longokaAccent);
    accentStrip.strokeColor = doc.swatches.itemByName("None");

    var footerRule = page.graphicLines.add();
    footerRule.paths[0].entirePath = [[20, 210], [132, 210]];
    footerRule.strokeWeight = 0.7;
    footerRule.strokeColor = doc.colors.itemByName(LG.SWATCHES.longokaAccent);

    var footer = page.textFrames.add();
    footer.geometricBounds = [214, 20, 222, 132];
    footer.contents = SpecialCharacters.AUTO_PAGE_NUMBER;
    footer.paragraphs[0].appliedParagraphStyle = doc.paragraphStyles.itemByName(LG.STYLES.p.pageNumber);
};

LG.decorateMasterSolutions = function(master) {
    LG.clearMasterPageItems(master);
    var page = master.pages[0];
    var doc = master.parent;

    var topBand = page.rectangles.add();
    topBand.geometricBounds = [0, 0, 16, 152.4];
    topBand.fillColor = doc.colors.itemByName(LG.SWATCHES.longokaWarm);
    topBand.fillTint = 55;
    topBand.strokeColor = doc.swatches.itemByName("None");

    var header = page.textFrames.add();
    header.geometricBounds = [7, 22, 15, 130];
    header.contents = "Solutions";
    header.paragraphs[0].appliedParagraphStyle = doc.paragraphStyles.itemByName(LG.STYLES.p.footer);

    var footerRule = page.graphicLines.add();
    footerRule.paths[0].entirePath = [[20, 210], [132, 210]];
    footerRule.strokeWeight = 0.7;
    footerRule.strokeColor = doc.colors.itemByName(LG.SWATCHES.longokaAccent);

    var footer = page.textFrames.add();
    footer.geometricBounds = [214, 20, 222, 132];
    footer.contents = SpecialCharacters.AUTO_PAGE_NUMBER;
    footer.paragraphs[0].appliedParagraphStyle = doc.paragraphStyles.itemByName(LG.STYLES.p.pageNumber);
};
