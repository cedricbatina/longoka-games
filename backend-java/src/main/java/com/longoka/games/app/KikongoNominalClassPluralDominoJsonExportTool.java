package com.longoka.games.app;

import com.longoka.games.lexikongo.KikongoMorphoDominoService;

/**
 * Tool CLI pour generer un pack JSON de dominos morphologiques Kikongo
 * par classes nominales en pluriel.
 */
public final class KikongoNominalClassPluralDominoJsonExportTool {

  private KikongoNominalClassPluralDominoJsonExportTool() {
    // utilitaire
  }

  public static void main(String[] args) throws Exception {
    System.out.println("== Export JSON : Dominos morphologiques Kikongo (classes nominales - pluriel) ==");

    int puzzleCount = 24;
    int targetTileCount = 12;
    String meaningLanguageCode = "fr";
    String outputPath = "target/kikongo-class-plural-domino-book.v1.json";

    KikongoMorphoDominoPackExporter.exportNominalClassPack(
        KikongoMorphoDominoService.NumberPolicy.PLURAL_ONLY,
        puzzleCount,
        targetTileCount,
        meaningLanguageCode,
        outputPath);
  }
}
