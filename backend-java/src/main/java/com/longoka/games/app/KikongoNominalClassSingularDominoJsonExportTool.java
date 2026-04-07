package com.longoka.games.app;

import com.longoka.games.lexikongo.KikongoMorphoDominoService;

/**
 * Tool CLI pour generer un pack JSON de dominos morphologiques Kikongo
 * par classes nominales en singulier.
 */
public final class KikongoNominalClassSingularDominoJsonExportTool {

  private KikongoNominalClassSingularDominoJsonExportTool() {
    // utilitaire
  }

  public static void main(String[] args) throws Exception {
    System.out.println("== Export JSON : Dominos morphologiques Kikongo (classes nominales - singulier) ==");

    int puzzleCount = 24;
    int targetTileCount = 12;
    String meaningLanguageCode = "fr";
    String outputPath = "target/kikongo-class-singular-domino-book.v1.json";

    KikongoMorphoDominoPackExporter.exportNominalClassPack(
        KikongoMorphoDominoService.NumberPolicy.SINGULAR_ONLY,
        puzzleCount,
        targetTileCount,
        meaningLanguageCode,
        outputPath);
  }
}
