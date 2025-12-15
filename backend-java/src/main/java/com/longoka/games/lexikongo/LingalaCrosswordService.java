package com.longoka.games.lexikongo;

import com.longoka.games.puzzles.crossword.json.CrosswordJsonModels;
import com.longoka.games.puzzles.wordsearch.WordSearchPuzzle;
import com.longoka.games.puzzles.wordsearch.WordToFind;
import com.longoka.games.puzzles.wordsearch.json.SemanticTagger;

import java.sql.SQLException;
import java.util.*;

/**
 * Service de génération de grilles de mots croisés Lingala.
 */
public final class LingalaCrosswordService {

 private final LingalaMixedWordSearchService mixedWordSearchService = new LingalaMixedWordSearchService();

 public LingalaCrosswordService() {
 }

 /**
  * Génère une grille de mots croisés à partir de Lexilingala.
  *
  * @param mode            "nouns", "verbs" ou "mixed"
  * @param rows            nombre de lignes (ex: 12)
  * @param cols            nombre de colonnes (ex: 12)
  * @param maxEntries      nombre max de mots dans la grille
  * @param meaningLanguage langue des définitions (ex: "fr")
  * @param index           index de la grille (pour l'id)
  */
 public CrosswordJsonModels.PuzzleV1 generateCrossword(
   String mode,
   int rows,
   int cols,
   int maxEntries,
   String meaningLanguage,
   int index) throws SQLException {

  int sourceMaxWords = maxEntries * 3;
  WordSearchPuzzle sourcePuzzle = mixedWordSearchService.generateMixedLingalaWordSearch(
    rows,
    cols,
    sourceMaxWords,
    meaningLanguage);

  List<WordToFind> candidates = sourcePuzzle.getWords();
  if (candidates == null) {
   candidates = new ArrayList<>();
  }

  List<WordToFind> filtered = new ArrayList<>();
  Set<String> seenBase = new HashSet<>();

  for (WordToFind w : candidates) {
   if (w == null) {
    continue;
   }
   String base = w.getBaseForm();
   if (base == null || base.isBlank()) {
    continue;
   }

   String pos = w.getPartOfSpeech();
   if ("nouns".equalsIgnoreCase(mode)) {
    if (!"noun".equals(pos)) {
     continue;
    }
   } else if ("verbs".equalsIgnoreCase(mode)) {
    if (!"verb".equals(pos)) {
     continue;
    }
   } else {
    // "mixed" -> on prend tout
   }

   String key = base.toLowerCase(Locale.ROOT);
   if (seenBase.contains(key)) {
    continue;
   }
   seenBase.add(key);

   String answer = sanitizeAnswer(base);
   if (answer.length() < 3 || (answer.length() > rows && answer.length() > cols)) {
    continue;
   }

   filtered.add(w);
   if (filtered.size() >= sourceMaxWords) {
    break;
   }
  }

  return buildCrosswordFromWords(filtered, mode, rows, cols, maxEntries, meaningLanguage, index);
 }

