package com.longoka.games.lexikongo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class LexikongoWordRepository {

  private final Connection connection;

  public LexikongoWordRepository(Connection connection) {
    this.connection = connection;
  }

  /**
   * Tire des mots au hasard (toutes classes confondues),
   * avec leurs significations dans la langue demandée.
   */
  public List<LexWord> findRandomWords(int limit, String languageCode) throws SQLException {
    return findRandomWordsInternal(null, limit, languageCode);
  }

  /**
   * Tire des mots au hasard, filtrés par class_id,
   * avec leurs significations dans la langue demandée.
   *
   * C'est cette méthode que tes services KikongoWordSearchService /
   * LingalaWordSearchService appellent.
   */
  public List<LexWord> findRandomWordsByClassId(int classId, int limit, String languageCode) throws SQLException {
    return findRandomWordsInternal(classId, limit, languageCode);
  }

  // ----------------------------------------------------------------------
  // Implementation interne
  // ----------------------------------------------------------------------

  private List<LexWord> findRandomWordsInternal(Integer classIdFilter, int limit, String languageCode)
      throws SQLException {

    // 1) On récupère les mots (sans les meanings pour l'instant)
    StringBuilder sql = new StringBuilder();
    sql.append("SELECT ")
        .append("w.word_id, w.singular, w.plural, w.root, ")
        .append("w.class_id, w.phonetic, w.derived_word, ")
        .append("w.derived_from_word, w.derived_from_verb, ")
        .append("w.is_approved, w.user_id, w.number_variability, ")
        .append("s.slug, nc.class_name ")
        .append("FROM words w ")
        .append("LEFT JOIN slugs s ON s.word_id = w.word_id AND s.content_type = 'word' ")
        .append("LEFT JOIN nominal_classes nc ON nc.class_id = w.class_id ")
        .append("WHERE w.is_approved = 1 ");

    if (classIdFilter != null) {
      sql.append("AND w.class_id = ? ");
    }

    sql.append("ORDER BY RAND() ");
    sql.append("LIMIT ?");

    Map<Integer, LexWord> wordsById = new LinkedHashMap<>();
    List<Integer> wordIds = new ArrayList<>();

    try (PreparedStatement ps = connection.prepareStatement(sql.toString())) {
      int index = 1;
      if (classIdFilter != null) {
        ps.setInt(index++, classIdFilter);
      }
      ps.setInt(index, limit);

      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          int wordId = rs.getInt("word_id");
          String singular = rs.getString("singular");
          String plural = rs.getString("plural");
          String root = rs.getString("root");
          int classId = rs.getInt("class_id");
          String className = rs.getString("class_name");
          String phonetic = rs.getString("phonetic");
          boolean derivedWord = rs.getBoolean("derived_word");
          Integer derivedFromWordId = getNullableInt(rs, "derived_from_word");
          Integer derivedFromVerbId = getNullableInt(rs, "derived_from_verb");
          boolean approved = rs.getBoolean("is_approved");
          Integer userId = getNullableInt(rs, "user_id");
          String numberVariability = rs.getString("number_variability");
          String slug = rs.getString("slug");

          NominalClass nominalClass = new NominalClass(classId, className);

          LexWord word = new LexWord(
              wordId,
              singular,
              plural,
              root,
              nominalClass,
              phonetic,
              derivedWord,
              derivedFromWordId,
              derivedFromVerbId,
              approved,
              userId,
              numberVariability,
              slug,
              new ArrayList<>());

          wordsById.put(wordId, word);
          wordIds.add(wordId);
        }
      }
    }

    if (wordsById.isEmpty()) {
      return new ArrayList<>();
    }

    // 2) On charge les significations pour ces word_id, filtrées par langue si
    // fournie
    loadMeaningsForWords(wordsById, wordIds, languageCode);

    return new ArrayList<>(wordsById.values());
  }

  private void loadMeaningsForWords(Map<Integer, LexWord> wordsById,
      List<Integer> wordIds,
      String languageCode) throws SQLException {

    if (wordIds.isEmpty()) {
      return;
    }

    StringBuilder sql = new StringBuilder();
    sql.append("SELECT meaning_id, word_id, language_code, meaning ")
        .append("FROM word_meanings ")
        .append("WHERE word_id IN (");

    for (int i = 0; i < wordIds.size(); i++) {
      if (i > 0) {
        sql.append(",");
      }
      sql.append("?");
    }
    sql.append(") ");

    // ⚠️ IMPORTANT : plus de filterByLang, plus de "language_code IN (...)"
    // On récupère toutes les meanings pour ces word_id,
    // et c’est le code Java (findMeaningTextForLanguage) qui filtrera par langue.

    try (PreparedStatement ps = connection.prepareStatement(sql.toString())) {
      int index = 1;
      for (Integer id : wordIds) {
        ps.setInt(index++, id);
      }

      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          int meaningId = rs.getInt("meaning_id");
          int wordId = rs.getInt("word_id");
          String lang = rs.getString("language_code");
          String meaningText = rs.getString("meaning");

          LexWord word = wordsById.get(wordId);
          if (word != null) {
            LexMeaning meaning = new LexMeaning(meaningId, lang, meaningText);
            word.addMeaning(meaning);
          }
        }
      }
    }
  }

  private Integer getNullableInt(ResultSet rs, String column) throws SQLException {
    int value = rs.getInt(column);
    return rs.wasNull() ? null : value;
  }
}
