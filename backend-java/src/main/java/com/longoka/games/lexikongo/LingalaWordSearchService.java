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
 * Builds word search puzzles (mots mêlés) from the Lingala lexicon database.
 * (copie du KikongoWordSearchService, mais pour Lingala)
 */
public class LingalaWordSearchService {

 private final WordSearchGenerator generator;

 public LingalaWordSearchService() {
  this.generator = new WordSearchGenerator();
 }

 public LingalaWordSearchService(WordSearchGenerator generator) {
  this.generator = (generator != null) ? generator : new WordSearchGenerator();
 }

 /**
  * Generate a random word search puzzle with Lingala nouns.
  *
  * @param rows                number of rows in the grid
  * @param cols                number of columns in the grid
  * @param maxWords            maximum number of words to place
  * @param meaningLanguageCode language code for meanings (for example "fr")
  */
 public WordSearchPuzzle generateRandomLingalaWordSearch(
   int rows,
   int cols,
   int maxWords,
   String meaningLanguageCode) throws SQLException {

  try (Connection conn = DbConfig.openLingalaLexConnection()) {
   LexikongoWordRepository wordRepo = new LexikongoWordRepository(conn);

   // We fetch more words than needed to give the generator some freedom.
   int fetchCount = maxWords * 3;
   List<LexWord> lexWords = wordRepo.findRandomWords(fetchCount, meaningLanguageCode);

   List<WordToFind> wordsToFind = toWordToFindList(
     lexWords,
     meaningLanguageCode,
     maxWords);

   return generator.generatePuzzle(
     "ln", // language code for puzzle (Lingala)
     "Mots meles Lingala",
     "Noms generiques",
     rows,
     cols,
     wordsToFind);
  }
 }

 /**
  * Convert LexWord objects to WordToFind objects for the puzzle.
  * Même logique que KikongoWordSearchService, avec FR/EN fusionnés.
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

   // FR fusionné
   String translationFr = w.getFrenchMeaningsJoined();
   if (translationFr == null) {
    translationFr = findMeaningText(w, meaningLanguageCode);
   }

   // EN fusionné
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
     translationFr, // FR
     slug,
     "noun",
     extraInfo,
     phonetic,
     translationEn // EN
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

  String byRequested = findMeaningTextForLanguage(w, meaningLanguageCode);
  if (byRequested != null) {
   return byRequested;
  }

  LexMeaning first = w.getMeanings().get(0);
  if (first == null) {
   return null;
  }
  return first.getMeaning();
 }

}
