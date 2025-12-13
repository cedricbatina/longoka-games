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
      String translationEn = findMeaningTextForLanguage(v, "en");
      String slug = v.getSlug();
      String phonetic = v.getPhonetic();

      WordToFind wordToFind = new WordToFind(
          baseForm, // baseForm
          baseForm, // displayForm
          translation, // traduction dans la langue demandée
          slug,
          "verb",
          null, // extraInfo (à remplir plus tard si besoin)
          phonetic,
          translationEn);
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

  /**
   * Concatène toutes les définitions pour une langue donnée (fr, en, etc.).
   * Les sens sont séparés par " ; " et les doublons exacts sont évités.
   */
  private String findMeaningTextForLanguage(LexVerb v, String languageCode) {
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

  private String findMeaningText(LexVerb v, String meaningLanguageCode) {
    if (v.getMeanings() == null || v.getMeanings().isEmpty()) {
      return null;
    }

    String byRequested = findMeaningTextForLanguage(v, meaningLanguageCode);
    if (byRequested != null) {
      return byRequested;
    }

    LexMeaning first = v.getMeanings().get(0);
    if (first == null) {
      return null;
    }
    return first.getMeaning();
  }

}