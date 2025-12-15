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
 * Builds word search puzzles (mots meles) from the Kikongo lexicon database.
 */
public class KikongoWordSearchService {

  private final WordSearchGenerator generator;

  public KikongoWordSearchService() {
    this.generator = new WordSearchGenerator();
  }

  public KikongoWordSearchService(WordSearchGenerator generator) {
    this.generator = (generator != null) ? generator : new WordSearchGenerator();
  }

  /**
   * Generate a random word search puzzle with Kikongo nouns.
   *
   * @param rows                number of rows in the grid
   * @param cols                number of columns in the grid
   * @param maxWords            maximum number of words to place
   * @param meaningLanguageCode language code for meanings (for example "fr")
   */
  public WordSearchPuzzle generateRandomKikongoWordSearch(
      int rows,
      int cols,
      int maxWords,
      String meaningLanguageCode) throws SQLException {

    try (Connection conn = DbConfig.openKikongoLexConnection()) {
      LexikongoWordRepository wordRepo = new LexikongoWordRepository(conn);

      // We fetch more words than needed to give the generator some freedom.
      int fetchCount = maxWords * 3;
      List<LexWord> lexWords = wordRepo.findRandomWords(fetchCount, meaningLanguageCode);

      List<WordToFind> wordsToFind = toWordToFindList(
          lexWords,
          meaningLanguageCode,
          maxWords);

      return generator.generatePuzzle(
          "kg", // language code for puzzle
          "Mots meles Kikongo", // title
          "Noms generiques", // simple theme
          rows,
          cols,
          wordsToFind);
    }
  }

  /**
   * Generate a word search puzzle restricted to one nominal class.
   *
   * @param classId             nominal class id (from nominal_classes)
   * @param rows                grid rows
   * @param cols                grid cols
   * @param maxWords            max words to place
   * @param meaningLanguageCode meaning language code ("fr", "en", etc.)
   */
  public WordSearchPuzzle generateKikongoWordSearchForClass(
      int classId,
      int rows,
      int cols,
      int maxWords,
      String meaningLanguageCode) throws SQLException {

    try (Connection conn = DbConfig.openKikongoLexConnection()) {
      LexikongoWordRepository wordRepo = new LexikongoWordRepository(conn);

      int fetchCount = maxWords * 3;
      List<LexWord> lexWords = wordRepo.findRandomWordsByClassId(
          classId,
          fetchCount,
          meaningLanguageCode);

      List<WordToFind> wordsToFind = toWordToFindList(
          lexWords,
          meaningLanguageCode,
          maxWords);

      String theme = "Classe nominale " + classId;

      return generator.generatePuzzle(
          "kg",
          "Mots meles Kikongo - Classe " + classId,
          theme,
          rows,
          cols,
          wordsToFind);
    }
  }

  /**
   * Convert LexWord objects to WordToFind objects for the puzzle.
   */
  private List<WordToFind> toWordToFindList(
      List<LexWord> lexWords,
      String meaningLanguageCode,
      int maxWords) {
    List<WordToFind> result = new ArrayList<>();
    if (lexWords == null) {
      return result;
    }

    for (LexWord w : lexWords) {
      if (result.size() >= maxWords) {
        break;
      }

      String baseForm = chooseBaseForm(w);
      if (baseForm == null || baseForm.isBlank()) {
        continue;
      }

      // FR = toutes les définitions françaises, concaténées
      String translation;
      if ("fr".equalsIgnoreCase(meaningLanguageCode)) {
        translation = w.getFrenchMeaningsJoined();
      } else if ("en".equalsIgnoreCase(meaningLanguageCode)) {
        translation = w.getEnglishMeaningsJoined();
      } else {
        // fallback pour d'autres langues éventuelles (lingala plus tard, etc.)
        translation = findMeaningText(w, meaningLanguageCode);
      }

      // EN = toujours toutes les définitions anglaises si la DB en contient
      String translationEn = w.getEnglishMeaningsJoined();

      String slug = w.getSlug();

      String extraInfo = null;
      if (w.getNominalClass() != null) {
        extraInfo = w.getNominalClass().getClassName();
      }

      String phonetic = w.getPhonetic();

      WordToFind wordToFind = new WordToFind(
          baseForm,
          baseForm, // display form
          translation, // traduction dans la langue demandée (souvent "fr")
          slug,
          "noun",
          extraInfo,
          phonetic, // nouvelle info
          translationEn // traduction anglaise si disponible
      );
      result.add(wordToFind);

    }

    return result;
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

  /**
   * Concatène toutes les définitions pour une langue donnée (fr, en, etc.).
   * Les sens sont séparés par " ; " et les doublons exacts sont évités.
   */
  private String findMeaningTextForLanguage(LexWord w, String languageCode) {
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

  /**
   * Retourne les définitions dans la langue demandée,
   * ou, en fallback, la toute première définition.
   */
  private String findMeaningText(LexWord w, String meaningLanguageCode) {
    if (w.getMeanings() == null || w.getMeanings().isEmpty()) {
      return null;
    }

    // 1) on essaie dans la langue demandée (fr, en, etc.) en fusionnant
    String byRequested = findMeaningTextForLanguage(w, meaningLanguageCode);
    if (byRequested != null) {
      return byRequested;
    }

    // 2) fallback : première définition, quelle que soit la langue
    LexMeaning first = w.getMeanings().get(0);
    if (first == null) {
      return null;
    }
    return first.getMeaning();
  }

}
