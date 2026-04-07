package com.longoka.games.app;

/**
 * Tool CLI pour générer un pack JSON de mots fléchés Lingala (noms).
 */
public final class LingalaNounArrowwordJsonExportTool {

  private LingalaNounArrowwordJsonExportTool() {
    // utilitaire
  }

  public static void main(String[] args) throws Exception {
    System.out.println("== Export JSON : Mots fléchés Lingala (noms) ==");

    int puzzleCount = 48;
    int rows = 12;
    int cols = 12;
    int maxEntries = 12;
    String meaningLanguageCode = "fr";
    String mode = "nouns";
    String languageCode = "ln";
    String outputPath = "target/lingala-nouns-arrowword-book.v1.json";

    LingalaArrowwordPackExporter.exportPack(
        mode,
        puzzleCount,
        rows,
        cols,
        maxEntries,
        meaningLanguageCode,
        languageCode,
        "ln-arrow-nouns",
        "Lingala – mots fléchés (noms)",
        "Pack généré automatiquement : " + puzzleCount + " grilles " + rows + "x" + cols + " (noms).",
        outputPath);
  }
}
