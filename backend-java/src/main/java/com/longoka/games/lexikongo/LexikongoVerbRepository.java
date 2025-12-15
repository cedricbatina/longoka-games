package com.longoka.games.lexikongo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository pour les verbes Lexikongo (table verbs + verb_meanings + slugs).
 */
public class LexikongoVerbRepository {

  private final Connection connection;

  public LexikongoVerbRepository(Connection connection) {
    this.connection = connection;
  }

  /**
   * Récupère des verbes aléatoires dont la longueur du nom est comprise entre
   * minLength et maxLength, avec leurs significations (optionnellement filtrées
   * par code langue).
   *
   * @param minLength    longueur minimale du champ name
   * @param maxLength    longueur maximale du champ name
   * @param limit        nombre maximum de verbes retournés
   * @param languageCode code langue pour les meanings (ex: "fr"), ou null pour
   *                     tout
   */
  public List<LexVerb> findRandomVerbsByLength(
      int minLength,
      int maxLength,
      int limit,
      String languageCode) throws SQLException {

    // 1) On récupère d'abord les verbes (sans meanings)
    String sql = "SELECT " +
        "v.verb_id, v.name, v.root, v.suffix, v.phonetic, " +
        "v.active_verb, v.derived_verb, v.derived_from, " +
        "v.is_approved, v.user_id, " +
        "s.slug " +
        "FROM verbs v " +
        "LEFT JOIN slugs s " +
        "  ON s.verb_id = v.verb_id " +
        " AND s.content_type = 'verb' " +
        "WHERE v.is_approved = 1 " +
        "  AND v.active_verb = 1 " +
        "  AND CHAR_LENGTH(v.name) BETWEEN ? AND ? " +
        "ORDER BY RAND() " +
        "LIMIT ?";

    Map<Integer, LexVerb> byId = new LinkedHashMap<>();
    List<Integer> verbIds = new ArrayList<>();

    try (PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setInt(1, minLength);
      ps.setInt(2, maxLength);
      ps.setInt(3, limit);

      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          int verbId = rs.getInt("verb_id");
          if (byId.containsKey(verbId)) {
            continue;
          }

          String name = rs.getString("name");
          String root = rs.getString("root");
          String suffix = rs.getString("suffix");
          String phonetic = rs.getString("phonetic");
          boolean active = rs.getBoolean("active_verb");
          boolean derived = rs.getBoolean("derived_verb");
          Integer derivedFrom = (Integer) rs.getObject("derived_from");
          boolean isApproved = rs.getBoolean("is_approved");
          Integer userId = (Integer) rs.getObject("user_id");
          String slug = rs.getString("slug");

          LexVerb verb = new LexVerb(
              verbId,
              name,
              root,
              suffix,
              phonetic,
              active,
              derived,
              derivedFrom,
              isApproved,
              userId,
              slug,
              new ArrayList<>());

          byId.put(verbId, verb);
          verbIds.add(verbId);
        }
      }
    }

    if (verbIds.isEmpty()) {
      return new ArrayList<>();
    }

    // 2) On charge les meanings (toutes langues) pour ces verbes
    loadMeaningsForVerbs(byId, verbIds, languageCode);

    return new ArrayList<>(byId.values());
  }

  private void loadMeaningsForVerbs(Map<Integer, LexVerb> verbsById,
      List<Integer> verbIds,
      String languageCode) throws SQLException {

    if (verbIds.isEmpty()) {
      return;
    }

    StringBuilder sql = new StringBuilder();
    sql.append("SELECT meaning_id, verb_id, language_code, meaning ")
        .append("FROM verb_meanings ")
        .append("WHERE verb_id IN (");

    for (int i = 0; i < verbIds.size(); i++) {
      if (i > 0) {
        sql.append(",");
      }
      sql.append("?");
    }
    sql.append(")");

    // 👉 pas de filtre par langue ici.
    // On récupère toutes les meanings pour ces verbes,
    // et ce sera LexVerb / le service qui feront le tri (fr, en, etc.).

    try (PreparedStatement ps = connection.prepareStatement(sql.toString())) {
      int index = 1;
      for (Integer id : verbIds) {
        ps.setInt(index++, id);
      }

      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          int meaningId = rs.getInt("meaning_id");
          int verbId = rs.getInt("verb_id");
          String lang = rs.getString("language_code");
          String meaningText = rs.getString("meaning");

          LexVerb verb = verbsById.get(verbId);
          if (verb != null) {
            LexMeaning meaning = new LexMeaning(meaningId, lang, meaningText);
            verb.addMeaning(meaning);
          }
        }
      }
    }
  }
}
