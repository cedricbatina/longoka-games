package com.longoka.games.app;

/**
 * Tool CLI pour générer un pack JSON de mots fléchés Kikongo (noms).
 */
public final class KikongoNounArrowwordJsonExportTool {

  private KikongoNounArrowwordJsonExportTool() {
    // utilitaire
  }

  public static void main(String[] args) throws Exception {
    System.out.println("== Export JSON : Mots fléchés Kikongo (noms) ==");

    int puzzleCount = 48;
    int rows = 12;
    int cols = 12;
    int maxEntries = 12;
    String meaningLanguageCode = "fr";
    String mode = "nouns";
    String languageCode = "kg";
    String outputPath = "target/kikongo-nouns-arrowword-book.v1.json";

    KikongoArrowwordPackExporter.exportPack(
        mode,
        puzzleCount,
        rows,
        cols,
        maxEntries,
        meaningLanguageCode,
        languageCode,
        "kg-arrow-nouns",
        "Kikongo – mots fléchés (noms)",
        "Pack généré automatiquement : " + puzzleCount + " grilles " + rows + "x" + cols + " (noms).",
        outputPath);
  }
}
