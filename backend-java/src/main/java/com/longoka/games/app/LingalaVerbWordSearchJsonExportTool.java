package com.longoka.games.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.longoka.games.lexikongo.LingalaVerbWordSearchService;
import com.longoka.games.puzzles.wordsearch.WordPlacement;
import com.longoka.games.puzzles.wordsearch.WordSearchPuzzle;
import com.longoka.games.puzzles.wordsearch.WordToFind;
import com.longoka.games.puzzles.wordsearch.json.WordSearchJsonModels;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;

public final class LingalaVerbWordSearchJsonExportTool {

 private LingalaVerbWordSearchJsonExportTool() {
 }

 public static void main(String[] args) throws Exception {
  System.out.println("== Export JSON : Mots mêlés Lingala (verbes) ==");

  int puzzleCount = 48;
  int rows = 12;
  int cols = 12;
  int maxWords = 12;
  String meaningLanguageCode = "fr";
  String outputPath = "target/lingala-verbs-wordsearch-book.v1.json";

  LingalaVerbWordSearchService service = new LingalaVerbWordSearchService();
  List<WordSearchJsonModels.PuzzleV1> jsonPuzzles = new ArrayList<>();

  for (int i = 0; i < puzzleCount; i++) {
   System.out.println(" - Génération de la grille " + (i + 1) + "/" + puzzleCount + "...");
   WordSearchPuzzle puzzle = service.generateRandomLingalaVerbWordSearch(
     rows,
     cols,
     maxWords,
     meaningLanguageCode);

   WordSearchJsonModels.PuzzleV1 jsonPuzzle = toJsonPuzzle(puzzle, "verbs", "easy", meaningLanguageCode, i + 1);
   jsonPuzzles.add(jsonPuzzle);
  }

  WordSearchJsonModels.PackV1 pack = new WordSearchJsonModels.PackV1();
  pack.language = "ln";
  pack.packId = "ln-verbs-auto-" + LocalDate.now();
  pack.title = "Lingala – mots mêlés (verbes)";
  pack.description = "Pack généré automatiquement : "
    + puzzleCount + " grilles " + rows + "x" + cols + " (verbes seulement).";
  pack.meaningLanguage = meaningLanguageCode;
  pack.puzzles = jsonPuzzles;

  ObjectMapper mapper = new ObjectMapper();
  mapper.enable(SerializationFeature.INDENT_OUTPUT);

  Path out = Path.of(outputPath);
  if (out.getParent() != null) {
   Files.createDirectories(out.getParent());
  }

  mapper.writeValue(out.toFile(), pack);

  System.out.println("✅ Export JSON (Lingala verbes) terminé : " + out.toAbsolutePath());
 }

 private static WordSearchJsonModels.PuzzleV1 toJsonPuzzle(
   WordSearchPuzzle puzzle,
   String mode,
   String difficulty,
   String meaningLanguageCode,
   int index) {

  WordSearchJsonModels.PuzzleV1 json = new WordSearchJsonModels.PuzzleV1();

  if (puzzle.getId() != null && !puzzle.getId().isBlank()) {
   json.id = puzzle.getId();
  } else {
   json.id = puzzle.getLanguageCode() + "-" + mode + "-" + index;
  }

  json.language = puzzle.getLanguageCode(); // "ln"
  json.mode = mode;
  json.difficulty = difficulty;
  json.title = puzzle.getTitle();
  json.theme = puzzle.getTheme();
  json.rows = puzzle.getRows();
  json.cols = puzzle.getCols();

  char[][] grid = puzzle.getGrid();
  List<String> gridLines = new ArrayList<>(grid.length);
  for (char[] row : grid) {
   gridLines.add(new String(row));
  }
  json.grid = gridLines;

  List<WordToFind> words = puzzle.getWords();
  Map<String, WordSearchJsonModels.EntryV1> entryByBase = new LinkedHashMap<>();

  if (words != null) {
   for (WordToFind w : words) {
    String base = w.getBaseForm();
    if (base == null || base.isBlank()) {
     base = w.getDisplayForm();
    }
    if (base == null || base.isBlank()) {
     continue;
    }

    WordSearchJsonModels.EntryV1 existing = entryByBase.get(base);
    if (existing == null) {
     WordSearchJsonModels.EntryV1 e = new WordSearchJsonModels.EntryV1();
     e.base = base;
     e.display = w.getDisplayForm();
     e.translation = w.getTranslation();
     e.slug = w.getSlug();
     e.partOfSpeech = w.getPartOfSpeech();
     e.extraInfo = w.getExtraInfo();
     e.phonetic = w.getPhonetic();
     e.translationEn = w.getTranslationEn();
     entryByBase.put(base, e);
    } else {
     String t = w.getTranslation();
     if (t != null && !t.isBlank()) {
      if (existing.translation == null || existing.translation.isBlank()) {
       existing.translation = t;
      } else if (!existing.translation.equals(t)) {
       existing.translation = existing.translation + " ; " + t;
      }
     }

     if (existing.translationEn == null && w.getTranslationEn() != null) {
      existing.translationEn = w.getTranslationEn();
     }

     if (existing.slug == null && w.getSlug() != null) {
      existing.slug = w.getSlug();
     }

     if (existing.extraInfo == null && w.getExtraInfo() != null) {
      existing.extraInfo = w.getExtraInfo();
     }

     if (existing.partOfSpeech == null && w.getPartOfSpeech() != null) {
      existing.partOfSpeech = w.getPartOfSpeech();
     }

     if (existing.display == null && w.getDisplayForm() != null) {
      existing.display = w.getDisplayForm();
     }

     if (existing.phonetic == null && w.getPhonetic() != null) {
      existing.phonetic = w.getPhonetic();
     }
    }
   }
  }

  json.entries = new ArrayList<>(entryByBase.values());

  List<WordSearchJsonModels.PlacementV1> placements = new ArrayList<>();
  if (puzzle.getPlacements() != null) {
   Set<String> seenWords = new LinkedHashSet<>();
   for (WordPlacement p : puzzle.getPlacements()) {
    String word = p.getWord();
    if (word == null || word.isBlank()) {
     continue;
    }
    String key = word.toUpperCase();
    if (seenWords.contains(key)) {
     continue;
    }
    seenWords.add(key);

    WordSearchJsonModels.PlacementV1 jp = new WordSearchJsonModels.PlacementV1();
    jp.word = word;
    jp.row = p.getRow();
    jp.col = p.getCol();
    jp.direction = p.getDirection().name();
    jp.length = word.length();
    placements.add(jp);
   }
  }
  json.placements = placements;

  Map<String, Object> meta = new HashMap<>();
  meta.put("source", "lexilingala");
  meta.put("meaningLanguage", meaningLanguageCode);

  json.meta = meta;

  return json;
 }
}
