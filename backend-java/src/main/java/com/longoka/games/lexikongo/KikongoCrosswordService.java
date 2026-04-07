package com.longoka.games.lexikongo;

import com.longoka.games.lexikongo.KikongoMixedWordSearchService;
import com.longoka.games.puzzles.crossword.json.CrosswordJsonModels;
import com.longoka.games.puzzles.wordsearch.WordSearchPuzzle;
import com.longoka.games.puzzles.wordsearch.WordToFind;
import com.longoka.games.puzzles.wordsearch.json.SemanticTagger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.sql.SQLException;

/**
 * Service de génération de grilles de mots croisés Kikongo (V1 simplifiée).
 *
 * V1 : on récupère des mots via KikongoMixedWordSearchService, on filtre
 * par partOfSpeech, puis on tente de les placer dans une grille avec quelques
 * croisements.
 */
public final class KikongoCrosswordService {

 private final KikongoMixedWordSearchService mixedWordSearchService = new KikongoMixedWordSearchService();

 public KikongoCrosswordService() {
 }

 /**
  * Génère une grille de mots croisés à partir de Lexikongo.
  *
  * @param mode            "nouns", "verbs" ou "mixed"
  * @param rows            nombre de lignes (ex: 12)
  * @param cols            nombre de colonnes (ex: 12)
  * @param maxEntries      nombre max de mots dans la grille
  * @param meaningLanguage langue des définitions (ex: "fr")
  * @param index           index de la grille (pour l'id)
  * @return un PuzzleV1 prêt à être sérialisé en JSON
  */
 public CrosswordJsonModels.PuzzleV1 generateCrossword(
   String mode,
   int rows,
   int cols,
   int maxEntries,
   String meaningLanguage,
   int index) throws SQLException {

  // 1) On récupère un puzzle "wordsearch" mixte comme source de mots
  // (on l'utilise juste comme sac de WordToFind).
  int sourceMaxWords = Math.max(maxEntries * 4, maxEntries + 12); // marge plus large pour densifier la grille
  WordSearchPuzzle sourcePuzzle = mixedWordSearchService.generateMixedKikongoWordSearch(
    rows,
    cols,
    sourceMaxWords,
    meaningLanguage);

  List<WordToFind> candidates = sourcePuzzle.getWords();
  if (candidates == null) {
   candidates = new ArrayList<>();
  }

  // 2) On filtre par partOfSpeech selon le mode + on déduplique par baseForm
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

  // 3) On construit plusieurs candidats et on garde le plus dense.
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

  return best != null
    ? best
    : buildCrosswordFromWords(words, mode, rows, cols, maxEntries, meaningLanguage, index);
 }

