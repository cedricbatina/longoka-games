package com.longoka.games.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.longoka.games.lexikongo.LingalaCrosswordService;
import com.longoka.games.puzzles.crossword.json.CrosswordJsonModels;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Tool CLI pour générer un pack JSON de mots croisés
 * Lingala (noms) à partir de la base Lexilingala.
 */
public final class LingalaNounCrosswordJsonExportTool {

 private LingalaNounCrosswordJsonExportTool() {
 }

 public static void main(String[] args) throws Exception {
  System.out.println("== Export JSON : Mots croisés Lingala (noms) ==");

  int puzzleCount = 48;
  int rows = 12;
  int cols = 12;
  int maxEntries = 12;
  String meaningLanguageCode = "fr";
  String languageCode = "ln";
  String outputPath = "target/lingala-nouns-crossword-book.v1.json";

  LingalaCrosswordService service = new LingalaCrosswordService();
  List<CrosswordJsonModels.PuzzleV1> puzzles = new ArrayList<>();

  for (int i = 0; i < puzzleCount; i++) {
   System.out.println(" - Génération de la grille " + (i + 1) + "/" + puzzleCount + "...");
   CrosswordJsonModels.PuzzleV1 puzzle = service.generateCrossword(
     "nouns",
     rows,
     cols,
     maxEntries,
     meaningLanguageCode,
     i + 1);
   puzzles.add(puzzle);
  }

  CrosswordJsonModels.PackV1 pack = new CrosswordJsonModels.PackV1();
  pack.language = languageCode;
  pack.packId = "ln-cross-nouns-" + LocalDate.now();
  pack.title = "Lingala – mots croisés (noms)";
  pack.description = "Pack généré automatiquement : " + puzzleCount
    + " grilles " + rows + "x" + cols + ".";
  pack.meaningLanguage = meaningLanguageCode;
  pack.puzzles = puzzles;

  ObjectMapper mapper = new ObjectMapper();
  mapper.enable(SerializationFeature.INDENT_OUTPUT);

  Path out = Path.of(outputPath);
  if (out.getParent() != null) {
   Files.createDirectories(out.getParent());
  }

  System.out.println("DEBUG: path absolu = " + out.toAbsolutePath());
  mapper.writeValue(out.toFile(), pack);

  System.out.println("✅ Export JSON (mots croisés noms Lingala) terminé : " + out.toAbsolutePath());
 }
}
