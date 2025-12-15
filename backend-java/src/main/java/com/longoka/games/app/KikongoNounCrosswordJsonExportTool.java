package com.longoka.games.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.longoka.games.lexikongo.KikongoCrosswordService;
import com.longoka.games.puzzles.crossword.json.CrosswordJsonModels;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Tool CLI pour générer un pack JSON de mots croisés Kikongo (noms).
 */
public final class KikongoNounCrosswordJsonExportTool {

  private KikongoNounCrosswordJsonExportTool() {
    // utilitaire
  }

  public static void main(String[] args) throws Exception {
    System.out.println("== Export JSON : Mots croisés Kikongo (noms) ==");

    int puzzleCount = 48; // nombre de grilles
    int rows = 12;
    int cols = 12;
    int maxEntries = 10; // nb max de mots par grille
    String meaningLanguageCode = "fr";
    String mode = "nouns";
    String languageCode = "kg";

    String outputPath = "target/kikongo-nouns-crossword-book.v3.json";

    KikongoCrosswordService service = new KikongoCrosswordService();
    List<CrosswordJsonModels.PuzzleV1> puzzles = new ArrayList<>();

    for (int i = 0; i < puzzleCount; i++) {
      System.out.println(" - Génération de la grille " + (i + 1) + "/" + puzzleCount + "...");
      CrosswordJsonModels.PuzzleV1 puzzle = service.generateCrossword(
          mode,
          rows,
          cols,
          maxEntries,
          meaningLanguageCode,
          i + 1);
      puzzles.add(puzzle);
    }

    // Pack
    CrosswordJsonModels.PackV1 pack = new CrosswordJsonModels.PackV1();
    pack.language = languageCode;
    pack.packId = "kg-cross-nouns-" + LocalDate.now();
    pack.title = "Kikongo – mots croisés (noms)";
    pack.description = "Pack généré automatiquement : " + puzzleCount + " grilles " + rows + "x" + cols + " (noms).";
    pack.meaningLanguage = meaningLanguageCode;
    pack.puzzles = puzzles;

    // Sérialisation JSON
    ObjectMapper mapper = new ObjectMapper();
    mapper.enable(SerializationFeature.INDENT_OUTPUT);

    Path out = Path.of(outputPath);
    if (out.getParent() != null) {
      Files.createDirectories(out.getParent());
    }

    System.out.println("DEBUG: path absolu = " + out.toAbsolutePath());
    mapper.writeValue(out.toFile(), pack);

    System.out.println("✅ Export JSON (mots croisés noms) terminé : " + out.toAbsolutePath());
  }
}