 private CrosswordJsonModels.PuzzleV1 buildCrosswordFromWords(
   List<WordToFind> words,
   String mode,
   int rows,
   int cols,
   int maxEntries,
   String meaningLanguage,
   int index) {

  char[][] grid = new char[rows][cols];
  for (int r = 0; r < rows; r++) {
   for (int c = 0; c < cols; c++) {
    grid[r][c] = '#';
   }
  }

  List<PlacedWord> placed = new ArrayList<>();

  // 1) premier mot en ACROSS en haut à gauche
  for (WordToFind w : words) {
   String answer = sanitizeAnswer(w.getBaseForm());
   if (answer.length() <= cols) {
    placeAcross(grid, answer, 0, 0);
    PlacedWord pw = new PlacedWord();
    pw.word = w;
    pw.row = 0;
    pw.col = 0;
    pw.across = true;
    placed.add(pw);
    break;
   }
  }

  // 2) autres mots avec croisements + fallback
  for (WordToFind w : words) {
   if (placed.size() >= maxEntries) {
    break;
   }
   if (isAlreadyPlaced(placed, w)) {
    continue;
   }

   String answer = sanitizeAnswer(w.getBaseForm());
   if (answer.length() < 3) {
    continue;
   }

   PlacedWord found = tryPlaceWithCrossing(grid, answer);
   if (found != null) {
    found.word = w;
    placed.add(found);
   } else {
    PlacedWord fallback = tryPlaceSimple(grid, answer);
    if (fallback != null) {
     fallback.word = w;
     placed.add(fallback);
    }
   }
  }

  CrosswordJsonModels.PuzzleV1 puzzle = new CrosswordJsonModels.PuzzleV1();
  puzzle.id = "ln-cross-" + mode + "-" + index;
  puzzle.language = "ln";
  puzzle.mode = mode;
  puzzle.difficulty = "easy";
  puzzle.title = "Mots croisés Lingala (" + mode + ")";
  puzzle.theme = mode;

  puzzle.rows = rows;
  puzzle.cols = cols;

  List<String> gridLines = new ArrayList<>(rows);
  for (int r = 0; r < rows; r++) {
   gridLines.add(new String(grid[r]));
  }
  puzzle.grid = gridLines;

  List<CrosswordJsonModels.EntryV1> entries = new ArrayList<>();
  Set<String> nominalClasses = new LinkedHashSet<>();
  Set<String> semanticDomains = new LinkedHashSet<>();

  int number = 1;
  for (PlacedWord pw : placed) {
   WordToFind w = pw.word;
   if (w == null) {
    continue;
   }

   String base = w.getBaseForm();
   String answer = sanitizeAnswer(base);

   CrosswordJsonModels.EntryV1 e = new CrosswordJsonModels.EntryV1();
   e.id = number + (pw.across ? "-A" : "-D");
   e.number = number;
   e.direction = pw.across ? "ACROSS" : "DOWN";
   e.row = pw.row;
   e.col = pw.col;
   e.answer = answer;
   e.display = w.getDisplayForm();
   e.clue = w.getTranslation();
   e.translation = w.getTranslation();
   e.translationEn = w.getTranslationEn();
   e.phonetic = w.getPhonetic();
   e.slug = w.getSlug();
   e.partOfSpeech = w.getPartOfSpeech();
   e.extraInfo = w.getExtraInfo();

   List<String> tags = SemanticTagger.guessTags(e.translation);
   e.semanticTags = tags;
   if (tags != null) {
    semanticDomains.addAll(tags);
   }

   if (e.extraInfo != null && !e.extraInfo.isBlank()) {
    nominalClasses.add(e.extraInfo);
   }

   entries.add(e);
   number++;
  }

  puzzle.entries = entries;

  Map<String, Object> meta = new HashMap<>();
  meta.put("source", "lexilingala");
  meta.put("meaningLanguage", meaningLanguage);

  if (!nominalClasses.isEmpty()) {
   meta.put("nominalClasses", new ArrayList<>(nominalClasses));
  }
  if (!semanticDomains.isEmpty()) {
   meta.put("semanticDomains", new ArrayList<>(semanticDomains));
  }

  puzzle.meta = meta;

  return puzzle;
 }

 private String sanitizeAnswer(String baseForm) {
  if (baseForm == null) {
   return "";
  }
  StringBuilder sb = new StringBuilder();
  String s = baseForm.toUpperCase(Locale.ROOT);
  for (int i = 0; i < s.length(); i++) {
   char ch = s.charAt(i);
   if (ch >= 'A' && ch <= 'Z') {
    sb.append(ch);
   }
  }
  return sb.toString();
 }

