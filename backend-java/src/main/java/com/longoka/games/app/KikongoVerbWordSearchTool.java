package com.longoka.games.app;

import com.longoka.games.lexikongo.KikongoVerbWordSearchService;
import com.longoka.games.puzzles.wordsearch.WordSearchPuzzle;
import com.longoka.games.puzzles.wordsearch.WordToFind;

public class KikongoVerbWordSearchTool {

  public static void main(String[] args) throws Exception {
    System.out.println("Config DB (local) chargée depuis " +
        System.getProperty("user.dir") + "/config/db-local.properties");

    KikongoVerbWordSearchService service = new KikongoVerbWordSearchService();

    // Une grille 12x12 avec 10 verbes
    WordSearchPuzzle puzzle = service.generateRandomKikongoVerbWordSearch(
        12,
        12,
        10,
        "fr");

    System.out.println("== MOTS MÊLÉS KIKONGO (verbes) ==");
    System.out.println("Titre : " + puzzle.getTitle());
    System.out.println("Thème : " + puzzle.getTheme());
    System.out.println();

    char[][] grid = puzzle.getGrid();
    for (char[] row : grid) {
      StringBuilder sb = new StringBuilder();
      for (char c : row) {
        sb.append(c);
      }
      System.out.println(sb);
    }

    System.out.println();
    System.out.println("Verbes à trouver :");
    for (WordToFind w : puzzle.getWords()) {
      System.out.println(" - " + w);
    }
  }
}
