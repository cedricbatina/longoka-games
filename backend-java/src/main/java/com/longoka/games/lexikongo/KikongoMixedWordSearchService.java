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
 * Service pour générer des mots mêlés KIKONGO
 * qui mélangent des noms (words) et des verbes (verbs).
 */
public class KikongoMixedWordSearchService {

  private final WordSearchGenerator generator;

  public KikongoMixedWordSearchService() {
    this.generator = new WordSearchGenerator();
  }

  public KikongoMixedWordSearchService(WordSearchGenerator generator) {
    this.generator = (generator != null) ? generator : new WordSearchGenerator();
  }

  /**
   * Génère un mot mêlé mixte (noms + verbes) en kikongo.
   *
   * @param rows                nombre de lignes
   * @param cols                nombre de colonnes
   * @param maxWords            nombre maximal total de mots (noms + verbes)
   * @param meaningLanguageCode code langue pour les définitions ("fr", "en", ...)
   */
  public WordSearchPuzzle generateMixedKikongoWordSearch(
      int rows,
      int cols,
      int maxWords,
      String meaningLanguageCode) throws SQLException {

    try (Connection conn = DbConfig.openKikongoLexConnection()) {
      LexikongoWordRepository wordRepo = new LexikongoWordRepository(conn);
      LexikongoVerbRepository verbRepo = new LexikongoVerbRepository(conn);

      int nounTarget = maxWords / 2;
      int verbTarget = maxWords - nounTarget;

      // On sur-sélectionne un peu pour laisser de la marge au générateur
      int nounFetch = nounTarget * 3;
      int verbFetch = verbTarget * 3;

      List<LexWord> lexWords = wordRepo.findRandomWords(nounFetch, meaningLanguageCode);
      List<LexVerb> lexVerbs = verbRepo.findRandomVerbsByLength(
          3,
          12,
          verbFetch,
          meaningLanguageCode);

      List<WordToFind> wordsToFind = new ArrayList<>();

      fillFromNouns(wordsToFind, lexWords, meaningLanguageCode, nounTarget);
      fillFromVerbs(wordsToFind, lexVerbs, meaningLanguageCode, verbTarget);

      // Si on n’atteint pas exactement maxWords (pas assez de données en DB),
      // ce n’est pas grave : la grille sera juste un peu moins chargée.
      return generator.generatePuzzle(
          "kg",
          "Mots meles Kikongo (noms + verbes)",
          "Noms + verbes",
          rows,
          cols,
          wordsToFind);
    }
  }

  /* ===== Helpers pour les noms ===== */
  private void fillFromNouns(
      List<WordToFind> target,
      List<LexWord> lexWords,
      String meaningLanguageCode,
      int limit) {
    if (lexWords == null) {
      return;
    }

    int count = 0;
    for (LexWord w : lexWords) {
      if (count >= limit) {
        break;
      }
      String baseForm = chooseBaseForm(w);
      if (baseForm == null || baseForm.isBlank()) {
        continue;
      }

      // La traduction principale suit la langue demandee (fr, en, pt, ...).
      String translationPrimary = findMeaningTextFromWord(w, meaningLanguageCode);
      if (translationPrimary == null) {
        translationPrimary = w.getFrenchMeaningsJoined();
      }

      // 🔹 Toutes les traductions EN fusionnées
      String translationEn = w.getEnglishMeaningsJoined();

      String slug = w.getSlug();
      String extraInfo = null;
      if (w.getNominalClass() != null) {
        extraInfo = w.getNominalClass().getClassName();
      }
      String phonetic = w.getPhonetic();

      WordToFind wordToFind = new WordToFind(
          baseForm,
          baseForm, // forme affichée
          translationPrimary,
          slug,
          "noun",
          extraInfo,
          phonetic,
          translationEn // EN fusionné (ou null si pas de données)
      );

      target.add(wordToFind);
      count++;
    }
  }

