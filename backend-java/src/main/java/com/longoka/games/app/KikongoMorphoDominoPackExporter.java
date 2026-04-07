package com.longoka.games.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.longoka.games.lexikongo.KikongoMorphoDominoService;
import com.longoka.games.puzzles.domino.MorphoDominoValidator;
import com.longoka.games.puzzles.domino.json.MorphoDominoJsonModels;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Exporteur reutilisable de packs JSON de dominos morphologiques Kikongo.
 */
public final class KikongoMorphoDominoPackExporter {

  private KikongoMorphoDominoPackExporter() {
    // utilitaire
  }

  public static void exportNominalClassPack(
      KikongoMorphoDominoService.NumberPolicy numberPolicy,
      int puzzleCount,
      int targetTileCount,
      String meaningLanguageCode,
      String outputPath) throws Exception {

    KikongoMorphoDominoService service = new KikongoMorphoDominoService();
    MorphoDominoJsonModels.PackV1 pack = service.generateNominalClassPack(
        numberPolicy,
        puzzleCount,
        targetTileCount,
        meaningLanguageCode);

    MorphoDominoValidator.validatePack(pack);

    ObjectMapper mapper = new ObjectMapper();
    mapper.enable(SerializationFeature.INDENT_OUTPUT);

    Path out = Path.of(outputPath);
    if (out.getParent() != null) {
      Files.createDirectories(out.getParent());
    }

    mapper.writeValue(out.toFile(), pack);
    System.out.println("✅ Export JSON (dominos morphologiques Kikongo) termine : " + out.toAbsolutePath());
  }
}
