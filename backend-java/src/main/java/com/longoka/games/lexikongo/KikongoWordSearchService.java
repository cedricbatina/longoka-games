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

   String translation = findMeaningText(w, meaningLanguageCode);
   String slug = w.getSlug();
   String extraInfo = null;
   if (w.getNominalClass() != null) {
    extraInfo = w.getNominalClass().getClassName();
   }

   WordToFind wordToFind = new WordToFind(
     baseForm,
     baseForm, // display form, can be customized later
     translation,
     slug,
     "noun",
     extraInfo);
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

 private String findMeaningText(LexWord w, String meaningLanguageCode) {
  if (w.getMeanings() == null || w.getMeanings().isEmpty()) {
   return null;
  }
  // First try with the requested language code
  if (meaningLanguageCode != null && !meaningLanguageCode.isBlank()) {
   for (LexMeaning m : w.getMeanings()) {
    if (meaningLanguageCode.equalsIgnoreCase(m.getLanguageCode())) {
     return m.getMeaning();
    }
   }
  }
  // Fallback: first meaning whatever the language
  return w.getMeanings().get(0).getMeaning();
 }
}
