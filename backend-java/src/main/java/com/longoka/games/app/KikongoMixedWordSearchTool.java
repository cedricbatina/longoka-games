package com.longoka.games.app;

import com.longoka.games.lexikongo.KikongoMixedWordSearchService;
import com.longoka.games.puzzles.wordsearch.WordSearchPuzzle;
import com.longoka.games.puzzles.wordsearch.WordToFind;

import java.util.List;

public class KikongoMixedWordSearchTool {

  public static void main(String[] args) throws Exception {
    System.out.println("== Mots mêlés Kikongo (noms + verbes) ==");

    // Pas besoin d'ouvrir la connexion ici :
    // KikongoMixedWordSearchService utilise déjà
    // DbConfig.openKikongoLexConnection()
    KikongoMixedWordSearchService service = new KikongoMixedWordSearchService();

    // Une petite grille 12x12, avec 12 entrées max, définitions en FR
    WordSearchPuzzle puzzle = service.generateMixedKikongoWordSearch(
        12, // rows
        12, // cols
        12, // max words
        "fr" // langage des définitions
    );

    System.out.println("Titre : " + puzzle.getTitle());
    System.out.println("Thème : " + puzzle.getTheme());
    System.out.println();

    // Affichage de la grille
    char[][] grid = puzzle.getGrid();
    for (char[] row : grid) {
      System.out.println(new String(row));
    }

    System.out.println();
    System.out.println("Entrées à trouver :");

    // Ici j’assume que WordSearchPuzzle a une méthode getWords()
    // (comme ce qu’on a dû faire pour les autres tools de mots mêlés).
    // Si le nom exact diffère (getTargets(), getWordList(), etc.),
    // il suffira de changer cette ligne.
    List<WordToFind> words = puzzle.getWords();

    if (words == null || words.isEmpty()) {
      System.out.println("  (aucune entrée renvoyée par le puzzle)");
    } else {
      for (WordToFind w : words) {
        String base = w.getBaseForm();
        String pos = w.getPartOfSpeech();
        String extra = w.getExtraInfo();
        String translation = w.getTranslation();

        StringBuilder line = new StringBuilder(" - ");
        line.append(base != null ? base : "<?>");

        if (pos != null) {
          line.append(" [").append(pos).append("]");
        }
        if (extra != null) {
          line.append(" {").append(extra).append("}");
        }
        if (translation != null) {
          line.append(" : ").append(translation);
        }

        System.out.println(line);
      }
    }
  }
}
