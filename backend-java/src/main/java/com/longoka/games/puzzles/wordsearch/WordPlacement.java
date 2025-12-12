package com.longoka.games.puzzles.wordsearch;

/**
 * Represents the placement of a word in the grid.
 */
public final class WordPlacement {

 private final String word; // word as written in the grid (upper-case, no spaces)
 private final int row; // start row (0-based)
 private final int col; // start column (0-based)
 private final Direction direction;

 public WordPlacement(String word, int row, int col, Direction direction) {
  this.word = word;
  this.row = row;
  this.col = col;
  this.direction = direction;
 }

 public String getWord() {
  return word;
 }

 public int getRow() {
  return row;
 }

 public int getCol() {
  return col;
 }

 public Direction getDirection() {
  return direction;
 }

 @Override
 public String toString() {
  return "WordPlacement{" +
    "word='" + word + '\'' +
    ", row=" + row +
    ", col=" + col +
    ", direction=" + direction +
    '}';
 }
}
