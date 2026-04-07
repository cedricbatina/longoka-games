package com.longoka.games.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.longoka.games.lexikongo.LingalaArrowwordService;
import com.longoka.games.puzzles.arrowword.ArrowwordValidator;
import com.longoka.games.puzzles.arrowword.json.ArrowwordJsonModels;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Exporteur réutilisable de packs JSON de mots fléchés Lingala.
 */
public final class LingalaArrowwordPackExporter {

  private LingalaArrowwordPackExporter() {
    // utilitaire
  }

  public static void exportPack(
      String mode,
      int puzzleCount,
      int rows,
      int cols,
      int maxEntries,
      String meaningLanguageCode,
      String languageCode,
      String packIdPrefix,
      String title,
      String description,
      String outputPath) throws Exception {

    LingalaArrowwordService service = new LingalaArrowwordService();
    List<ArrowwordJsonModels.PuzzleV1> puzzles = new ArrayList<>();

    for (int i = 0; i < puzzleCount; i++) {
      System.out.println(" - Génération de la grille " + (i + 1) + "/" + puzzleCount + "...");
      ArrowwordJsonModels.PuzzleV1 puzzle = service.generateArrowword(
          mode,
          rows,
          cols,
          maxEntries,
          meaningLanguageCode,
          i + 1);
      puzzles.add(puzzle);
    }

    ArrowwordJsonModels.PackV1 pack = new ArrowwordJsonModels.PackV1();
    pack.language = languageCode;
    pack.packId = packIdPrefix + "-" + LocalDate.now();
    pack.title = title;
    pack.description = description;
    pack.meaningLanguage = meaningLanguageCode;
    pack.puzzles = puzzles;

    ArrowwordValidator.validatePack(pack);

    ObjectMapper mapper = new ObjectMapper();
    mapper.enable(SerializationFeature.INDENT_OUTPUT);

    Path out = Path.of(outputPath);
    if (out.getParent() != null) {
      Files.createDirectories(out.getParent());
    }

    mapper.writeValue(out.toFile(), pack);
    System.out.println("✅ Export JSON (mots fléchés Lingala) terminé : " + out.toAbsolutePath());
  }
}