 private boolean isAlreadyPlaced(List<PlacedWord> placed, WordToFind w) {
  String base = w.getBaseForm();
  if (base == null) {
   return false;
  }
  String key = base.toLowerCase(Locale.ROOT);
  for (PlacedWord pw : placed) {
   if (pw.word != null && pw.word.getBaseForm() != null) {
    if (key.equals(pw.word.getBaseForm().toLowerCase(Locale.ROOT))) {
     return true;
    }
   }
  }
  return false;
 }

 private PlacedWord tryPlaceWithCrossing(char[][] grid, String answer) {
  int rows = grid.length;
  int cols = grid[0].length;
  char[] letters = answer.toCharArray();

  for (int r = 0; r < rows; r++) {
   for (int c = 0; c < cols; c++) {
    char existing = grid[r][c];
    if (existing == '#' || existing == 0) {
     continue;
    }
    for (int i = 0; i < letters.length; i++) {
     if (letters[i] != existing) {
      continue;
     }

     int startCol = c - i;
     if (startCol >= 0 && startCol + letters.length <= cols) {
      if (canPlaceAcross(grid, letters, r, startCol)) {
       placeAcross(grid, answer, r, startCol);
       PlacedWord pw = new PlacedWord();
       pw.row = r;
       pw.col = startCol;
       pw.across = true;
       return pw;
      }
     }

     int startRow = r - i;
     if (startRow >= 0 && startRow + letters.length <= rows) {
      if (canPlaceDown(grid, letters, startRow, c)) {
       placeDown(grid, answer, startRow, c);
       PlacedWord pw = new PlacedWord();
       pw.row = startRow;
       pw.col = c;
       pw.across = false;
       return pw;
      }
     }
    }
   }
  }
  return null;
 }

 private PlacedWord tryPlaceSimple(char[][] grid, String answer) {
  int rows = grid.length;
  int cols = grid[0].length;
  char[] letters = answer.toCharArray();

  for (int r = 0; r < rows; r++) {
   for (int startCol = 0; startCol + letters.length <= cols; startCol++) {
    if (canPlaceAcross(grid, letters, r, startCol)) {
     placeAcross(grid, answer, r, startCol);
     PlacedWord pw = new PlacedWord();
     pw.row = r;
     pw.col = startCol;
     pw.across = true;
     return pw;
    }
   }
  }

  for (int c = 0; c < cols; c++) {
   for (int startRow = 0; startRow + letters.length <= rows; startRow++) {
    if (canPlaceDown(grid, letters, startRow, c)) {
     placeDown(grid, answer, startRow, c);
     PlacedWord pw = new PlacedWord();
     pw.row = startRow;
     pw.col = c;
     pw.across = false;
     return pw;
    }
   }
  }

  return null;
 }

 private boolean canPlaceAcross(char[][] grid, char[] letters, int row, int startCol) {
  int cols = grid[0].length;
  if (startCol < 0 || startCol + letters.length > cols) {
   return false;
  }
  for (int i = 0; i < letters.length; i++) {
   char existing = grid[row][startCol + i];
   if (existing != '#' && existing != letters[i]) {
    return false;
   }
  }
  return true;
 }

 private boolean canPlaceDown(char[][] grid, char[] letters, int startRow, int col) {
  int rows = grid.length;
  if (startRow < 0 || startRow + letters.length > rows) {
   return false;
  }
  for (int i = 0; i < letters.length; i++) {
   char existing = grid[startRow + i][col];
   if (existing != '#' && existing != letters[i]) {
    return false;
   }
  }
  return true;
 }

 private void placeAcross(char[][] grid, String answer, int row, int startCol) {
  char[] letters = answer.toCharArray();
  for (int i = 0; i < letters.length; i++) {
   grid[row][startCol + i] = letters[i];
  }
 }

 private void placeDown(char[][] grid, String answer, int startRow, int col) {
  char[] letters = answer.toCharArray();
  for (int i = 0; i < letters.length; i++) {
   grid[startRow + i][col] = letters[i];
  }
 }

 private static final class PlacedWord {
  WordToFind word;
  int row;
  int col;
  boolean across;
 }
}
