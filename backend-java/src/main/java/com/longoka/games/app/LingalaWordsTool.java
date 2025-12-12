package com.longoka.games.app;

import com.longoka.games.lexikongo.LexikongoWordRepository;
import com.longoka.games.lexikongo.LexWord;
import com.longoka.games.lexikongo.LexMeaning;

import java.sql.Connection;
import java.util.List;

public class LingalaWordsTool {

 public static void main(String[] args) throws Exception {
  System.out.println("== Mots Lingala depuis 6i695q_lexilingala (avec définitions) ==");

  try (Connection conn = DbConfig.openLingalaLexConnection()) {

   LexikongoWordRepository repo = new LexikongoWordRepository(conn);

   // On tire 20 mots, avec priorité sur les définitions FR
   List<LexWord> words = repo.findRandomWords(20, "fr");

   for (LexWord w : words) {
    System.out.println(w);

    List<LexMeaning> meanings = w.getMeanings();
    if (meanings == null || meanings.isEmpty()) {
     System.out.println("  (aucune définition trouvée)");
    } else {
     System.out.println("  Définitions :");
     for (LexMeaning m : meanings) {
      System.out.println("    [" + m.getLanguageCode() + "] " + m.getMeaning());
     }
    }

    System.out.println();
   }
  }
 }
}