 /**
  * Construit une grille de mots croisés simplifiée à partir d'une liste de mots.
  */
 private CrosswordJsonModels.PuzzleV1 buildCrosswordFromWords(
   List<WordToFind> words,
   String mode,
   int rows,
   int cols,
   int maxEntries,
   String meaningLanguage,
   int index) {

  char[][] grid = new char[rows][cols];
  // On remplit tout par '#'
  for (int r = 0; r < rows; r++) {
   for (int c = 0; c < cols; c++) {
    grid[r][c] = '#';
   }
  }

  List<PlacedWord> placed = new ArrayList<>();

  // 1) On ancre le mot le plus prometteur près du centre pour ouvrir plus de croisements.
  WordToFind anchorWord = findAnchorWord(words, rows, cols);
  if (anchorWord != null) {
   String answer = sanitizeAnswer(anchorWord.getBaseForm());
   placeAnchorWord(grid, answer, anchorWord, placed);
  }

  // 2) On essaie de placer les autres mots avec croisements, puis en fallback
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

  // 3) Construction du PuzzleV1 JSON
  CrosswordJsonModels.PuzzleV1 puzzle = new CrosswordJsonModels.PuzzleV1();
  puzzle.id = "kg-cross-" + mode + "-" + index;
  puzzle.language = "kg";
  puzzle.mode = mode;
  puzzle.difficulty = "easy";
  puzzle.title = "Mots croisés Kikongo (" + mode + ")";
  puzzle.theme = mode;

  puzzle.rows = rows;
  puzzle.cols = cols;

  // Grille -> List<String>
  List<String> gridLines = new ArrayList<>(rows);
  for (int r = 0; r < rows; r++) {
   gridLines.add(new String(grid[r]));
  }
  puzzle.grid = gridLines;

  // Entries + meta
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
   e.clue = w.getTranslation(); // FR comme indice
   e.translation = w.getTranslation(); // FR
   e.translationEn = w.getTranslationEn();
   e.phonetic = w.getPhonetic();
   e.slug = w.getSlug();
   e.partOfSpeech = w.getPartOfSpeech();
   e.extraInfo = w.getExtraInfo();

   // Semantic tags
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
  meta.put("source", "lexikongo");
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

 /**
  * Nettoie une base pour obtenir la forme "réponse" en lettres A-Z (majuscules).
  */
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

 /**
  * Essaie de placer un mot en croisant des lettres déjà présentes.
  */
 private PlacedWord tryPlaceWithCrossing(char[][] grid, String answer) {
  int rows = grid.length;
  int cols = grid[0].length;
  char[] letters = answer.toCharArray();

  // On essaie de trouver une lettre commune dans la grille
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

     // Tentative horizontale (ACROSS)
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

     // Tentative verticale (DOWN)
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

 /**
  * Fallback : on tente une place horizontale simple sur une ligne libre.
  */
 private PlacedWord tryPlaceSimple(char[][] grid, String answer) {
  int rows = grid.length;
  int cols = grid[0].length;
  char[] letters = answer.toCharArray();

  // On tente d'abord horizontal
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

  // Puis vertical
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
  if (startCol < 0 || startCol + letters.length > cols) {
   return false;
  }
  if (startCol > 0 && grid[row][startCol - 1] != '#') {
   return false;
  }
  if (startCol + letters.length < cols && grid[row][startCol + letters.length] != '#') {
   return false;
  }
  for (int i = 0; i < letters.length; i++) {
   int col = startCol + i;
   char existing = grid[row][col];
   if (existing != '#' && existing != letters[i]) {
    return false;
   }
   if (existing == '#') {
    if (row > 0 && grid[row - 1][col] != '#') {
     return false;
    }
    if (row + 1 < rows && grid[row + 1][col] != '#') {
     return false;
    }
   } else {
    if ((col > 0 && grid[row][col - 1] != '#') || (col + 1 < cols && grid[row][col + 1] != '#')) {
     return false;
    }
   }
  }
  return true;
 }

 private boolean canPlaceDown(char[][] grid, char[] letters, int startRow, int col) {
  int rows = grid.length;
  int cols = grid[0].length;
  if (startRow < 0 || startRow + letters.length > rows) {
   return false;
  }
  if (startRow > 0 && grid[startRow - 1][col] != '#') {
   return false;
  }
  if (startRow + letters.length < rows && grid[startRow + letters.length][col] != '#') {
   return false;
  }
  for (int i = 0; i < letters.length; i++) {
   int row = startRow + i;
   char existing = grid[row][col];
   if (existing != '#' && existing != letters[i]) {
    return false;
   }
   if (existing == '#') {
    if (col > 0 && grid[row][col - 1] != '#') {
     return false;
    }
    if (col + 1 < cols && grid[row][col + 1] != '#') {
     return false;
    }
   } else {
    if ((row > 0 && grid[row - 1][col] != '#') || (row + 1 < rows && grid[row + 1][col] != '#')) {
     return false;
    }
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

 /**
  * Petite structure interne pour mémoriser un mot placé.
  */
 private static final class PlacedWord {
  WordToFind word;
  int row;
  int col;
  boolean across;
 }
}
