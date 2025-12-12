package com.longoka.games.puzzles.wordsearch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Simple word search generator.
 * Works only on ASCII letters for the grid; words can contain accents in
 * displayForm.
 */
public class WordSearchGenerator {

 private static final String DEFAULT_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

 private final Random random;
 private final String alphabet;

 public WordSearchGenerator() {
  this(new Random(), DEFAULT_ALPHABET);
 }

 public WordSearchGenerator(Random random) {
  this(random, DEFAULT_ALPHABET);
 }

 public WordSearchGenerator(Random random, String alphabet) {
  this.random = random != null ? random : new Random();
  this.alphabet = (alphabet != null && !alphabet.isEmpty()) ? alphabet : DEFAULT_ALPHABET;
 }

 /**
  * Generate a new word search puzzle.
  *
  * @param languageCode language code, for example "kg" or "ln"
  * @param title        puzzle title
  * @param theme        optional theme
  * @param rows         number of rows
  * @param cols         number of columns
  * @param words        words to place (baseForm will be used)
  */
 public WordSearchPuzzle generatePuzzle(
   String languageCode,
   String title,
   String theme,
   int rows,
   int cols,
   List<WordToFind> words) {
  if (rows <= 0 || cols <= 0) {
   throw new IllegalArgumentException("rows and cols must be > 0");
  }
  if (words == null || words.isEmpty()) {
   throw new IllegalArgumentException("words must not be null or empty");
  }

  char[][] grid = new char[rows][cols];
  List<WordPlacement> placements = new ArrayList<>();

  // We can shuffle the order to avoid always placing in the same order
  List<WordToFind> shuffledWords = new ArrayList<>(words);
  Collections.shuffle(shuffledWords, random);

  for (WordToFind wordToFind : shuffledWords) {
   String normalized = normalizeWordForGrid(wordToFind.getBaseForm());
   if (normalized.length() == 0) {
    continue;
   }
   if (normalized.length() > rows && normalized.length() > cols) {
    // word is too long for this grid, skip it
    continue;
   }

   boolean placed = tryPlaceWord(grid, normalized, placements);
   // If not placed, we simply skip the word. Later we can log or track failures.
  }

  // Fill empty cells
  fillEmptyCells(grid);

  return new WordSearchPuzzle(
    null, // no id yet
    languageCode,
    title,
    theme,
    rows,
    cols,
    grid,
    words,
    placements);
 }

 private String normalizeWordForGrid(String baseForm) {
  // Remove spaces, dashes and apostrophes; convert to upper case.
  StringBuilder sb = new StringBuilder();
  for (int i = 0; i < baseForm.length(); i++) {
   char ch = baseForm.charAt(i);
   if (Character.isLetter(ch)) {
    sb.append(Character.toUpperCase(ch));
   }
   // ignore other characters for the grid (spaces, punctuation)
  }
  return sb.toString();
 }

 private boolean tryPlaceWord(char[][] grid,
   String word,
   List<WordPlacement> placements) {
  int rows = grid.length;
  int cols = grid[0].length;
  Direction[] directions = Direction.values();
  int maxAttempts = 100;

  for (int attempt = 0; attempt < maxAttempts; attempt++) {
   Direction dir = directions[random.nextInt(directions.length)];

   int maxRowStart;
   int maxColStart;
   switch (dir) {
    case RIGHT:
     maxRowStart = rows;
     maxColStart = cols - word.length() + 1;
     break;
    case DOWN:
     maxRowStart = rows - word.length() + 1;
     maxColStart = cols;
     break;
    case DIAGONAL_DOWN_RIGHT:
     maxRowStart = rows - word.length() + 1;
     maxColStart = cols - word.length() + 1;
     break;
    case DIAGONAL_UP_RIGHT:
     maxRowStart = rows;
     maxColStart = cols - word.length() + 1;
     break;
    default:
     maxRowStart = rows;
     maxColStart = cols;
   }

   if (maxRowStart <= 0 || maxColStart <= 0) {
    // not enough space in this direction
    continue;
   }

   int row = random.nextInt(maxRowStart);
   int col = random.nextInt(maxColStart);

   if (dir == Direction.DIAGONAL_UP_RIGHT && row - (word.length() - 1) < 0) {
    // adjust row so that it fits when going up
    row = word.length() - 1 + random.nextInt(rows - word.length() + 1);
   }

   if (canPlaceWord(grid, word, row, col, dir)) {
    placeWord(grid, word, row, col, dir);
    placements.add(new WordPlacement(word, row, col, dir));
    return true;
   }
  }

  return false;
 }

 private boolean canPlaceWord(char[][] grid,
   String word,
   int row,
   int col,
   Direction direction) {
  int rows = grid.length;
  int cols = grid[0].length;

  int dr;
  int dc;
  switch (direction) {
   case RIGHT:
    dr = 0;
    dc = 1;
    break;
   case DOWN:
    dr = 1;
    dc = 0;
    break;
   case DIAGONAL_DOWN_RIGHT:
    dr = 1;
    dc = 1;
    break;
   case DIAGONAL_UP_RIGHT:
    dr = -1;
    dc = 1;
    break;
   default:
    dr = 0;
    dc = 1;
  }

  int r = row;
  int c = col;

  for (int i = 0; i < word.length(); i++) {
   // out of bounds
   if (r < 0 || r >= rows || c < 0 || c >= cols) {
    return false;
   }
   char existing = grid[r][c];
   char ch = word.charAt(i);

   // if cell is not empty and not equal to the letter, collision
   if (existing != '\0' && existing != ch) {
    return false;
   }
   r += dr;
   c += dc;
  }

  return true;
 }

 private void placeWord(char[][] grid,
   String word,
   int row,
   int col,
   Direction direction) {
  int dr;
  int dc;
  switch (direction) {
   case RIGHT:
    dr = 0;
    dc = 1;
    break;
   case DOWN:
    dr = 1;
    dc = 0;
    break;
   case DIAGONAL_DOWN_RIGHT:
    dr = 1;
    dc = 1;
    break;
   case DIAGONAL_UP_RIGHT:
    dr = -1;
    dc = 1;
    break;
   default:
    dr = 0;
    dc = 1;
  }

  int r = row;
  int c = col;
  for (int i = 0; i < word.length(); i++) {
   grid[r][c] = word.charAt(i);
   r += dr;
   c += dc;
  }
 }

 private void fillEmptyCells(char[][] grid) {
  int rows = grid.length;
  int cols = grid[0].length;

  for (int r = 0; r < rows; r++) {
   for (int c = 0; c < cols; c++) {
    if (grid[r][c] == '\0') {
     grid[r][c] = randomLetter();
    }
   }
  }
 }

 private char randomLetter() {
  int idx = random.nextInt(alphabet.length());
  return alphabet.charAt(idx);
 }
}
