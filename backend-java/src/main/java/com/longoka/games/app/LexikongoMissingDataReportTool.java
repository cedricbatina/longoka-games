package com.longoka.games.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Exporte un rapport JSON des entrees Lexikongo qui manquent
 * de phonétique ou de significations FR/EN.
 */
public final class LexikongoMissingDataReportTool {

  private LexikongoMissingDataReportTool() {
  }

  public static void main(String[] args) throws Exception {
    String outputPath = (args != null && args.length > 0 && args[0] != null && !args[0].isBlank())
        ? args[0]
        : "target/lexikongo-missing-data-report.json";

    Report report = new Report();
    report.generatedAt = Instant.now().toString();
    report.source = "lexikongo";

    try (Connection conn = DbConfig.openKikongoLexConnection()) {
      report.words = loadWords(conn);
      report.verbs = loadVerbs(conn);
    }

    report.summary = buildSummary(report.words, report.verbs);

    Path out = Path.of(outputPath);
    if (out.getParent() != null) {
      Files.createDirectories(out.getParent());
    }

    ObjectMapper mapper = new ObjectMapper();
    mapper.enable(SerializationFeature.INDENT_OUTPUT);
    mapper.writeValue(out.toFile(), report);

    Map<String, Object> wordSummary = summaryMap(report.summary, "words");
    Map<String, Object> verbSummary = summaryMap(report.summary, "verbs");
    System.out.println("✅ Rapport Lexikongo généré : " + out.toAbsolutePath());
    System.out.println("Words incomplets : " + wordSummary.get("total"));
    System.out.println("Verbs incomplets : " + verbSummary.get("total"));
  }