  private String chooseBaseForm(LexWord w) {
    if (w.getSingular() != null && !w.getSingular().isBlank()) {
      return w.getSingular();
    }
    if (w.getPlural() != null && !w.getPlural().isBlank()) {
      return w.getPlural();
    }
    if (w.getRoot() != null && !w.getRoot().isBlank()) {
      return w.getRoot();
    }
    return null;
  }

  private String findMeaningTextForLanguageFromWord(LexWord w, String languageCode) {
    if (w.getMeanings() == null || w.getMeanings().isEmpty()) {
      return null;
    }
    if (languageCode == null || languageCode.isBlank()) {
      return null;
    }
    java.util.LinkedHashSet<String> texts = new java.util.LinkedHashSet<>();
    for (LexMeaning m : w.getMeanings()) {
      if (m == null) {
        continue;
      }
      if (languageCode.equalsIgnoreCase(m.getLanguageCode())) {
        String t = m.getMeaning();
        if (t != null) {
          t = t.trim();
        }
        if (t != null && !t.isEmpty()) {
          texts.add(t);
        }
      }
    }
    if (texts.isEmpty()) {
      return null;
    }
    return String.join(" ; ", texts);
  }

  private String findMeaningTextFromWord(LexWord w, String meaningLanguageCode) {
    if (w.getMeanings() == null || w.getMeanings().isEmpty()) {
      return null;
    }
    if (meaningLanguageCode != null && !meaningLanguageCode.isBlank()) {
      for (LexMeaning m : w.getMeanings()) {
        if (meaningLanguageCode.equalsIgnoreCase(m.getLanguageCode())) {
          return m.getMeaning();
        }
      }
    }
    // fallback : première définition
    return w.getMeanings().get(0).getMeaning();
  }

  /* ===== Helpers pour les verbes ===== */

  private void fillFromVerbs(
      List<WordToFind> target,
      List<LexVerb> lexVerbs,
      String meaningLanguageCode,
      int limit) {
    if (lexVerbs == null) {
      return;
    }

    int count = 0;
    for (LexVerb v : lexVerbs) {
      if (count >= limit) {
        break;
      }

      String baseForm = chooseVerbBaseForm(v);
      if (baseForm == null || baseForm.isBlank()) {
        continue;
      }

      String translationPrimary = findMeaningTextFromVerb(v, meaningLanguageCode);
      if (translationPrimary == null) {
        translationPrimary = v.getFrenchMeaningsJoined();
      }
      String translationEn = v.getEnglishMeaningsJoined();
      String slug = v.getSlug();
      String phonetic = v.getPhonetic();

      WordToFind wordToFind = new WordToFind(
          baseForm,
          baseForm,
          translationPrimary,
          slug,
          "verb",
          null,
          phonetic,
          translationEn);

      target.add(wordToFind);
      count++;
    }
  }

  private String chooseVerbBaseForm(LexVerb v) {
    if (v == null) {
      return null;
    }
    if (v.getGridForm() != null && !v.getGridForm().isBlank()) {
      return v.getGridForm();
    }
    return v.getName();
  }

  private String findMeaningTextForLanguageFromVerb(LexVerb v, String languageCode) {
    if (v.getMeanings() == null || v.getMeanings().isEmpty()) {
      return null;
    }
    if (languageCode == null || languageCode.isBlank()) {
      return null;
    }
    java.util.LinkedHashSet<String> texts = new java.util.LinkedHashSet<>();
    for (LexMeaning m : v.getMeanings()) {
      if (m == null) {
        continue;
      }
      if (languageCode.equalsIgnoreCase(m.getLanguageCode())) {
        String t = m.getMeaning();
        if (t != null) {
          t = t.trim();
        }
        if (t != null && !t.isEmpty()) {
          texts.add(t);
        }
      }
    }
    if (texts.isEmpty()) {
      return null;
    }
    return String.join(" ; ", texts);
  }

  private String findMeaningTextFromVerb(LexVerb v, String meaningLanguageCode) {
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
    return v.getMeanings().get(0).getMeaning();
  }
}
