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

    String sql = "SELECT " +
        "v.verb_id, v.name, v.root, v.suffix, v.phonetic, " +
        "v.active_verb, v.derived_verb, v.derived_from, " +
        "v.is_approved, v.user_id, " +
        "s.slug, " +
        "vm.meaning_id, vm.language_code, vm.meaning " +
        "FROM verbs v " +
        "LEFT JOIN slugs s " +
        "  ON s.verb_id = v.verb_id " +
        " AND s.content_type = 'verb' " +
        "LEFT JOIN verb_meanings vm " +
        "  ON vm.verb_id = v.verb_id " +
        "WHERE v.is_approved = 1 " +
        "  AND v.active_verb = 1 " +
        "  AND CHAR_LENGTH(v.name) BETWEEN ? AND ? " +
        "  AND ( ? IS NULL OR vm.language_code = ? ) " +
        "ORDER BY RAND() " +
        "LIMIT ?";

    try (PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setInt(1, minLength);
      ps.setInt(2, maxLength);
      ps.setString(3, languageCode);
      ps.setString(4, languageCode);
      ps.setInt(5, limit);

      try (ResultSet rs = ps.executeQuery()) {
        Map<Integer, LexVerb> byId = new LinkedHashMap<>();

        while (rs.next()) {
          int verbId = rs.getInt("verb_id");
          LexVerb verb = byId.get(verbId);

          if (verb == null) {
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

            verb = new LexVerb(
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
          }

          int meaningId = rs.getInt("meaning_id");
          if (!rs.wasNull()) {
            String mLang = rs.getString("language_code");
            String mText = rs.getString("meaning");
            LexMeaning meaning = new LexMeaning(meaningId, mLang, mText);
            verb.addMeaning(meaning);
          }
        }

        return new ArrayList<>(byId.values());
      }
    }
  }
}