  private static List<Entry> loadWords(Connection conn) throws SQLException {
    String sql = """
        SELECT
          w.word_id AS id,
          COALESCE(NULLIF(TRIM(w.singular), ''), NULLIF(TRIM(w.plural), '')) AS label,
          NULLIF(TRIM(w.singular), '') AS singular,
          NULLIF(TRIM(w.plural), '') AS plural,
          NULLIF(TRIM(w.root), '') AS root,
          NULLIF(TRIM(w.phonetic), '') AS phonetic,
          s.slug AS slug,
          w.class_id AS nominal_class_id,
          COALESCE(NULLIF(TRIM(nc.class_name), ''), CONCAT('class-', w.class_id)) AS nominal_class_name,
          SUM(CASE
                WHEN LOWER(TRIM(wm.language_code)) = 'fr'
                 AND NULLIF(TRIM(wm.meaning), '') IS NOT NULL
                THEN 1 ELSE 0
              END) AS fr_count,
          SUM(CASE
                WHEN LOWER(TRIM(wm.language_code)) = 'en'
                 AND NULLIF(TRIM(wm.meaning), '') IS NOT NULL
                THEN 1 ELSE 0
              END) AS en_count,
          GROUP_CONCAT(DISTINCT CASE
                WHEN LOWER(TRIM(wm.language_code)) = 'fr'
                 AND NULLIF(TRIM(wm.meaning), '') IS NOT NULL
                THEN TRIM(wm.meaning)
                ELSE NULL
              END SEPARATOR ' ; ') AS fr_meanings,
          GROUP_CONCAT(DISTINCT CASE
                WHEN LOWER(TRIM(wm.language_code)) = 'en'
                 AND NULLIF(TRIM(wm.meaning), '') IS NOT NULL
                THEN TRIM(wm.meaning)
                ELSE NULL
              END SEPARATOR ' ; ') AS en_meanings
        FROM words w
        LEFT JOIN slugs s
          ON s.word_id = w.word_id
         AND s.content_type = 'word'
        LEFT JOIN nominal_classes nc
          ON nc.class_id = w.class_id
        LEFT JOIN word_meanings wm
          ON wm.word_id = w.word_id
        WHERE w.is_approved = 1
        GROUP BY
          w.word_id,
          w.singular,
          w.plural,
          w.root,
          w.phonetic,
          s.slug,
          w.class_id,
          nc.class_name
        HAVING
          NULLIF(TRIM(w.phonetic), '') IS NULL
          OR SUM(CASE
              WHEN LOWER(TRIM(wm.language_code)) = 'fr'
               AND NULLIF(TRIM(wm.meaning), '') IS NOT NULL
              THEN 1 ELSE 0
            END) = 0
          OR SUM(CASE
              WHEN LOWER(TRIM(wm.language_code)) = 'en'
               AND NULLIF(TRIM(wm.meaning), '') IS NOT NULL
              THEN 1 ELSE 0
            END) = 0
        ORDER BY w.word_id ASC
        """;

    List<Entry> items = new ArrayList<>();
    try (PreparedStatement ps = conn.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {
      while (rs.next()) {
        Entry entry = new Entry();
        entry.lexicalType = "word";
        entry.id = rs.getInt("id");
        entry.label = rs.getString("label");
        entry.slug = rs.getString("slug");
        entry.root = rs.getString("root");
        entry.phonetic = rs.getString("phonetic");
        entry.nominalClassId = nullableInt(rs, "nominal_class_id");
        entry.nominalClassName = rs.getString("nominal_class_name");
        entry.singular = rs.getString("singular");
        entry.plural = rs.getString("plural");
        entry.frMeaningCount = rs.getInt("fr_count");
        entry.enMeaningCount = rs.getInt("en_count");
        entry.frMeanings = rs.getString("fr_meanings");
        entry.enMeanings = rs.getString("en_meanings");
        entry.missingFields = computeMissingFields(entry.phonetic, entry.frMeaningCount, entry.enMeaningCount);
        items.add(entry);
      }
    }
    return items;
  }

  private static List<Entry> loadVerbs(Connection conn) throws SQLException {
    String sql = """
        SELECT
          v.verb_id AS id,
          NULLIF(TRIM(v.name), '') AS label,
          NULLIF(TRIM(v.root), '') AS root,
          NULLIF(TRIM(v.suffix), '') AS suffix,
          NULLIF(TRIM(v.phonetic), '') AS phonetic,
          s.slug AS slug,
          SUM(CASE
                WHEN LOWER(TRIM(vm.language_code)) = 'fr'
                 AND NULLIF(TRIM(vm.meaning), '') IS NOT NULL
                THEN 1 ELSE 0
              END) AS fr_count,
          SUM(CASE
                WHEN LOWER(TRIM(vm.language_code)) = 'en'
                 AND NULLIF(TRIM(vm.meaning), '') IS NOT NULL
                THEN 1 ELSE 0
              END) AS en_count,
          GROUP_CONCAT(DISTINCT CASE
                WHEN LOWER(TRIM(vm.language_code)) = 'fr'
                 AND NULLIF(TRIM(vm.meaning), '') IS NOT NULL
                THEN TRIM(vm.meaning)
                ELSE NULL
              END SEPARATOR ' ; ') AS fr_meanings,
          GROUP_CONCAT(DISTINCT CASE
                WHEN LOWER(TRIM(vm.language_code)) = 'en'
                 AND NULLIF(TRIM(vm.meaning), '') IS NOT NULL
                THEN TRIM(vm.meaning)
                ELSE NULL
              END SEPARATOR ' ; ') AS en_meanings
        FROM verbs v
        LEFT JOIN slugs s
          ON s.verb_id = v.verb_id
         AND s.content_type = 'verb'
        LEFT JOIN verb_meanings vm
          ON vm.verb_id = v.verb_id
        WHERE v.is_approved = 1
          AND v.active_verb = 1
        GROUP BY
          v.verb_id,
          v.name,
          v.root,
          v.suffix,
          v.phonetic,
          s.slug
        HAVING
          NULLIF(TRIM(v.phonetic), '') IS NULL
          OR SUM(CASE
              WHEN LOWER(TRIM(vm.language_code)) = 'fr'
               AND NULLIF(TRIM(vm.meaning), '') IS NOT NULL
              THEN 1 ELSE 0
            END) = 0
          OR SUM(CASE
              WHEN LOWER(TRIM(vm.language_code)) = 'en'
               AND NULLIF(TRIM(vm.meaning), '') IS NOT NULL
              THEN 1 ELSE 0
            END) = 0
        ORDER BY v.verb_id ASC
        """;

    List<Entry> items = new ArrayList<>();
    try (PreparedStatement ps = conn.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {
      while (rs.next()) {
        Entry entry = new Entry();
        entry.lexicalType = "verb";
        entry.id = rs.getInt("id");
        entry.label = rs.getString("label");
        entry.slug = rs.getString("slug");
        entry.root = rs.getString("root");
        entry.suffix = rs.getString("suffix");
        entry.phonetic = rs.getString("phonetic");
        entry.frMeaningCount = rs.getInt("fr_count");
        entry.enMeaningCount = rs.getInt("en_count");
        entry.frMeanings = rs.getString("fr_meanings");
        entry.enMeanings = rs.getString("en_meanings");
        entry.missingFields = computeMissingFields(entry.phonetic, entry.frMeaningCount, entry.enMeaningCount);
        items.add(entry);
      }
    }
    return items;
  }

  private static List<String> computeMissingFields(String phonetic, int frMeaningCount, int enMeaningCount) {
    List<String> missing = new ArrayList<>();
    if (phonetic == null || phonetic.isBlank()) {
      missing.add("phonetic");
    }
    if (frMeaningCount <= 0) {
      missing.add("fr-meaning");
    }
    if (enMeaningCount <= 0) {
      missing.add("en-meaning");
    }
    return missing;
  }

  private static Integer nullableInt(ResultSet rs, String column) throws SQLException {
    Object raw = rs.getObject(column);
    return raw instanceof Number ? ((Number) raw).intValue() : null;
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> summaryMap(Map<String, Object> summary, String key) {
    Object value = summary != null ? summary.get(key) : null;
    return value instanceof Map ? (Map<String, Object>) value : Map.of();
  }

  private static Map<String, Object> buildSummary(List<Entry> words, List<Entry> verbs) {
    Map<String, Object> summary = new LinkedHashMap<>();
    summary.put("words", summarizeEntries(words));
    summary.put("verbs", summarizeEntries(verbs));
    summary.put("totalIncompleteEntries", words.size() + verbs.size());
    return summary;
  }

  private static Map<String, Object> summarizeEntries(List<Entry> items) {
    int missingPhonetic = 0;
    int missingFr = 0;
    int missingEn = 0;
    int missingMultiple = 0;

    for (Entry item : items) {
      boolean hasMissingPhonetic = item.missingFields.contains("phonetic");
      boolean hasMissingFr = item.missingFields.contains("fr-meaning");
      boolean hasMissingEn = item.missingFields.contains("en-meaning");

      if (hasMissingPhonetic) {
        missingPhonetic++;
      }
      if (hasMissingFr) {
        missingFr++;
      }
      if (hasMissingEn) {
        missingEn++;
      }
      if (item.missingFields.size() > 1) {
        missingMultiple++;
      }
    }

    Map<String, Object> summary = new LinkedHashMap<>();
    summary.put("total", items.size());
    summary.put("missingPhonetic", missingPhonetic);
    summary.put("missingFrenchMeaning", missingFr);
    summary.put("missingEnglishMeaning", missingEn);
    summary.put("missingMultipleFields", missingMultiple);
    return summary;
  }

  public static final class Report {
    public String generatedAt;
    public String source;
    public Map<String, Object> summary;
    public List<Entry> words;
    public List<Entry> verbs;
  }

  public static final class Entry {
    public String lexicalType;
    public int id;
    public String label;
    public String slug;
    public String root;
    public String suffix;
    public String phonetic;
    public Integer nominalClassId;
    public String nominalClassName;
    public String singular;
    public String plural;
    public int frMeaningCount;
    public int enMeaningCount;
    public String frMeanings;
    public String enMeanings;
    public List<String> missingFields;
  }
}
