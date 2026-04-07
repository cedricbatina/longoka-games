package com.longoka.games.app;

/**
 * Tool CLI pour générer un pack JSON de mots fléchés Kikongo (verbes).
 */
public final class KikongoVerbArrowwordJsonExportTool {

  private KikongoVerbArrowwordJsonExportTool() {
    // utilitaire
  }

  public static void main(String[] args) throws Exception {
    System.out.println("== Export JSON : Mots fléchés Kikongo (verbes) ==");

    int puzzleCount = 48;
    int rows = 12;
    int cols = 12;
    int maxEntries = 12;
    String meaningLanguageCode = "fr";
    String mode = "verbs";
    String languageCode = "kg";
    String outputPath = "target/kikongo-verbs-arrowword-book.v1.json";

    KikongoArrowwordPackExporter.exportPack(
        mode,
        puzzleCount,
        rows,
        cols,
        maxEntries,
        meaningLanguageCode,
        languageCode,
        "kg-arrow-verbs",
        "Kikongo – mots fléchés (verbes)",
        "Pack généré automatiquement : " + puzzleCount + " grilles " + rows + "x" + cols + " (verbes).",
        outputPath);
  }
}
