// generate_kikongo_wordsearch_book.jsx
// Script InDesign pour générer un livre de mots mêlés à partir d'un JSON Longoka

#target "InDesign"

(function () {
    // --- 1) Choisir le fichier JSON ---
    var jsonFile = File.openDialog("Choisis un fichier JSON Longoka (kikongo-*-wordsearch-book.v1.json)", "*.json");
    if (!jsonFile) {
        alert("Aucun fichier sélectionné. Script annulé.");
        return;
    }

    if (!jsonFile.exists) {
        alert("Le fichier sélectionné n'existe pas.");
        return;
    }

    // --- 2) Lire le JSON ---
    jsonFile.open("r");
    var jsonStr = jsonFile.read();
    jsonFile.close();

    var pack;
    try {
        // ExtendScript n'a pas toujours JSON.parse, donc on sécurise
        if (typeof JSON !== "undefined" && JSON.parse) {
            pack = JSON.parse(jsonStr);
        } else {
            // fallback un peu sale mais efficace si le JSON est propre
            pack = eval("(" + jsonStr + ")");
        }
    } catch (e) {
        alert("Impossible de parser le JSON :\n" + e);
        return;
    }

    if (!pack || !pack.puzzles || !pack.puzzles.length) {
        alert("Le JSON ne contient pas de puzzles ('pack.puzzles').");
        return;
    }

    // --- 3) Créer le document InDesign ---
    var doc = app.documents.add();
    doc.documentPreferences.pageWidth = "210mm";
    doc.documentPreferences.pageHeight = "297mm";
    doc.documentPreferences.pageOrientation = PageOrientation.PORTRAIT;

    // Un peu de marges
    doc.documentPreferences.top = "15mm";
    doc.documentPreferences.bottom = "15mm";
    doc.documentPreferences.left = "15mm";
    doc.documentPreferences.right = "15mm";

    // Définir une police monospaced si dispo (sinon InDesign prendra la police par défaut)
    var monoFontName = "Courier New";
    var titleSize = 18;
    var gridFontSize = 12;
    var listFontSize = 11;

    // --- 4) Boucle sur les puzzles ---
    for (var i = 0; i < pack.puzzles.length; i++) {
        var puzzle = pack.puzzles[i];

        // Créer une page (la première est déjà là, mais on simplifie)
        var page;
        if (i === 0) {
            page = doc.pages[0];
        } else {
            page = doc.pages.add();
        }

        // Récupérer les marges
        var margins = page.marginPreferences;
        var pageBounds = page.bounds; // [y1, x1, y2, x2]
        var top = margins.top;
        var left = margins.left;
        var right = margins.right;
        var bottom = margins.bottom;

        var pageWidth = pageBounds[3] - pageBounds[1];
        var pageHeight = pageBounds[2] - pageBounds[0];

        var contentWidth = pageWidth - left - right;
        var contentHeight = pageHeight - top - bottom;

        // --- 4.1) Titre ---
        var titleFrame = page.textFrames.add();
        titleFrame.geometricBounds = [
            top,
            left,
            top + 15, // hauteur arbitraire
            left + contentWidth
        ];

        var puzzleNumber = (i + 1);
        var puzzleTitle = puzzle.title ? puzzle.title : "Mots mêlés";
        titleFrame.contents = "Grille " + puzzleNumber + " – " + puzzleTitle;

        var titlePar = titleFrame.paragraphs[0];
        titlePar.pointSize = titleSize;
        titlePar.justification = Justification.CENTER_ALIGN;

        // --- 4.2) Grille (tableau 12x12) ---
        var gridTop = top + 20; // sous le titre
        var gridHeight = contentHeight * 0.45; // ~45% de la page
        var gridWidth = contentWidth;

        var gridFrame = page.textFrames.add();
        gridFrame.geometricBounds = [
            gridTop,
            left,
            gridTop + gridHeight,
            left + gridWidth
        ];

        // On suppose que puzzle.grid est une liste de strings
        var rows = puzzle.rows || (puzzle.grid ? puzzle.grid.length : 0);
        var cols = puzzle.cols || (rows > 0 ? puzzle.grid[0].length : 0);

        if (!rows || !cols || !puzzle.grid) {
            gridFrame.contents = "[Grille non disponible]";
        } else {
            // Créer un tableau vide
            gridFrame.contents = "";
            var table = gridFrame.tables.add();
            table.columnCount = cols;
            table.bodyRowCount = rows;

            // Remplir le tableau avec les lettres de la grille
            for (var r = 0; r < rows; r++) {
                var line = puzzle.grid[r];
                for (var c = 0; c < cols; c++) {
                    var letter = (c < line.length) ? line.charAt(c) : "";
                    table.rows[r].cells[c].contents = letter;
                }
            }

            // Mise en forme de base
            try {
                table.appliedFont = monoFontName;
            } catch (eFont) {
                // si la police n'existe pas, on laisse la police par défaut
            }
            table.pointSize = gridFontSize;
            table.cells.everyItem().justification = Justification.CENTER_ALIGN;
        }

        // --- 4.3) Liste des mots (Kikongo + FR) ---
        var listTop = gridTop + gridHeight + 5;
        var listHeight = contentHeight - (gridHeight + 25); // un peu de marge

        var listFrame = page.textFrames.add();
        listFrame.geometricBounds = [
            listTop,
            left,
            top + contentHeight,
            left + contentWidth
        ];

        if (!puzzle.entries || !puzzle.entries.length) {
            listFrame.contents = "[Aucune entrée]";
        } else {
            // Créer un tableau à 2 colonnes (Kikongo / FR)
            listFrame.contents = "";
            var listTable = listFrame.tables.add();
            listTable.columnCount = 2;
            listTable.bodyRowCount = puzzle.entries.length;

            for (var e = 0; e < puzzle.entries.length; e++) {
                var entry = puzzle.entries[e];
                var kikongo = entry.display || entry.base || "";
                var fr = entry.translation || "";

                listTable.rows[e].cells[0].contents = kikongo;
                listTable.rows[e].cells[1].contents = fr;
            }

            listTable.columns[0].width = contentWidth * 0.4;
            listTable.columns[1].width = contentWidth * 0.6;

            listTable.pointSize = listFontSize;

            try {
                listTable.appliedFont = monoFontName;
            } catch (eFont2) {
                // idem, fallback sur la police par défaut
            }
        }
    }

    alert("Génération terminée : " + pack.puzzles.length + " grilles importées.");

})();
