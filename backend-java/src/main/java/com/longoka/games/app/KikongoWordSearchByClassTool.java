package com.longoka.games.app;

import com.longoka.games.lexikongo.KikongoWordSearchService;
import com.longoka.games.puzzles.wordsearch.WordSearchPuzzle;
import com.longoka.games.puzzles.wordsearch.WordToFind;

public final class KikongoWordSearchByClassTool {

 private KikongoWordSearchByClassTool() {
 }

 public static void main(String[] args) throws Exception {
  // 👉 À adapter selon ton tableau nominal_classes
  // ex. 1 = mu-ba, 2 = mu-mi, etc. (tu peux changer la valeur ici)
  int classId = 1; // <-- change ce chiffre pour tester une autre classe
  int rows = 12;
  int cols = 12;
  int maxWords = 12;
  String meaningLang = "fr";

  KikongoWordSearchService service = new KikongoWordSearchService();

  WordSearchPuzzle puzzle = service.generateKikongoWordSearchForClass(
    classId,
    rows,
    cols,
    maxWords,
    meaningLang);

  printPuzzle(puzzle, classId);
 }

 private static void printPuzzle(WordSearchPuzzle puzzle, int classId) {
  System.out.println("== MOTS MÊLÉS KIKONGO (classe " + classId + ") ==");

  try {
   System.out.println("Titre : " + puzzle.getTitle());
   System.out.println("Thème : " + puzzle.getTheme());
  } catch (NoSuchMethodError | RuntimeException ignored) {
  }

  System.out.println();

  char[][] grid = puzzle.getGrid();
  for (char[] row : grid) {
   System.out.println(new String(row));
  }

  System.out.println();
  System.out.println("Mots à trouver :");
  for (WordToFind w : puzzle.getWords()) {
   System.out.println(" - " + w);
  }
 }
}
