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
 * Builds word search puzzles (mots meles) from the Lingala lexicon database.
 * Same schema as Lexikongo, but using the Lingala DB.
 */
public class LingalaWordSearchService {

 private final WordSearchGenerator generator;

 public LingalaWordSearchService() {
  this.generator = new WordSearchGenerator();
 }

 public LingalaWordSearchService(WordSearchGenerator generator) {
  this.generator = (generator != null) ? generator : new WordSearchGenerator();
 }

 public WordSearchPuzzle generateRandomLingalaWordSearch(
   int rows,
   int cols,
   int maxWords,
   String meaningLanguageCode) throws SQLException {

  try (Connection conn = DbConfig.openLingalaLexConnection()) {
   LexikongoWordRepository wordRepo = new LexikongoWordRepository(conn);

   int fetchCount = maxWords * 3;
   List<LexWord> lexWords = wordRepo.findRandomWords(fetchCount, meaningLanguageCode);

   List<WordToFind> wordsToFind = toWordToFindList(
     lexWords,
     meaningLanguageCode,
     maxWords);

   return generator.generatePuzzle(
     "ln",
     "Mots meles Lingala",
     "Noms generiques",
     rows,
     cols,
     wordsToFind);
  }
 }

 public WordSearchPuzzle generateLingalaWordSearchForClass(
   int classId,
   int rows,
   int cols,
   int maxWords,
   String meaningLanguageCode) throws SQLException {

  try (Connection conn = DbConfig.openLingalaLexConnection()) {
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
     "ln",
     "Mots meles Lingala - Classe " + classId,
     theme,
     rows,
     cols,
     wordsToFind);
  }
 }

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
     baseForm,
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
  if (meaningLanguageCode != null && !meaningLanguageCode.isBlank()) {
   for (LexMeaning m : w.getMeanings()) {
    if (meaningLanguageCode.equalsIgnoreCase(m.getLanguageCode())) {
     return m.getMeaning();
    }
   }
  }
  return w.getMeanings().get(0).getMeaning();
 }
}
