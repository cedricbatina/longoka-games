package com.longoka.games.app;

/**
 * Tool CLI pour générer un pack JSON de mots fléchés Lingala (verbes).
 */
public final class LingalaVerbArrowwordJsonExportTool {

  private LingalaVerbArrowwordJsonExportTool() {
    // utilitaire
  }

  public static void main(String[] args) throws Exception {
    System.out.println("== Export JSON : Mots fléchés Lingala (verbes) ==");

    int puzzleCount = 48;
    int rows = 12;
    int cols = 12;
    int maxEntries = 12;
    String meaningLanguageCode = "fr";
    String mode = "verbs";
    String languageCode = "ln";
    String outputPath = "target/lingala-verbs-arrowword-book.v1.json";

    LingalaArrowwordPackExporter.exportPack(
        mode,
        puzzleCount,
        rows,
        cols,
        maxEntries,
        meaningLanguageCode,
        languageCode,
        "ln-arrow-verbs",
        "Lingala – mots fléchés (verbes)",
        "Pack généré automatiquement : " + puzzleCount + " grilles " + rows + "x" + cols + " (verbes).",
        outputPath);
  }
}
