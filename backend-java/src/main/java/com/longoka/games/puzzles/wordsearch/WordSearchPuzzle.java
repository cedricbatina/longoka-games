package com.longoka.games.puzzles.wordsearch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a word search puzzle: grid + words + placements.
 */
public final class WordSearchPuzzle {

 private final String id; // can be null if not persisted yet
 private final String languageCode; // "kg", "ln", "fr", etc.
 private final String title; // e.g. "Mots mêlés Kikongo - Niveaux 1"
 private final String theme; // optional, e.g. "Animaux", "Verbes"
 private final int rows;
 private final int cols;
 private final char[][] grid;
 private final List<WordToFind> words;
 private final List<WordPlacement> placements;

 public WordSearchPuzzle(
   String id,
   String languageCode,
   String title,
   String theme,
   int rows,
   int cols,
   char[][] grid,
   List<WordToFind> words,
   List<WordPlacement> placements) {
  this.id = id;
  this.languageCode = languageCode;
  this.title = title;
  this.theme = theme;
  this.rows = rows;
  this.cols = cols;
  this.grid = grid;
  this.words = new ArrayList<>(words);
  this.placements = new ArrayList<>(placements);
 }

 public String getId() {
  return id;
 }

 public String getLanguageCode() {
  return languageCode;
 }

 public String getTitle() {
  return title;
 }

 public String getTheme() {
  return theme;
 }

 public int getRows() {
  return rows;
 }

 public int getCols() {
  return cols;
 }

 public char[][] getGrid() {
  return grid;
 }

 /**
  * Convenience: returns the grid as a list of row strings (useful for JSON).
  */
 public List<String> getGridAsStrings() {
  List<String> rowsList = new ArrayList<>(rows);
  for (int r = 0; r < rows; r++) {
   rowsList.add(new String(grid[r]));
  }
  return Collections.unmodifiableList(rowsList);
 }

 public List<WordToFind> getWords() {
  return Collections.unmodifiableList(words);
 }

 public List<WordPlacement> getPlacements() {
  return Collections.unmodifiableList(placements);
 }
}
