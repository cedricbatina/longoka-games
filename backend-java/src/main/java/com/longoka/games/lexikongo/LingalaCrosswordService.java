package com.longoka.games.lexikongo;

import com.longoka.games.puzzles.crossword.CrosswordGridQuality;
import com.longoka.games.puzzles.crossword.json.CrosswordJsonModels;
import com.longoka.games.puzzles.wordsearch.WordSearchPuzzle;
import com.longoka.games.puzzles.wordsearch.WordToFind;
import com.longoka.games.puzzles.wordsearch.json.SemanticTagger;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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

  int sourceMaxWords = Math.max(maxEntries * 4, maxEntries + 12);
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

  return buildBestCrosswordFromWords(filtered, mode, rows, cols, maxEntries, meaningLanguage, index);
 }

 private CrosswordJsonModels.PuzzleV1 buildBestCrosswordFromWords(
   List<WordToFind> words,
   String mode,
   int rows,
   int cols,
   int maxEntries,
   String meaningLanguage,
   int index) {

  CrosswordJsonModels.PuzzleV1 best = null;
  int attempts = Math.max(4, Math.min(10, maxEntries));

  for (int attempt = 0; attempt < attempts; attempt++) {
   List<WordToFind> ordered = reorderWordsForCrossword(words, maxEntries, attempt);
   CrosswordJsonModels.PuzzleV1 candidate = buildCrosswordFromWords(
     ordered,
     mode,
     rows,
     cols,
     maxEntries,
     meaningLanguage,
     index);

   if (isBetterCrosswordCandidate(candidate, best)) {
    best = candidate;
   }

   if (countEntries(candidate) >= maxEntries) {
    break;
   }
  }

  if (best != null && !CrosswordGridQuality.whiteCellsOrthogonallyConnected(best)) {
   CrosswordJsonModels.PuzzleV1 bestConnected = null;
   int extra = Math.max(30, attempts * 4);
   for (int attempt = attempts; attempt < attempts + extra; attempt++) {
    List<WordToFind> ordered = reorderWordsForCrossword(words, maxEntries, attempt);
    CrosswordJsonModels.PuzzleV1 candidate = buildCrosswordFromWords(
      ordered,
      mode,
      rows,
      cols,
      maxEntries,
      meaningLanguage,
      index);
    if (!CrosswordGridQuality.whiteCellsOrthogonallyConnected(candidate)) {
     continue;
    }
    if (bestConnected == null || isBetterCrosswordCandidate(candidate, bestConnected)) {
     bestConnected = candidate;
    }
   }
   if (bestConnected != null) {
    best = bestConnected;
   }
  }

  return best != null
    ? best
    : buildCrosswordFromWords(words, mode, rows, cols, maxEntries, meaningLanguage, index);
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

  // 1) On ancre le meilleur mot près du centre pour densifier la grille.
  WordToFind anchorWord = findAnchorWord(words, rows, cols);
  if (anchorWord != null) {
   String answer = sanitizeAnswer(anchorWord.getBaseForm());
   placeAnchorWord(grid, answer, anchorWord, placed);
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
   } else if (placed.isEmpty()) {
    // On n'autorise un placement libre que pour le tout premier mot.
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

  Map<String, Integer> startNumbers = buildCrosswordStartNumbers(placed);
  List<PlacedWord> orderedPlaced = sortPlacedWordsForExport(placed);

  for (PlacedWord pw : orderedPlaced) {
   WordToFind w = pw.word;
   if (w == null) {
    continue;
   }

   String base = w.getBaseForm();
   String answer = sanitizeAnswer(base);
   int number = startNumbers.getOrDefault(startKey(pw.row, pw.col), 0);

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

  meta.put("whiteRegionConnected", CrosswordGridQuality.whiteCellsOrthogonallyConnected(puzzle));
  puzzle.meta = meta;

  return puzzle;
 }

 private List<WordToFind> reorderWordsForCrossword(List<WordToFind> words, int maxEntries, int attempt) {
  List<WordToFind> ordered = new ArrayList<>(words != null ? words : List.of());
  ordered.sort(
    Comparator
      .comparingInt((WordToFind word) -> sanitizeAnswer(word != null ? word.getBaseForm() : "").length())
      .reversed()
      .thenComparing(word -> String.valueOf(word != null ? word.getBaseForm() : "")));

  if (ordered.size() > 1) {
   int anchorWindow = Math.min(ordered.size(), Math.max(2, Math.min(6, maxEntries)));
   int anchorIndex = attempt % anchorWindow;
   if (anchorIndex > 0) {
    WordToFind anchor = ordered.remove(anchorIndex);
    ordered.add(0, anchor);
   }

   if (ordered.size() > 3) {
    int rotate = (attempt / Math.max(anchorWindow, 1)) % (ordered.size() - 1);
    if (rotate > 0) {
      Collections.rotate(ordered.subList(1, ordered.size()), rotate);
    }
   }
  }

  return ordered;
 }

 private boolean isBetterCrosswordCandidate(
   CrosswordJsonModels.PuzzleV1 candidate,
   CrosswordJsonModels.PuzzleV1 currentBest) {

  if (candidate == null) {
   return false;
  }
  if (currentBest == null) {
   return true;
  }

  boolean cConn = CrosswordGridQuality.whiteCellsOrthogonallyConnected(candidate);
  boolean bConn = CrosswordGridQuality.whiteCellsOrthogonallyConnected(currentBest);
  if (cConn != bConn) {
   return cConn;
  }

  int candidateEntries = countEntries(candidate);
  int currentEntries = countEntries(currentBest);
  if (candidateEntries != currentEntries) {
   return candidateEntries > currentEntries;
  }

  return countFilledLetters(candidate) > countFilledLetters(currentBest);
 }

 private int countEntries(CrosswordJsonModels.PuzzleV1 puzzle) {
  return puzzle != null && puzzle.entries != null ? puzzle.entries.size() : 0;
 }

 private int countFilledLetters(CrosswordJsonModels.PuzzleV1 puzzle) {
  if (puzzle == null || puzzle.grid == null) {
   return 0;
  }

  int count = 0;
  for (String row : puzzle.grid) {
   if (row == null) {
    continue;
   }
   for (int i = 0; i < row.length(); i++) {
    if (row.charAt(i) != '#') {
     count++;
    }
   }
  }
  return count;
 }

 private WordToFind findAnchorWord(List<WordToFind> words, int rows, int cols) {
  if (words == null) {
   return null;
  }

  for (WordToFind word : words) {
   String answer = sanitizeAnswer(word != null ? word.getBaseForm() : "");
   if (answer.length() >= 3 && (answer.length() <= cols || answer.length() <= rows)) {
    return word;
   }
  }
  return null;
 }

 private void placeAnchorWord(char[][] grid, String answer, WordToFind word, List<PlacedWord> placed) {
  PlacedWord anchor = tryPlaceCenteredAcross(grid, answer);
  if (anchor == null) {
   anchor = tryPlaceCenteredDown(grid, answer);
  }
  if (anchor == null) {
   anchor = tryPlaceSimple(grid, answer);
  }
  if (anchor == null) {
   return;
  }

  anchor.word = word;
  placed.add(anchor);
 }

 private PlacedWord tryPlaceCenteredAcross(char[][] grid, String answer) {
  char[] letters = answer.toCharArray();
  int rows = grid.length;
  int cols = grid[0].length;
  if (letters.length > cols) {
   return null;
  }

  int centerRow = rows / 2;
  int centeredStartCol = Math.max(0, (cols - letters.length) / 2);
  for (int offset = 0; offset < rows; offset++) {
   int upperRow = centerRow - offset;
   if (upperRow >= 0 && canPlaceAcross(grid, letters, upperRow, centeredStartCol)) {
    placeAcross(grid, answer, upperRow, centeredStartCol);
    PlacedWord placedWord = new PlacedWord();
    placedWord.row = upperRow;
    placedWord.col = centeredStartCol;
    placedWord.across = true;
    return placedWord;
   }

   if (offset == 0) {
    continue;
   }

   int lowerRow = centerRow + offset;
   if (lowerRow < rows && canPlaceAcross(grid, letters, lowerRow, centeredStartCol)) {
    placeAcross(grid, answer, lowerRow, centeredStartCol);
    PlacedWord placedWord = new PlacedWord();
    placedWord.row = lowerRow;
    placedWord.col = centeredStartCol;
    placedWord.across = true;
    return placedWord;
   }
  }

  return null;
 }

 private PlacedWord tryPlaceCenteredDown(char[][] grid, String answer) {
  char[] letters = answer.toCharArray();
  int rows = grid.length;
  int cols = grid[0].length;
  if (letters.length > rows) {
   return null;
  }

  int centerCol = cols / 2;
  int centeredStartRow = Math.max(0, (rows - letters.length) / 2);
  for (int offset = 0; offset < cols; offset++) {
   int leftCol = centerCol - offset;
   if (leftCol >= 0 && canPlaceDown(grid, letters, centeredStartRow, leftCol)) {
    placeDown(grid, answer, centeredStartRow, leftCol);
    PlacedWord placedWord = new PlacedWord();
    placedWord.row = centeredStartRow;
    placedWord.col = leftCol;
    placedWord.across = false;
    return placedWord;
   }

   if (offset == 0) {
    continue;
   }

   int rightCol = centerCol + offset;
   if (rightCol < cols && canPlaceDown(grid, letters, centeredStartRow, rightCol)) {
    placeDown(grid, answer, centeredStartRow, rightCol);
    PlacedWord placedWord = new PlacedWord();
    placedWord.row = centeredStartRow;
    placedWord.col = rightCol;
    placedWord.across = false;
    return placedWord;
   }
  }

  return null;
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
  int rows = grid.length;
  int cols = grid[0].length;
  int len = letters.length;
  if (startCol < 0 || startCol + len > cols) {
   return false;
  }
  if (startCol > 0 && grid[row][startCol - 1] != '#') {
   return false;
  }
  if (startCol + len < cols && grid[row][startCol + len] != '#') {
   return false;
  }
  for (int i = 0; i < len; i++) {
   int c = startCol + i;
   char g = grid[row][c];
   if (g != '#' && g != letters[i]) {
    return false;
   }
   if (i > 0 && grid[row][c - 1] != letters[i - 1]) {
    return false;
   }
   char need = letters[i];
   if (row > 0 && grid[row - 1][c] != '#' && grid[row - 1][c] != need) {
    return false;
   }
   if (row + 1 < rows && grid[row + 1][c] != '#' && grid[row + 1][c] != need) {
    return false;
   }
  }
  return true;
 }

 private boolean canPlaceDown(char[][] grid, char[] letters, int startRow, int col) {
  int rows = grid.length;
  int cols = grid[0].length;
  int len = letters.length;
  if (startRow < 0 || startRow + len > rows) {
   return false;
  }
  if (startRow > 0 && grid[startRow - 1][col] != '#') {
   return false;
  }
  if (startRow + len < rows && grid[startRow + len][col] != '#') {
   return false;
  }
  for (int i = 0; i < len; i++) {
   int row = startRow + i;
   char g = grid[row][col];
   if (g != '#' && g != letters[i]) {
    return false;
   }
   if (i > 0 && grid[row - 1][col] != letters[i - 1]) {
    return false;
   }
   char need = letters[i];
   if (col > 0 && grid[row][col - 1] != '#' && grid[row][col - 1] != need) {
    return false;
   }
   if (col + 1 < cols && grid[row][col + 1] != '#' && grid[row][col + 1] != need) {
    return false;
   }
  }
  return true;
 }

 private List<PlacedWord> sortPlacedWordsForExport(List<PlacedWord> placed) {
  List<PlacedWord> ordered = new ArrayList<>(placed);
  ordered.sort((a, b) -> {
   int rowCompare = Integer.compare(a.row, b.row);
   if (rowCompare != 0) {
    return rowCompare;
   }
   int colCompare = Integer.compare(a.col, b.col);
   if (colCompare != 0) {
    return colCompare;
   }
   return Boolean.compare(a.across, b.across) * -1;
  });
  return ordered;
 }

 private Map<String, Integer> buildCrosswordStartNumbers(List<PlacedWord> placed) {
  Map<String, Integer> numbering = new HashMap<>();
  int number = 1;
  for (PlacedWord pw : sortPlacedWordsForExport(placed)) {
   String key = startKey(pw.row, pw.col);
   if (!numbering.containsKey(key)) {
    numbering.put(key, number++);
   }
  }
  return numbering;
 }

 private String startKey(int row, int col) {
  return row + ":" + col;
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
