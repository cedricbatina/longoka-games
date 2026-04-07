package com.longoka.games.app;

/**
 * Tool CLI pour générer un pack JSON de mots fléchés Kikongo (mixte).
 */
public final class KikongoMixedArrowwordJsonExportTool {

  private KikongoMixedArrowwordJsonExportTool() {
    // utilitaire
  }

  public static void main(String[] args) throws Exception {
    System.out.println("== Export JSON : Mots fléchés Kikongo (mixte) ==");

    int puzzleCount = 48;
    int rows = 12;
    int cols = 12;
    int maxEntries = 12;
    String meaningLanguageCode = "fr";
    String mode = "mixed";
    String languageCode = "kg";
    String outputPath = "target/kikongo-mixed-arrowword-book.v1.json";

    KikongoArrowwordPackExporter.exportPack(
        mode,
        puzzleCount,
        rows,
        cols,
        maxEntries,
        meaningLanguageCode,
        languageCode,
        "kg-arrow-mixed",
        "Kikongo – mots fléchés (mixte)",
        "Pack généré automatiquement : " + puzzleCount + " grilles " + rows + "x" + cols + " (mixte).",
        outputPath);
  }
}
