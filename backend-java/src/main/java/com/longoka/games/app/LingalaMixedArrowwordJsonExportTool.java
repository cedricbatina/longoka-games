package com.longoka.games.app;

/**
 * Tool CLI pour générer un pack JSON de mots fléchés Lingala (mixte).
 */
public final class LingalaMixedArrowwordJsonExportTool {

  private LingalaMixedArrowwordJsonExportTool() {
    // utilitaire
  }

  public static void main(String[] args) throws Exception {
    System.out.println("== Export JSON : Mots fléchés Lingala (mixte) ==");

    int puzzleCount = 48;
    int rows = 12;
    int cols = 12;
    int maxEntries = 12;
    String meaningLanguageCode = "fr";
    String mode = "mixed";
    String languageCode = "ln";
    String outputPath = "target/lingala-mixed-arrowword-book.v1.json";

    LingalaArrowwordPackExporter.exportPack(
        mode,
        puzzleCount,
        rows,
        cols,
        maxEntries,
        meaningLanguageCode,
        languageCode,
        "ln-arrow-mixed",
        "Lingala – mots fléchés (mixte)",
        "Pack généré automatiquement : " + puzzleCount + " grilles " + rows + "x" + cols + " (mixte).",
        outputPath);
  }
}
