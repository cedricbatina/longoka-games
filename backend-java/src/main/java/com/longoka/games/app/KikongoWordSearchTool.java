package com.longoka.games.app;

import com.longoka.games.lexikongo.KikongoWordSearchService;
import com.longoka.games.puzzles.wordsearch.WordSearchPuzzle;
import com.longoka.games.puzzles.wordsearch.WordToFind;

public final class KikongoWordSearchTool {

 private KikongoWordSearchTool() {
 }

 public static void main(String[] args) throws Exception {
  // paramètres de la grille : à ajuster comme tu veux
  int rows = 12;
  int cols = 12;
  int maxWords = 12;
  String meaningLang = "fr";

  KikongoWordSearchService service = new KikongoWordSearchService();

  WordSearchPuzzle puzzle = service.generateRandomKikongoWordSearch(
    rows,
    cols,
    maxWords,
    meaningLang);

  // Affichage simple dans la console
  printPuzzle(puzzle);
 }

 private static void printPuzzle(WordSearchPuzzle puzzle) {
  System.out.println("== MOTS MÊLÉS KIKONGO ==");
  try {
   // si tu as getTitle() / getTheme(), on les affiche
   System.out.println("Titre : " + puzzle.getTitle());
   System.out.println("Thème : " + puzzle.getTheme());
  } catch (NoSuchMethodError | RuntimeException ignored) {
   // au cas où ces méthodes n'existent pas, on n'explose pas
  }
  System.out.println();

  // Grille
  char[][] grid = puzzle.getGrid(); // supposé exister
  for (char[] row : grid) {
   System.out.println(new String(row));
  }

  System.out.println();
  System.out.println("Mots à trouver :");
  for (WordToFind w : puzzle.getWords()) { // supposé exister
   // on se contente de toString() pour ne pas dépendre de getters
   System.out.println(" - " + w);
  }
 }
}
