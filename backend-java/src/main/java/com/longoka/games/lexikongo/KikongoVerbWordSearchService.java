package com.longoka.games.lexikongo;

import com.longoka.games.app.DbConfig;
import com.longoka.games.puzzles.wordsearch.WordSearchGenerator;
import com.longoka.games.puzzles.wordsearch.WordSearchPuzzle;
import com.longoka.games.puzzles.wordsearch.WordToFind;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Construit des grilles de mots mêlés à partir des verbes Kikongo.
 */
public class KikongoVerbWordSearchService {

  private final WordSearchGenerator generator;

  public KikongoVerbWordSearchService() {
    this.generator = new WordSearchGenerator();
  }

  public KikongoVerbWordSearchService(WordSearchGenerator generator) {
    this.generator = (generator != null) ? generator : new WordSearchGenerator();
  }

  /**
   * Génère une grille de mots mêlés avec des verbes Kikongo.
   *
   * @param rows                nombre de lignes
   * @param cols                nombre de colonnes
   * @param maxVerbs            nombre max de verbes à placer
   * @param meaningLanguageCode langue pour les définitions (ex: "fr")
   */
  public WordSearchPuzzle generateRandomKikongoVerbWordSearch(
      int rows,
      int cols,
      int maxVerbs,
      String meaningLanguageCode) throws SQLException {

    try (Connection conn = DbConfig.openKikongoLexConnection()) {
      LexikongoVerbRepository repo = new LexikongoVerbRepository(conn);

      // On récupère plus de verbes que nécessaire pour laisser de la marge au
      // générateur.
      int fetchCount = maxVerbs * 3;
      List<LexVerb> verbs = repo.findRandomVerbsByLength(
          4, // min length
          12, // max length
          fetchCount,
          meaningLanguageCode);

      List<WordToFind> wordsToFind = toWordToFindList(
          verbs,
          meaningLanguageCode,
          maxVerbs);

      return generator.generatePuzzle(
          "kg",
          "Mots meles Kikongo (verbes)",
          "Verbes Kikongo",
          rows,
          cols,
          wordsToFind);
    }
  }

  private List<WordToFind> toWordToFindList(
      List<LexVerb> verbs,
      String meaningLanguageCode,
      int maxVerbs) {
    List<WordToFind> result = new ArrayList<>();
    if (verbs == null) {
      return result;
    }

    for (LexVerb v : verbs) {
      if (result.size() >= maxVerbs) {
        break;
      }

      String baseForm = chooseVerbBaseForm(v);
      if (baseForm == null || baseForm.isBlank()) {
        continue;
      }

      String translation = findMeaningText(v, meaningLanguageCode);
      String slug = v.getSlug();

      WordToFind wordToFind = new WordToFind(
          baseForm, // baseForm
          baseForm, // displayForm
          translation, // translation
          slug, // slug (peut être null)
          "verb", // partOfSpeech
          null // extraInfo (on pourra mettre la racine, type de verbe, etc.)
      );
      result.add(wordToFind);
    }

    return result;
  }

  private String chooseVerbBaseForm(LexVerb v) {
    if (v == null) {
      return null;
    }
    // Pour l’instant, on utilise simplement le name (infinitif)
    if (v.getGridForm() != null && !v.getGridForm().isBlank()) {
      return v.getGridForm();
    }
    return null;
  }

  private String findMeaningText(LexVerb v, String meaningLanguageCode) {
    if (v.getMeanings() == null || v.getMeanings().isEmpty()) {
      return null;
    }

    if (meaningLanguageCode != null && !meaningLanguageCode.isBlank()) {
      for (LexMeaning m : v.getMeanings()) {
        if (meaningLanguageCode.equalsIgnoreCase(m.getLanguageCode())) {
          return m.getMeaning();
        }
      }
    }

    // fallback : premier sens quel que soit la langue
    return v.getMeanings().get(0).getMeaning();
  }
}
