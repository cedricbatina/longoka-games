package com.longoka.games.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.longoka.games.meta.PackMeaningMeta;
import com.longoka.games.lexikongo.LexMeaning;
import com.longoka.games.lexikongo.LexVerb;
import com.longoka.games.lexikongo.LexWord;
import com.longoka.games.lexikongo.LexikongoVerbRepository;
import com.longoka.games.lexikongo.LexikongoWordRepository;
import com.longoka.games.puzzles.anagram.MorphoAnagramValidator;
import com.longoka.games.puzzles.anagram.json.MorphoAnagramJsonModels;
import com.longoka.games.puzzles.arrowword.ArrowwordFromCrosswordConverter;
import com.longoka.games.puzzles.arrowword.json.ArrowwordJsonModels;
import com.longoka.games.puzzles.crossword.CrosswordGridQuality;
import com.longoka.games.puzzles.crossword.json.CrosswordJsonModels;
import com.longoka.games.puzzles.domino.MorphoDominoValidator;
import com.longoka.games.puzzles.domino.json.MorphoDominoJsonModels;
import com.longoka.games.puzzles.memory.MemoryMatchValidator;
import com.longoka.games.puzzles.memory.json.MemoryMatchJsonModels;
import com.longoka.games.puzzles.scrabble.ScrabbleLikeValidator;
import com.longoka.games.puzzles.scrabble.json.ScrabbleLikeJsonModels;
import com.longoka.games.puzzles.wordsearch.WordPlacement;
import com.longoka.games.puzzles.wordsearch.WordSearchGenerator;
import com.longoka.games.puzzles.wordsearch.WordSearchPuzzle;
import com.longoka.games.puzzles.wordsearch.WordToFind;
import com.longoka.games.puzzles.wordsearch.json.SemanticTagger;
import com.longoka.games.puzzles.wordsearch.json.WordSearchJsonModels;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.time.LocalDate;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Generates puzzle packs directly from DB for Kikongo and Lingala (sortie sous target/packs/).
 *
 * Profiles supported:
 * - verbs only
 * - nouns singular only
 * - nouns plural only
 * - nouns singular-or-plural (never both forms for the same DB word in one grid)
 * - verbs + nouns with the three noun policies above
 */
public final class BiweeklyPuzzleBatchTool {

  private static final int DEFAULT_PUZZLE_COUNT = 48;
  private static final int DEFAULT_ROWS = 12;
  private static final int DEFAULT_COLS = 12;
  private static final int DEFAULT_MAX_ENTRIES = 12;
  private static final int DENSE_GENERATION_ATTEMPTS = 8;
  private static final int EXTRA_WORD_BUFFER = 8;

  /** Tentatives par puzzle arrowword (grille dérivée d’un crossword) si le batch interne renvoie une grille vide. */
  private static final int ARROWWORD_SLOT_MAX_TRIES = 12;
  private static final int DEFAULT_MAX_NOUN_RADICALS = 8;
  private static final int DEFAULT_MAX_VERB_RADICALS = 8;
  private static final String DEFAULT_MEANING_LANG = "fr";
  private static final String DEFAULT_PUZZLE_TYPE = "both";
  private static final String DEFAULT_LANGUAGE_MODE = "both";
  private static final String DEFAULT_DIFFICULTY = "auto";
  private static final String DEFAULT_EDITION_TIER = "mulongoki";
  private static final String DEFAULT_CADENCE = "weekly";

  private enum LanguageProfile {
    KG("kg", "lexikongo"),
    LN("ln", "lexilingala");

    final String code;
    final String source;

    LanguageProfile(String code, String source) {
      this.code = code;
      this.source = source;
    }
  }

  private enum NumberMode {
    SINGULAR_ONLY,
    PLURAL_ONLY,
    SINGULAR_OR_PLURAL
  }

  private enum PuzzleTypeMode {
    BOTH,
    WORDSEARCH_ONLY,
    CROSSWORD_ONLY,
    ARROWWORD_ONLY,
    DOMINO_ONLY,
    MEMORY_ONLY,
    SCRABBLE_ONLY,
    ANAGRAM_ONLY
  }

  private enum EditionTier {
    STANDARD,
    PREMIUM
  }

  private enum ProfileSetMode {
    ALL,
    BASE_ONLY
  }

  private enum MorphologyProfileMode {
    GENERAL,
    NOMINAL_CLASS,
    RADICAL_NOUNS,
    RADICAL_VERBS
  }

  private static final class CombinationProfile {
    final String id;
    final boolean includeNouns;
    final boolean includeVerbs;
    final NumberMode nounNumberMode;
    final String lexicalProfile;
    final MorphologyProfileMode morphologyProfile;
    final Integer nominalClassId;
    final List<Integer> nominalClassIds;
    final String nominalClassName;
    final String radical;

    CombinationProfile(
        String id,
        boolean includeNouns,
        boolean includeVerbs,
        NumberMode nounNumberMode,
        String lexicalProfile,
        MorphologyProfileMode morphologyProfile,
        Integer nominalClassId,
        List<Integer> nominalClassIds,
        String nominalClassName,
        String radical) {
      this.id = id;
      this.includeNouns = includeNouns;
      this.includeVerbs = includeVerbs;
      this.nounNumberMode = nounNumberMode;
      this.lexicalProfile = lexicalProfile;
      this.morphologyProfile = morphologyProfile;
      LinkedHashSet<Integer> classIds = new LinkedHashSet<>();
      if (nominalClassIds != null) {
        for (Integer classId : nominalClassIds) {
          if (classId != null && classId > 0) {
            classIds.add(classId);
          }
        }
      }
      if (classIds.isEmpty() && nominalClassId != null && nominalClassId > 0) {
        classIds.add(nominalClassId);
      }
      this.nominalClassIds = List.copyOf(classIds);
      this.nominalClassId = !this.nominalClassIds.isEmpty() ? this.nominalClassIds.get(0) : nominalClassId;
      this.nominalClassName = nominalClassName;
      this.radical = radical;
    }

    static CombinationProfile general(String id, boolean includeNouns, boolean includeVerbs, NumberMode nounNumberMode) {
      String lexicalProfile = includeNouns && includeVerbs
          ? "mixed"
          : (includeVerbs ? "verbs" : "nouns");
      return new CombinationProfile(
          id,
          includeNouns,
          includeVerbs,
          nounNumberMode,
          lexicalProfile,
          MorphologyProfileMode.GENERAL,
          null,
              null,
          null,
          null);
    }

    static CombinationProfile nominalClass(int classId, String className, NumberMode nounNumberMode) {
      String suffix = nounNumberMode == NumberMode.PLURAL_ONLY ? "plural" : "singular";
      return new CombinationProfile(
          "class-" + classId + "-" + suffix,
          true,
          false,
          nounNumberMode,
          "nouns",
          MorphologyProfileMode.NOMINAL_CLASS,
          classId,
              List.of(classId),
          className,
          null);
    }

            static CombinationProfile nominalClassGroup(String groupToken, List<Integer> classIds, String className, NumberMode nounNumberMode) {
          String suffix = nounNumberMode == NumberMode.PLURAL_ONLY ? "plural" : "singular";
          Integer primaryClassId = (classIds != null && !classIds.isEmpty()) ? classIds.get(0) : null;
          return new CombinationProfile(
              "class-" + groupToken + "-" + suffix,
              true,
              false,
              nounNumberMode,
              "nouns",
              MorphologyProfileMode.NOMINAL_CLASS,
              primaryClassId,
              classIds,
              className,
              null);
            }

    static CombinationProfile radicalNouns(String radical, NumberMode nounNumberMode) {
      String suffix = nounNumberMode == NumberMode.PLURAL_ONLY ? "plural" : "singular";
      String radicalToken = slugToken(radical);
      return new CombinationProfile(
          "radical-" + radicalToken + "-nouns-" + suffix,
          true,
          false,
          nounNumberMode,
          "nouns",
          MorphologyProfileMode.RADICAL_NOUNS,
          null,
              null,
          null,
          normalizeRadicalLabel(radical));
    }

    static CombinationProfile radicalVerbs(String radical) {
      String radicalToken = slugToken(radical);
      return new CombinationProfile(
          "radical-" + radicalToken + "-verbs",
          false,
          true,
          NumberMode.SINGULAR_OR_PLURAL,
          "verbs",
          MorphologyProfileMode.RADICAL_VERBS,
          null,
          null,
          null,
          normalizeRadicalLabel(radical));
    }
  }

  private static final class NominalClassAvailability {
    final int classId;
    final String className;
    final int singularCount;
    final int pluralCount;

    NominalClassAvailability(int classId, String className, int singularCount, int pluralCount) {
      this.classId = classId;
      this.className = className;
      this.singularCount = singularCount;
      this.pluralCount = pluralCount;
    }
  }

  /** Anciens identifiants « -family- » → profils groupés nommés d’après les classes (mu-ba + mu-mi, etc.). */
  private static final Map<String, String> LEGACY_PROFILE_ID_ALIASES = Map.ofEntries(
      Map.entry("class-mu-family-singular", "class-mu-ba-mu-mi-singular"),
      Map.entry("class-mu-family-plural", "class-mu-ba-mu-mi-plural"),
      Map.entry("class-bu-ku-family-singular", "class-bu-ma-ku-ma-singular"),
      Map.entry("class-bu-ku-family-plural", "class-bu-ma-ku-ma-plural"),
      Map.entry("class-lu-family-singular", "class-lu-tu-lu-zi-lu-ma-singular"),
      Map.entry("class-lu-family-plural", "class-lu-tu-lu-zi-lu-ma-plural"),
      Map.entry("class-ki-fi-family-singular", "class-ki-bi-fi-bi-singular"),
      Map.entry("class-ki-fi-family-plural", "class-ki-bi-fi-bi-plural"));

  private static final List<CombinationProfile> BASE_COMBINATIONS = List.of(
      CombinationProfile.general("verbs-only", false, true, NumberMode.SINGULAR_OR_PLURAL),
      CombinationProfile.general("nouns-singular", true, false, NumberMode.SINGULAR_ONLY),
      CombinationProfile.general("nouns-plural", true, false, NumberMode.PLURAL_ONLY),
      CombinationProfile.general("mixed-verbs-nouns-singular", true, true, NumberMode.SINGULAR_ONLY),
      CombinationProfile.general("mixed-verbs-nouns-plural", true, true, NumberMode.PLURAL_ONLY));

  private BiweeklyPuzzleBatchTool() {
  }

  public static void main(String[] args) throws Exception {
    int puzzleCount = parseIntArg(args, "--count", DEFAULT_PUZZLE_COUNT);
    int rows = parseIntArg(args, "--rows", DEFAULT_ROWS);
    int cols = parseIntArg(args, "--cols", DEFAULT_COLS);
    int maxEntries = parseIntArg(args, "--maxEntries", DEFAULT_MAX_ENTRIES);
    int nounRadicalLimit = parseIntArg(args, "--nounRadicalLimit", DEFAULT_MAX_NOUN_RADICALS);
    int verbRadicalLimit = parseIntArg(args, "--verbRadicalLimit", DEFAULT_MAX_VERB_RADICALS);
    int minMorphologyEntries = parseIntArg(args, "--minMorphologyEntries", Math.max(6, Math.min(maxEntries, 10)));
    String meaningLang = parseStringArg(args, "--meaningLang", DEFAULT_MEANING_LANG);
    String languageMode = parseStringArg(
        args,
        "--language",
        parseStringArg(args, "--languages", DEFAULT_LANGUAGE_MODE));
    String outputLabel = parseStringArg(args, "--label", LocalDate.now().toString());
    PuzzleTypeMode puzzleTypeMode = parsePuzzleTypeMode(parseStringArg(args, "--type", DEFAULT_PUZZLE_TYPE));
    String requestedDifficulty = parseStringArg(args, "--difficulty", DEFAULT_DIFFICULTY);
    EditionTier editionTier = parseEditionTier(parseStringArg(args, "--tier", DEFAULT_EDITION_TIER));
    ProfileSetMode profileSetMode = parseProfileSetMode(parseStringArg(args, "--profileSet", "all"));
    Set<String> requestedProfileIds = parseProfileIdsArg(args, "--profiles");
    String cadence = parseStringArg(args, "--cadence", DEFAULT_CADENCE).trim().toLowerCase(Locale.ROOT);
    if (cadence.isEmpty()) {
      cadence = DEFAULT_CADENCE;
    }
    if ("biweekly".equals(cadence)) {
      System.out.println("Cadence 'biweekly' fusionnée avec 'weekly' (meta.exportCadence = weekly).");
      cadence = "weekly";
    } else if (!"weekly".equals(cadence)) {
      System.err.println("Cadence inconnue '" + cadence + "', utilisation de weekly.");
      cadence = "weekly";
    }
    System.setProperty("longoka.cadence", cadence);
    List<LanguageProfile> selectedLanguages = parseLanguageProfiles(languageMode);

    // Sortie unique : target/packs/<label>/ (plus de séparation weekly/ biweekly sur le disque).
    Path outDir = Path.of("target", "packs", outputLabel);
    Files.createDirectories(outDir);

    Random random = new Random();
    ObjectMapper mapper = new ObjectMapper();
    mapper.enable(SerializationFeature.INDENT_OUTPUT);

    System.out.println("== Puzzle Batch (target/packs) cadence=" + cadence + " ==");
    System.out.println("label=" + outputLabel
        + ", bookSize=" + puzzleCount
        + ", type=" + puzzleTypeMode
        + ", meaningLang=" + meaningLang
        + ", languages=" + formatLanguageSelection(selectedLanguages)
        + ", tier=" + editionTierKey(editionTier)
        + ", difficulty=" + resolveDifficulty(requestedDifficulty, editionTier));
    System.out.println("nounRadicalLimit=" + nounRadicalLimit + ", verbRadicalLimit=" + verbRadicalLimit + ", minMorphologyEntries=" + minMorphologyEntries);

    for (LanguageProfile language : selectedLanguages) {
      try (Connection conn = openLexConnection(language.code)) {
        List<CombinationProfile> combinations = buildCombinationProfiles(
            conn,
            language,
            nounRadicalLimit,
            verbRadicalLimit,
            minMorphologyEntries,
            profileSetMode,
            requestedProfileIds);

        System.out.println("- profiles for " + language.code + ": " + combinations.size());

        if (combinations.isEmpty()) {
          System.out.println("- no eligible profiles for " + language.code + " with current filters");
          continue;
        }

        for (CombinationProfile combination : combinations) {
          String exportProfileToken = publicProfileToken(combination);
          System.out.println("- generating " + language.code + " / " + combination.id + " ...");

          if (shouldGenerateWordsearch(puzzleTypeMode)) {
            WordSearchJsonModels.PackV1 wordSearchPack = generateWordSearchPack(
                conn,
                language,
                combination,
                puzzleCount,
                rows,
                cols,
                maxEntries,
                meaningLang,
                editionTier,
                requestedDifficulty,
                random);
            if (wordSearchPack != null && wordSearchPack.puzzles != null && !wordSearchPack.puzzles.isEmpty()) {
              Path wsPath = outDir.resolve(language.code + "-" + exportProfileToken + "-wordsearch-pack.v1.json");
              mapper.writeValue(wsPath.toFile(), wordSearchPack);
            }
          }

          if (shouldGenerateCrossword(puzzleTypeMode)) {
            CrosswordJsonModels.PackV1 crosswordPack = generateCrosswordPack(
                conn,
                language,
                combination,
                puzzleCount,
                rows,
                cols,
                maxEntries,
                meaningLang,
                editionTier,
                requestedDifficulty,
                random);
            if (crosswordPack != null && crosswordPack.puzzles != null && !crosswordPack.puzzles.isEmpty()) {
              Path cwPath = outDir.resolve(language.code + "-" + exportProfileToken + "-crossword-pack.v1.json");
              mapper.writeValue(cwPath.toFile(), crosswordPack);
            }
          }

          if (shouldGenerateArrowword(puzzleTypeMode)) {
            ArrowwordJsonModels.PackV1 arrowwordPack = generateArrowwordPack(
                conn,
                language,
                combination,
                puzzleCount,
                rows,
                cols,
                maxEntries,
                meaningLang,
                editionTier,
                requestedDifficulty,
                random);
            if (arrowwordPack != null && arrowwordPack.puzzles != null && !arrowwordPack.puzzles.isEmpty()) {
              Path awPath = outDir.resolve(language.code + "-" + exportProfileToken + "-arrowword-pack.v1.json");
              mapper.writeValue(awPath.toFile(), arrowwordPack);
            }
          }

          if (shouldGenerateDomino(puzzleTypeMode) && supportsMorphoDomino(combination)) {
            MorphoDominoJsonModels.PackV1 dominoPack = generateMorphoDominoPack(
                conn,
                language,
                combination,
                puzzleCount,
                maxEntries,
                meaningLang,
                editionTier,
                requestedDifficulty,
                random);
            if (dominoPack != null && dominoPack.puzzles != null && !dominoPack.puzzles.isEmpty()) {
              Path dmPath = outDir.resolve(language.code + "-" + exportProfileToken + "-domino-pack.v1.json");
              mapper.writeValue(dmPath.toFile(), dominoPack);
            }
          }

          if (shouldGenerateMemory(puzzleTypeMode)) {
            MemoryMatchJsonModels.PackV1 memoryPack = generateMemoryMatchPack(
                conn,
                language,
                combination,
                puzzleCount,
                maxEntries,
                meaningLang,
                editionTier,
                requestedDifficulty,
                random);
            if (memoryPack != null && memoryPack.puzzles != null && !memoryPack.puzzles.isEmpty()) {
              Path mmPath = outDir.resolve(language.code + "-" + exportProfileToken + "-memory-pack.v1.json");
              mapper.writeValue(mmPath.toFile(), memoryPack);
            }
          }

          if (shouldGenerateScrabble(puzzleTypeMode)) {
            ScrabbleLikeJsonModels.PackV1 scrabblePack = generateScrabbleLikePack(
                conn,
                language,
                combination,
                puzzleCount,
                maxEntries,
                meaningLang,
                editionTier,
                requestedDifficulty,
                random);
            if (scrabblePack != null && scrabblePack.puzzles != null && !scrabblePack.puzzles.isEmpty()) {
              Path slPath = outDir.resolve(language.code + "-" + exportProfileToken + "-scrabble-pack.v1.json");
              mapper.writeValue(slPath.toFile(), scrabblePack);
            }
          }

          if (shouldGenerateAnagram(puzzleTypeMode)) {
            MorphoAnagramJsonModels.PackV1 anagramPack = generateMorphoAnagramPack(
                conn,
                language,
                combination,
                puzzleCount,
                maxEntries,
                meaningLang,
                editionTier,
                requestedDifficulty,
                random);
            if (anagramPack != null && anagramPack.puzzles != null && !anagramPack.puzzles.isEmpty()) {
              Path maPath = outDir.resolve(language.code + "-" + exportProfileToken + "-anagram-pack.v1.json");
              mapper.writeValue(maPath.toFile(), anagramPack);
            }
          }
        }
      }
    }

    System.out.println("Done. Output directory: " + outDir.toAbsolutePath());
  }

  private static List<CombinationProfile> buildCombinationProfiles(
      Connection conn,
      LanguageProfile language,
      int nounRadicalLimit,
      int verbRadicalLimit,
      int minMorphologyEntries,
      ProfileSetMode profileSetMode,
      Set<String> requestedProfileIds) throws Exception {

    if (profileSetMode == ProfileSetMode.BASE_ONLY) {
      return filterProfiles(new ArrayList<>(BASE_COMBINATIONS), requestedProfileIds);
    }

    List<CombinationProfile> profiles = new ArrayList<>(BASE_COMBINATIONS);
    profiles.addAll(discoverNominalClassProfiles(conn, language, minMorphologyEntries));
    profiles.addAll(discoverNounRadicalProfiles(conn, nounRadicalLimit, minMorphologyEntries));
    profiles.addAll(discoverVerbRadicalProfiles(conn, verbRadicalLimit, Math.max(4, minMorphologyEntries - 2)));
    return filterProfiles(profiles, requestedProfileIds);
  }

  private static ProfileSetMode parseProfileSetMode(String raw) {
    String value = raw != null ? raw.trim().toLowerCase(Locale.ROOT) : "";
    if (value.isEmpty() || "all".equals(value) || "full".equals(value)) {
      return ProfileSetMode.ALL;
    }
    if ("base".equals(value) || "base-only".equals(value) || "base_only".equals(value) || "baseonly".equals(value)) {
      return ProfileSetMode.BASE_ONLY;
    }
    // Defensive default: keep current behavior.
    return ProfileSetMode.ALL;
  }

  private static List<CombinationProfile> discoverNominalClassProfiles(Connection conn, LanguageProfile language, int minCount) throws Exception {
    String sql = """
        SELECT w.class_id,
               NULLIF(TRIM(nc.class_name), '') AS class_name,
               SUM(CASE WHEN NULLIF(TRIM(w.singular), '') IS NOT NULL THEN 1 ELSE 0 END) AS singular_count,
               SUM(CASE WHEN NULLIF(TRIM(w.plural), '') IS NOT NULL THEN 1 ELSE 0 END) AS plural_count
        FROM words w
        LEFT JOIN nominal_classes nc ON nc.class_id = w.class_id
        WHERE w.is_approved = 1
          AND w.class_id IS NOT NULL
        GROUP BY w.class_id, NULLIF(TRIM(nc.class_name), '')
        ORDER BY w.class_id ASC
        """;

    Map<Integer, NominalClassAvailability> availabilityById = new LinkedHashMap<>();
    try (PreparedStatement ps = conn.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {
      while (rs.next()) {
        int classId = rs.getInt("class_id");
        String className = rs.getString("class_name");
        int singularCount = rs.getInt("singular_count");
        int pluralCount = rs.getInt("plural_count");

        availabilityById.put(classId, new NominalClassAvailability(classId, className, singularCount, pluralCount));
      }
    }

    List<CombinationProfile> profiles = new ArrayList<>();
    if (language == LanguageProfile.KG) {
      addSingleNominalClassProfiles(profiles, availabilityById.get(1), minCount);
      addSingleNominalClassProfiles(profiles, availabilityById.get(4), minCount);
      NominalClassAvailability kiBiAvailability = availabilityById.get(5);
      NominalClassAvailability fiBiAvailability = availabilityById.get(14);
      if (kiBiAvailability != null && fiBiAvailability != null) {
        addGroupedNominalClassProfiles(profiles, availabilityById, "ki-bi + fi-bi", List.of(5, 14), minCount);
      } else {
        addSingleNominalClassProfiles(profiles, kiBiAvailability, minCount);
        addSingleNominalClassProfiles(profiles, fiBiAvailability, minCount);
      }
      addGroupedNominalClassProfiles(profiles, availabilityById, "mu-ba + mu-mi", List.of(2, 3), minCount);
      addGroupedNominalClassProfiles(profiles, availabilityById, "bu-ma + ku-ma", List.of(6, 10), minCount);
      addGroupedNominalClassProfiles(profiles, availabilityById, "lu-tu + lu-zi + lu-ma", List.of(7, 8, 9), minCount);
      return profiles;
    }

    for (NominalClassAvailability availability : availabilityById.values()) {
      addSingleNominalClassProfiles(profiles, availability, minCount);
    }
    return profiles;
  }

  private static void addSingleNominalClassProfiles(
      List<CombinationProfile> profiles,
      NominalClassAvailability availability,
      int minCount) {

    if (availability == null) {
      return;
    }
    if (availability.singularCount >= minCount) {
      profiles.add(CombinationProfile.nominalClass(availability.classId, availability.className, NumberMode.SINGULAR_ONLY));
    }
    if (availability.pluralCount >= minCount) {
      profiles.add(CombinationProfile.nominalClass(availability.classId, availability.className, NumberMode.PLURAL_ONLY));
    }
  }

  private static void addGroupedNominalClassProfiles(
      List<CombinationProfile> profiles,
      Map<Integer, NominalClassAvailability> availabilityById,
      String groupLabel,
      List<Integer> classIds,
      int minCount) {

    String groupToken = slugToken(groupLabel);
    int singularCount = 0;
    int pluralCount = 0;
    List<Integer> activeClassIds = new ArrayList<>();
    for (Integer classId : classIds) {
      NominalClassAvailability availability = availabilityById.get(classId);
      if (availability == null) {
        continue;
      }
      activeClassIds.add(classId);
      singularCount += availability.singularCount;
      pluralCount += availability.pluralCount;
    }
    if (activeClassIds.isEmpty()) {
      return;
    }
    if (singularCount >= minCount) {
      profiles.add(CombinationProfile.nominalClassGroup(groupToken, activeClassIds, groupLabel, NumberMode.SINGULAR_ONLY));
    }
    if (pluralCount >= minCount) {
      profiles.add(CombinationProfile.nominalClassGroup(groupToken, activeClassIds, groupLabel, NumberMode.PLURAL_ONLY));
    }
  }

  private static Set<String> normalizeRequestedProfileIds(Set<String> requestedProfileIds) {
    if (requestedProfileIds == null || requestedProfileIds.isEmpty()) {
      return requestedProfileIds;
    }
    Set<String> normalized = new LinkedHashSet<>();
    for (String rawId : requestedProfileIds) {
      if (rawId == null || rawId.isBlank()) {
        continue;
      }
      String trimmed = rawId.trim();
      normalized.add(LEGACY_PROFILE_ID_ALIASES.getOrDefault(trimmed, trimmed));
    }
    return normalized;
  }

  private static List<CombinationProfile> filterProfiles(List<CombinationProfile> profiles, Set<String> requestedProfileIds) {
    if (requestedProfileIds == null || requestedProfileIds.isEmpty()) {
      return profiles;
    }
    Set<String> normalizedIds = normalizeRequestedProfileIds(requestedProfileIds);
    List<CombinationProfile> filtered = new ArrayList<>();
    for (CombinationProfile profile : profiles) {
      if (normalizedIds.contains(profile.id)) {
        filtered.add(profile);
      }
    }
    return filtered;
  }

  private static List<CombinationProfile> discoverNounRadicalProfiles(Connection conn, int limit, int minCount) throws Exception {
    if (limit <= 0) {
      return List.of();
    }

    String sql = """
        SELECT UPPER(TRIM(w.root)) AS root_key,
               MIN(TRIM(w.root)) AS root_label,
               SUM(CASE WHEN NULLIF(TRIM(w.singular), '') IS NOT NULL THEN 1 ELSE 0 END) AS singular_count,
               SUM(CASE WHEN NULLIF(TRIM(w.plural), '') IS NOT NULL THEN 1 ELSE 0 END) AS plural_count
        FROM words w
        WHERE w.is_approved = 1
          AND NULLIF(TRIM(w.root), '') IS NOT NULL
        GROUP BY UPPER(TRIM(w.root))
        ORDER BY GREATEST(
          SUM(CASE WHEN NULLIF(TRIM(w.singular), '') IS NOT NULL THEN 1 ELSE 0 END),
          SUM(CASE WHEN NULLIF(TRIM(w.plural), '') IS NOT NULL THEN 1 ELSE 0 END)
        ) DESC,
        root_key ASC
        LIMIT ?
        """;

    List<CombinationProfile> profiles = new ArrayList<>();
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, limit);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          String root = normalizeRadicalLabel(rs.getString("root_label"));
          int singularCount = rs.getInt("singular_count");
          int pluralCount = rs.getInt("plural_count");

          if (root == null) {
            continue;
          }
          if (singularCount >= minCount) {
            profiles.add(CombinationProfile.radicalNouns(root, NumberMode.SINGULAR_ONLY));
          }
          if (pluralCount >= minCount) {
            profiles.add(CombinationProfile.radicalNouns(root, NumberMode.PLURAL_ONLY));
          }
        }
      }
    }
    return profiles;
  }

  private static List<CombinationProfile> discoverVerbRadicalProfiles(Connection conn, int limit, int minCount) throws Exception {
    if (limit <= 0) {
      return List.of();
    }

    String sql = """
        SELECT UPPER(TRIM(v.root)) AS root_key,
               MIN(TRIM(v.root)) AS root_label,
               COUNT(*) AS total_count
        FROM verbs v
        WHERE v.is_approved = 1
          AND v.active_verb = 1
          AND NULLIF(TRIM(v.root), '') IS NOT NULL
        GROUP BY UPPER(TRIM(v.root))
        HAVING COUNT(*) >= ?
        ORDER BY total_count DESC, root_key ASC
        LIMIT ?
        """;

    List<CombinationProfile> profiles = new ArrayList<>();
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, minCount);
      ps.setInt(2, limit);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          String root = normalizeRadicalLabel(rs.getString("root_label"));
          if (root != null) {
            profiles.add(CombinationProfile.radicalVerbs(root));
          }
        }
      }
    }
    return profiles;
  }

  private static WordSearchJsonModels.PackV1 generateWordSearchPack(
      Connection conn,
      LanguageProfile language,
      CombinationProfile combination,
      int puzzleCount,
      int rows,
      int cols,
      int maxEntries,
      String meaningLang,
      EditionTier editionTier,
      String requestedDifficulty,
      Random random) throws Exception {

    WordSearchGenerator generator = new WordSearchGenerator(random);
    List<WordSearchJsonModels.PuzzleV1> puzzles = new ArrayList<>();
    String packDifficulty = resolveDifficulty(requestedDifficulty, editionTier);

    for (int i = 1; i <= puzzleCount; i++) {
      List<WordToFind> selectedWords = selectWordsFromDb(conn, language, combination, maxEntries, meaningLang, random);
      if (selectedWords.isEmpty()) {
        continue;
      }

      WordSearchPuzzle puzzle = generator.generatePuzzle(
          language.code,
          titleFor(language, combination, "wordsearch", editionTier, meaningLang),
          themeFor(combination),
          rows,
          cols,
          selectedWords);

      puzzles.add(toWordSearchJsonPuzzle(
          puzzle,
          language,
          combination,
          editionTier,
          packDifficulty,
          meaningLang,
          i));
    }

    WordSearchJsonModels.PackV1 pack = new WordSearchJsonModels.PackV1();
    pack.language = language.code;
    pack.packId = buildPackId(language, combination, "wordsearch", editionTier);
    pack.title = titleFor(language, combination, "wordsearch", editionTier, meaningLang);
    pack.description = descriptionFor(language, combination, "wordsearch", editionTier, packDifficulty, meaningLang, puzzles.size());
    pack.meaningLanguage = meaningLang;
    pack.meta = buildPackMeta(language, combination, "wordsearch", editionTier, packDifficulty, meaningLang, puzzles.size());
    pack.meta.put("rows", rows);
    pack.meta.put("cols", cols);
    pack.meta.put("entryTarget", maxEntries);
    pack.meta.put("targetEntries", maxEntries);
    finalizePackBookMeta(
        pack.meta, pack.title, pack.description, pack.packId, puzzles.size(), editionTier, meaningLang);
    pack.puzzles = puzzles;
    if (puzzles.isEmpty()) {
      return null;
    }
    return pack;
  }

  private static CrosswordJsonModels.PackV1 generateCrosswordPack(
      Connection conn,
      LanguageProfile language,
      CombinationProfile combination,
      int puzzleCount,
      int rows,
      int cols,
      int maxEntries,
      String meaningLang,
      EditionTier editionTier,
      String requestedDifficulty,
      Random random) throws Exception {

    List<CrosswordJsonModels.PuzzleV1> puzzles = new ArrayList<>();
    String packDifficulty = resolveDifficulty(requestedDifficulty, editionTier);
    for (int i = 1; i <= puzzleCount; i++) {
      CrosswordJsonModels.PuzzleV1 puzzle = buildBestGeneratedCrosswordPuzzle(
          conn,
          language,
          combination,
          rows,
          cols,
          maxEntries,
          meaningLang,
          editionTier,
          packDifficulty,
          random,
          i);
      if (puzzle == null) {
        continue;
      }
      puzzles.add(puzzle);
    }

    CrosswordJsonModels.PackV1 pack = new CrosswordJsonModels.PackV1();
    pack.language = language.code;
    pack.packId = buildPackId(language, combination, "crossword", editionTier);
    pack.title = titleFor(language, combination, "crossword", editionTier, meaningLang);
    pack.description = descriptionFor(language, combination, "crossword", editionTier, packDifficulty, meaningLang, puzzles.size());
    pack.meaningLanguage = meaningLang;
    pack.meta = buildPackMeta(language, combination, "crossword", editionTier, packDifficulty, meaningLang, puzzles.size());
    pack.meta.put("rows", rows);
    pack.meta.put("cols", cols);
    pack.meta.put("entryTarget", maxEntries);
    pack.meta.put("targetEntries", maxEntries);
    finalizePackBookMeta(
        pack.meta, pack.title, pack.description, pack.packId, puzzles.size(), editionTier, meaningLang);
    pack.puzzles = puzzles;
    if (puzzles.isEmpty()) {
      return null;
    }
    return pack;
  }

  private static ArrowwordJsonModels.PackV1 generateArrowwordPack(
      Connection conn,
      LanguageProfile language,
      CombinationProfile combination,
      int puzzleCount,
      int rows,
      int cols,
      int maxEntries,
      String meaningLang,
      EditionTier editionTier,
      String requestedDifficulty,
      Random random) throws Exception {

    if (rows < 6 || cols < 6) {
      throw new IllegalArgumentException("Arrowword batch requires at least 6x6.");
    }

    List<ArrowwordJsonModels.PuzzleV1> puzzles = new ArrayList<>();
    String packDifficulty = resolveDifficulty(requestedDifficulty, editionTier);
    int seedRows = rows - 1;
    int seedCols = cols - 1;
    for (int i = 1; i <= puzzleCount; i++) {
      CrosswordJsonModels.PuzzleV1 crossword = null;
      for (int slotTry = 0; slotTry < ARROWWORD_SLOT_MAX_TRIES; slotTry++) {
        CrosswordJsonModels.PuzzleV1 candidate = buildBestGeneratedCrosswordPuzzle(
            conn,
            language,
            combination,
            seedRows,
            seedCols,
            maxEntries,
            meaningLang,
            editionTier,
            packDifficulty,
            random,
            i);
        if (candidate != null && countCrosswordEntries(candidate) > 0) {
          crossword = candidate;
          break;
        }
      }
      if (crossword == null) {
        continue;
      }

      ArrowwordJsonModels.PuzzleV1 arrowword = ArrowwordFromCrosswordConverter.convertPuzzle(crossword, meaningLang);
      puzzles.add(arrowword);
    }

    ArrowwordJsonModels.PackV1 pack = new ArrowwordJsonModels.PackV1();
    pack.language = language.code;
    pack.packId = buildPackId(language, combination, "arrowword", editionTier);
    pack.title = titleFor(language, combination, "arrowword", editionTier, meaningLang);
    pack.description = descriptionFor(language, combination, "arrowword", editionTier, packDifficulty, meaningLang, puzzles.size());
    pack.meaningLanguage = meaningLang;
    pack.meta = buildPackMeta(language, combination, "arrowword", editionTier, packDifficulty, meaningLang, puzzles.size());
    pack.meta.put("crosswordSeedRows", rows - 1);
    pack.meta.put("crosswordSeedCols", cols - 1);
    pack.meta.put("entryTarget", maxEntries);
    pack.meta.put("targetEntries", maxEntries);
    finalizePackBookMeta(
        pack.meta, pack.title, pack.description, pack.packId, puzzles.size(), editionTier, meaningLang);
    pack.puzzles = puzzles;
    if (puzzles.isEmpty()) {
      System.err.println(
          "Aucun pack mots fléchés généré pour "
              + language.code
              + " / "
              + combination.id
              + " (grille source "
              + seedRows
              + "x"
              + seedCols
              + "). Pack ignoré pour ce cycle.");
      return null;
    }
    return pack;
  }

  private static MorphoDominoJsonModels.PackV1 generateMorphoDominoPack(
      Connection conn,
      LanguageProfile language,
      CombinationProfile combination,
      int puzzleCount,
      int targetTileCount,
      String meaningLang,
      EditionTier editionTier,
      String requestedDifficulty,
      Random random) throws Exception {

    int safeTileTarget = normalizeDominoTileTarget(targetTileCount);
    List<MorphoDominoJsonModels.PuzzleV1> puzzles = new ArrayList<>();
    String packDifficulty = resolveDifficulty(requestedDifficulty, editionTier);

    for (int i = 1; i <= puzzleCount; i++) {
      MorphoDominoJsonModels.PuzzleV1 puzzle = buildMorphoDominoPuzzle(
          conn,
          language,
          combination,
          safeTileTarget,
          meaningLang,
          editionTier,
          packDifficulty,
          random,
          i);
      if (puzzle != null) {
        puzzles.add(puzzle);
      }
    }

    if (puzzles.isEmpty()) {
      return null;
    }

    MorphoDominoJsonModels.PackV1 pack = new MorphoDominoJsonModels.PackV1();
    pack.language = language.code;
    pack.packId = buildPackId(language, combination, "domino", editionTier);
    pack.title = titleFor(language, combination, "domino", editionTier, meaningLang);
    pack.description = descriptionFor(language, combination, "domino", editionTier, packDifficulty, meaningLang, puzzles.size());
    pack.meaningLanguage = meaningLang;
    pack.meta = buildPackMeta(language, combination, "domino", editionTier, packDifficulty, meaningLang, puzzles.size());
    pack.meta.put("relationType", dominoRelationType(combination));
    pack.meta.put("layout", "chain");
    pack.meta.put("tileTarget", safeTileTarget);
    pack.meta.put("targetItems", safeTileTarget);
    finalizePackBookMeta(
        pack.meta, pack.title, pack.description, pack.packId, puzzles.size(), editionTier, meaningLang);
    pack.puzzles = puzzles;
    MorphoDominoValidator.validatePack(pack);
    return pack;
  }

  private static MemoryMatchJsonModels.PackV1 generateMemoryMatchPack(
      Connection conn,
      LanguageProfile language,
      CombinationProfile combination,
      int puzzleCount,
      int targetPairCount,
      String meaningLanguage,
      EditionTier editionTier,
      String requestedDifficulty,
      Random random) throws Exception {

    int safePairTarget = normalizeMemoryPairTarget(targetPairCount);
    List<MemoryMatchJsonModels.PuzzleV1> puzzles = new ArrayList<>();
    String packDifficulty = resolveDifficulty(requestedDifficulty, editionTier);

    for (int i = 1; i <= puzzleCount; i++) {
      MemoryMatchJsonModels.PuzzleV1 puzzle = buildMemoryMatchPuzzle(
          conn,
          language,
          combination,
          safePairTarget,
          meaningLanguage,
          editionTier,
          packDifficulty,
          random,
          i);
      if (puzzle != null) {
        puzzles.add(puzzle);
      }
    }

    if (puzzles.isEmpty()) {
      return null;
    }

    MemoryMatchJsonModels.PackV1 pack = new MemoryMatchJsonModels.PackV1();
    pack.language = language.code;
    pack.packId = buildPackId(language, combination, "memory", editionTier);
    pack.title = titleFor(language, combination, "memory", editionTier, meaningLanguage);
    pack.description = descriptionFor(language, combination, "memory", editionTier, packDifficulty, meaningLanguage, puzzles.size());
    pack.meaningLanguage = meaningLanguage;
    pack.meta = buildPackMeta(language, combination, "memory", editionTier, packDifficulty, meaningLanguage, puzzles.size());
    pack.meta.put("relationType", memoryRelationType(combination));
    pack.meta.put("layout", "grid");
    pack.meta.put("pairTarget", safePairTarget);
    pack.meta.put("targetItems", safePairTarget);
    finalizePackBookMeta(
        pack.meta,
        pack.title,
        pack.description,
        pack.packId,
        puzzles.size(),
        editionTier,
        meaningLanguage);
    pack.puzzles = puzzles;
    MemoryMatchValidator.validatePack(pack);
    return pack;
  }

  private static ScrabbleLikeJsonModels.PackV1 generateScrabbleLikePack(
      Connection conn,
      LanguageProfile language,
      CombinationProfile combination,
      int puzzleCount,
      int targetChallengeCount,
      String meaningLanguage,
      EditionTier editionTier,
      String requestedDifficulty,
      Random random) throws Exception {

    int safeChallengeTarget = normalizeScrabbleChallengeTarget(targetChallengeCount);
    List<ScrabbleLikeJsonModels.PuzzleV1> puzzles = new ArrayList<>();
    String packDifficulty = resolveDifficulty(requestedDifficulty, editionTier);

    for (int i = 1; i <= puzzleCount; i++) {
      ScrabbleLikeJsonModels.PuzzleV1 puzzle = buildScrabbleLikePuzzle(
          conn,
          language,
          combination,
          safeChallengeTarget,
          meaningLanguage,
          editionTier,
          packDifficulty,
          random,
          i);
      if (puzzle != null) {
        puzzles.add(puzzle);
      }
    }

    if (puzzles.isEmpty()) {
      return null;
    }

    ScrabbleLikeJsonModels.PackV1 pack = new ScrabbleLikeJsonModels.PackV1();
    pack.language = language.code;
    pack.packId = buildPackId(language, combination, "scrabble", editionTier);
    pack.title = titleFor(language, combination, "scrabble", editionTier, meaningLanguage);
    pack.description = descriptionFor(language, combination, "scrabble", editionTier, packDifficulty, meaningLanguage, puzzles.size());
    pack.meaningLanguage = meaningLanguage;
    pack.meta = buildPackMeta(language, combination, "scrabble", editionTier, packDifficulty, meaningLanguage, puzzles.size());
    pack.meta.put("relationType", scrabbleRelationType(combination));
    pack.meta.put("layout", "stacked-racks");
    pack.meta.put("challengeTarget", safeChallengeTarget);
    pack.meta.put("targetItems", safeChallengeTarget);
    finalizePackBookMeta(
        pack.meta,
        pack.title,
        pack.description,
        pack.packId,
        puzzles.size(),
        editionTier,
        meaningLanguage);
    pack.puzzles = puzzles;
    ScrabbleLikeValidator.validatePack(pack);
    return pack;
  }

  private static MorphoAnagramJsonModels.PackV1 generateMorphoAnagramPack(
      Connection conn,
      LanguageProfile language,
      CombinationProfile combination,
      int puzzleCount,
      int targetChallengeCount,
      String meaningLanguage,
      EditionTier editionTier,
      String requestedDifficulty,
      Random random) throws Exception {

    int safeChallengeTarget = normalizeAnagramChallengeTarget(targetChallengeCount);
    List<MorphoAnagramJsonModels.PuzzleV1> puzzles = new ArrayList<>();
    String packDifficulty = resolveDifficulty(requestedDifficulty, editionTier);

    for (int i = 1; i <= puzzleCount; i++) {
      MorphoAnagramJsonModels.PuzzleV1 puzzle = buildMorphoAnagramPuzzle(
          conn,
          language,
          combination,
          safeChallengeTarget,
          meaningLanguage,
          editionTier,
          packDifficulty,
          random,
          i);
      if (puzzle != null) {
        puzzles.add(puzzle);
      }
    }

    if (puzzles.isEmpty()) {
      return null;
    }

    MorphoAnagramJsonModels.PackV1 pack = new MorphoAnagramJsonModels.PackV1();
    pack.language = language.code;
    pack.packId = buildPackId(language, combination, "anagram", editionTier);
    pack.title = titleFor(language, combination, "anagram", editionTier, meaningLanguage);
    pack.description = descriptionFor(language, combination, "anagram", editionTier, packDifficulty, meaningLanguage, puzzles.size());
    pack.meaningLanguage = meaningLanguage;
    pack.meta = buildPackMeta(language, combination, "anagram", editionTier, packDifficulty, meaningLanguage, puzzles.size());
    pack.meta.put("relationType", anagramRelationType(combination));
    pack.meta.put("layout", "stacked-segments");
    pack.meta.put("challengeTarget", safeChallengeTarget);
    pack.meta.put("targetItems", safeChallengeTarget);
    finalizePackBookMeta(
        pack.meta,
        pack.title,
        pack.description,
        pack.packId,
        puzzles.size(),
        editionTier,
        meaningLanguage);
    pack.puzzles = puzzles;
    MorphoAnagramValidator.validatePack(pack);
    return pack;
  }

  private static List<WordToFind> selectWordsFromDb(
      Connection conn,
      LanguageProfile language,
      CombinationProfile combination,
      int maxEntries,
      String meaningLang,
      Random random) throws Exception {

    int nounTarget = combination.includeNouns && combination.includeVerbs ? (maxEntries / 2) : maxEntries;
    int verbTarget = combination.includeNouns && combination.includeVerbs ? (maxEntries - nounTarget) : maxEntries;

    List<WordToFind> selected = new ArrayList<>();
    Set<String> seenGridForms = new LinkedHashSet<>();

    if (combination.includeNouns) {
      List<WordToFind> nouns = selectNouns(
          conn,
          combination,
          nounTarget,
          combination.nounNumberMode,
          meaningLang,
          random,
          seenGridForms);
      selected.addAll(nouns);
    }

    if (combination.includeVerbs) {
      List<WordToFind> verbs = selectVerbs(
          conn,
          combination,
          verbTarget,
          meaningLang,
          seenGridForms);
      selected.addAll(verbs);
    }

    // top-up if mixed split cannot be satisfied from one side
    if (selected.size() < maxEntries && combination.includeNouns) {
      selected.addAll(selectNouns(
          conn,
          combination,
          maxEntries - selected.size(),
          combination.nounNumberMode,
          meaningLang,
          random,
          seenGridForms));
    }
    if (selected.size() < maxEntries && combination.includeVerbs) {
      selected.addAll(selectVerbs(
          conn,
          combination,
          maxEntries - selected.size(),
          meaningLang,
          seenGridForms));
    }

    return selected;
  }

  private static List<WordToFind> selectNouns(
      Connection conn,
      CombinationProfile combination,
      int target,
      NumberMode numberMode,
      String meaningLang,
      Random random,
      Set<String> seenGridForms) throws Exception {

    if (target <= 0) {
      return List.of();
    }

    LexikongoWordRepository repo = new LexikongoWordRepository(conn);
    List<LexWord> lexWords;
    if (combination.morphologyProfile == MorphologyProfileMode.NOMINAL_CLASS && !combination.nominalClassIds.isEmpty()) {
      int sampleSize = target * Math.max(8, combination.nominalClassIds.size() * 6);
      lexWords = combination.nominalClassIds.size() == 1
          ? repo.findRandomWordsByClassId(combination.nominalClassIds.get(0), sampleSize, meaningLang)
          : repo.findRandomWordsByClassIds(combination.nominalClassIds, sampleSize, meaningLang);
    } else if (combination.morphologyProfile == MorphologyProfileMode.RADICAL_NOUNS && combination.radical != null) {
      lexWords = repo.findRandomWords(target * 16, meaningLang);
    } else {
      lexWords = repo.findRandomWords(target * 6, meaningLang);
    }

    List<WordToFind> out = new ArrayList<>();
    for (LexWord w : lexWords) {
      if (out.size() >= target) {
        break;
      }

      String base = selectNounForm(w, numberMode, random);
      if (base == null || base.isBlank()) {
        continue;
      }

      if (combination.morphologyProfile == MorphologyProfileMode.RADICAL_NOUNS
          && !matchesSharedLetterProfile(nounSharedLetterSource(w, base), combination)) {
        continue;
      }

      String normalized = normalizeGridWord(base);
      if (normalized.length() < 3 || seenGridForms.contains(normalized)) {
        continue;
      }

      String translationPrimary = firstNonBlank(
          w.joinMeaningsForLanguage(meaningLang, " ; "),
          w.getFrenchMeaningsJoined(),
          w.getEnglishMeaningsJoined(),
          firstMeaning(w.getMeanings()));

      String extraInfo = (w.getNominalClass() != null) ? w.getNominalClass().getClassName() : null;

      WordToFind word = new WordToFind(
          base,
          base,
          translationPrimary,
          w.getSlug(),
          "noun",
          extraInfo,
          w.getPhonetic(),
          w.getEnglishMeaningsJoined());

      out.add(word);
      seenGridForms.add(normalized);
    }

    return out;
  }

  private static List<WordToFind> selectVerbs(
      Connection conn,
      CombinationProfile combination,
      int target,
      String meaningLang,
      Set<String> seenGridForms) throws Exception {

    if (target <= 0) {
      return List.of();
    }

    LexikongoVerbRepository repo = new LexikongoVerbRepository(conn);
    List<LexVerb> verbs;
    if (combination.morphologyProfile == MorphologyProfileMode.RADICAL_VERBS && combination.radical != null) {
      verbs = repo.findRandomVerbsByLength(3, 12, target * 16, meaningLang);
    } else {
      verbs = repo.findRandomVerbsByLength(3, 12, target * 6, meaningLang);
    }

    List<WordToFind> out = new ArrayList<>();
    for (LexVerb v : verbs) {
      if (out.size() >= target) {
        break;
      }

      String base = combination.morphologyProfile == MorphologyProfileMode.RADICAL_VERBS
          ? firstNonBlank(v.getRoot(), v.getGridForm(), v.getName())
          : v.getGridForm();
      if (base == null || base.isBlank()) {
        continue;
      }

      if (combination.morphologyProfile == MorphologyProfileMode.RADICAL_VERBS
          && !matchesSharedLetterProfile(firstNonBlank(v.getRoot(), base), combination)) {
        continue;
      }

      String normalized = normalizeGridWord(base);
      if (normalized.length() < 3 || seenGridForms.contains(normalized)) {
        continue;
      }

      String translationPrimary = firstNonBlank(
          v.joinMeaningsForLanguage(meaningLang, " ; "),
          v.getFrenchMeaningsJoined(),
          v.getEnglishMeaningsJoined(),
          firstMeaning(v.getMeanings()));

      WordToFind word = new WordToFind(
          base,
          base,
          translationPrimary,
          v.getSlug(),
          "verb",
          null,
          v.getPhonetic(),
          v.getEnglishMeaningsJoined());

      out.add(word);
      seenGridForms.add(normalized);
    }

    return out;
  }

  private static String selectNounForm(LexWord word, NumberMode mode, Random random) {
    String singular = blankToNull(word.getSingular());
    String plural = blankToNull(word.getPlural());

    switch (mode) {
      case SINGULAR_ONLY:
        return singular;
      case PLURAL_ONLY:
        return plural;
      case SINGULAR_OR_PLURAL:
      default:
        if (singular != null && plural != null) {
          return random.nextBoolean() ? singular : plural;
        }
        return (singular != null) ? singular : plural;
    }
  }

  private static MorphoDominoJsonModels.PuzzleV1 buildMorphoDominoPuzzle(
      Connection conn,
      LanguageProfile language,
      CombinationProfile combination,
      int targetTileCount,
      String meaningLanguage,
      EditionTier editionTier,
      String difficulty,
      Random random,
      int index) throws Exception {

    if (!supportsMorphoDomino(combination)) {
      return null;
    }

    int safeTileTarget = normalizeDominoTileTarget(targetTileCount);
    int wordTarget = Math.max(3, safeTileTarget / 2);

    List<WordToFind> selectedWords = selectWordsFromDb(
        conn,
        language,
        combination,
        wordTarget,
        meaningLanguage,
        random);
    if (selectedWords.isEmpty()) {
      return null;
    }

    List<DominoMaterial> materials = buildDominoMaterials(selectedWords, wordTarget);
    if (materials.size() < 3) {
      return null;
    }

    String relationType = dominoRelationType(combination);
    String anchorKind = dominoAnchorKind(combination);
    String anchorValue = dominoAnchorValue(combination);
    String anchorDisplay = dominoAnchorDisplay(combination);
    String anchorNormalized = dominoAnchorNormalized(combination);
    String anchorLabel = dominoAnchorLabel(combination);
    String formLabel = dominoFormLabel(combination);

    List<MorphoDominoJsonModels.TileV1> tiles = new ArrayList<>();
    List<String> solutionOrder = new ArrayList<>();
    int tileIndex = 1;

    for (DominoMaterial material : materials) {
      MorphoDominoJsonModels.TileV1 first = dominoTile(
          "tile-" + tileIndex++,
          dominoSide(anchorKind, anchorValue, anchorDisplay, anchorNormalized, anchorLabel),
          dominoSide("form", material.baseForm, material.displayForm, material.normalized, formLabel),
          material,
          combination);
      tiles.add(first);
      solutionOrder.add(first.id);

      MorphoDominoJsonModels.TileV1 second = dominoTile(
          "tile-" + tileIndex++,
          dominoSide("form", material.baseForm, material.displayForm, material.normalized, formLabel),
          dominoSide(anchorKind, anchorValue, anchorDisplay, anchorNormalized, anchorLabel),
          material,
          combination);
      tiles.add(second);
      solutionOrder.add(second.id);
    }

    MorphoDominoJsonModels.PuzzleV1 puzzle = new MorphoDominoJsonModels.PuzzleV1();
    String publicProfileId = publicProfileToken(combination);
    puzzle.id = language.code + "-domino-" + publicProfileId + "-" + index;
    puzzle.language = language.code;
    puzzle.mode = publicProfileId;
    puzzle.difficulty = difficulty;
    puzzle.title = titleFor(language, combination, "domino", editionTier, meaningLanguage);
    puzzle.theme = themeFor(combination);
    puzzle.relationType = relationType;
    puzzle.layout = "chain";
    puzzle.tiles = tiles;
    puzzle.solutionOrder = solutionOrder;
    puzzle.meta = buildCommonMeta(language, combination, "domino", editionTier, difficulty, meaningLanguage);
    puzzle.meta.put("createdAt", Instant.now().toString());
    puzzle.meta.put("puzzleNumber", index);
    puzzle.meta.put("entryCount", materials.size());
    puzzle.meta.put("placedEntries", materials.size());
    puzzle.meta.put("tileCount", tiles.size());
    puzzle.meta.put("relationType", relationType);
    puzzle.meta.put("layout", "chain");
    puzzle.meta.put("anchorKind", anchorKind);
    puzzle.meta.put("anchorDisplay", anchorDisplay);
    MorphoDominoValidator.validatePuzzle(puzzle);
    return puzzle;
  }

  private static List<DominoMaterial> buildDominoMaterials(List<WordToFind> selectedWords, int wordTarget) {
    List<DominoMaterial> items = new ArrayList<>();
    Set<String> seen = new LinkedHashSet<>();

    for (WordToFind word : selectedWords) {
      if (items.size() >= wordTarget) {
        break;
      }
      if (word == null || blankToNull(word.getBaseForm()) == null) {
        continue;
      }

      String normalized = normalizeDominoValue(word.getBaseForm());
      if (normalized.isBlank() || seen.contains(normalized)) {
        continue;
      }

      DominoMaterial item = new DominoMaterial();
      item.baseForm = word.getBaseForm().trim();
      item.displayForm = blankToNull(word.getDisplayForm()) != null ? word.getDisplayForm().trim() : item.baseForm;
      item.normalized = normalized;
      item.translation = firstNonBlank(word.getTranslation(), word.getTranslationEn());
      item.translationEn = word.getTranslationEn();
      item.slug = word.getSlug();
      item.partOfSpeech = word.getPartOfSpeech();
      item.phonetic = word.getPhonetic();
      item.extraInfo = word.getExtraInfo();
      items.add(item);
      seen.add(normalized);
    }

    return items;
  }

  private static MorphoDominoJsonModels.TileV1 dominoTile(
      String id,
      MorphoDominoJsonModels.SideV1 left,
      MorphoDominoJsonModels.SideV1 right,
      DominoMaterial material,
      CombinationProfile combination) {

    MorphoDominoJsonModels.TileV1 tile = new MorphoDominoJsonModels.TileV1();
    tile.id = id;
    tile.left = left;
    tile.right = right;
    tile.entryRef = new MorphoDominoJsonModels.EntryRefV1();
    tile.entryRef.slug = material.slug;
    tile.entryRef.partOfSpeech = material.partOfSpeech;
    tile.entryRef.phonetic = material.phonetic;
    tile.entryRef.extraInfo = material.extraInfo;
    tile.entryRef.root = combination.radical;
    tile.entryRef.translation = material.translation;
    tile.entryRef.translationEn = material.translationEn;
    if ("noun".equalsIgnoreCase(material.partOfSpeech)) {
      if (combination.nounNumberMode != NumberMode.PLURAL_ONLY) {
        tile.entryRef.singular = material.baseForm;
      }
      if (combination.nounNumberMode == NumberMode.PLURAL_ONLY) {
        tile.entryRef.plural = material.baseForm;
      }
    }

    tile.meta = new LinkedHashMap<>();
    tile.meta.put("slug", material.slug);
    tile.meta.put("display", material.displayForm);
    tile.meta.put("translation", material.translation);
    tile.meta.put("translationEn", material.translationEn);
    tile.meta.put("partOfSpeech", material.partOfSpeech);
    tile.meta.put("extraInfo", material.extraInfo);
    return tile;
  }

  private static MorphoDominoJsonModels.SideV1 dominoSide(
      String kind,
      String value,
      String display,
      String normalized,
      String label) {

    MorphoDominoJsonModels.SideV1 side = new MorphoDominoJsonModels.SideV1();
    side.kind = kind;
    side.value = value;
    side.display = display;
    side.normalized = normalized;
    side.label = label;
    return side;
  }

  private static MemoryMatchJsonModels.PuzzleV1 buildMemoryMatchPuzzle(
      Connection conn,
      LanguageProfile language,
      CombinationProfile combination,
      int targetPairCount,
      String meaningLanguage,
      EditionTier editionTier,
      String difficulty,
      Random random,
      int index) throws Exception {

    int safePairTarget = normalizeMemoryPairTarget(targetPairCount);
    List<WordToFind> selectedWords = selectWordsFromDb(
        conn,
        language,
        combination,
        safePairTarget,
        meaningLanguage,
        random);
    if (selectedWords.isEmpty()) {
      return null;
    }

    List<MemoryMaterial> materials = buildMemoryMaterials(selectedWords, safePairTarget, meaningLanguage, combination);
    if (materials.size() < 3) {
      return null;
    }

    MemoryGridSpec gridSpec = buildMemoryGridSpec(materials.size() * 2);
    List<MemoryMatchJsonModels.CardV1> cards = new ArrayList<>();

    int pairIndex = 1;
    for (MemoryMaterial material : materials) {
      String pairId = "pair-" + pairIndex;

      MemoryMatchJsonModels.CardV1 formCard = memoryCard(
          "card-" + pairIndex + "-a",
          pairId,
          "form",
          material.baseForm,
          material.displayForm,
          material.baseNormalized,
          memoryFormLabel(combination),
          material,
          combination);
      cards.add(formCard);

      MemoryMatchJsonModels.CardV1 translationCard = memoryCard(
          "card-" + pairIndex + "-b",
          pairId,
          "translation",
          material.translation,
          material.translation,
          material.translationNormalized,
          memoryTranslationLabel(meaningLanguage),
          material,
          combination);
      cards.add(translationCard);
      pairIndex++;
    }

    Collections.shuffle(cards, random);

    MemoryMatchJsonModels.PuzzleV1 puzzle = new MemoryMatchJsonModels.PuzzleV1();
    String publicProfileId = publicProfileToken(combination);
    puzzle.id = language.code + "-memory-" + publicProfileId + "-" + index;
    puzzle.language = language.code;
    puzzle.mode = publicProfileId;
    puzzle.difficulty = difficulty;
    puzzle.title = titleFor(language, combination, "memory", editionTier, meaningLanguage);
    puzzle.theme = themeFor(combination);
    puzzle.relationType = memoryRelationType(combination);
    puzzle.layout = "grid";
    puzzle.rows = gridSpec.rows;
    puzzle.cols = gridSpec.cols;
    puzzle.pairCount = materials.size();
    puzzle.cards = cards;
    puzzle.meta = buildCommonMeta(language, combination, "memory", editionTier, difficulty, meaningLanguage);
    puzzle.meta.put("createdAt", Instant.now().toString());
    puzzle.meta.put("puzzleNumber", index);
    puzzle.meta.put("pairCount", materials.size());
    puzzle.meta.put("placedEntries", materials.size());
    puzzle.meta.put("cardCount", cards.size());
    puzzle.meta.put("relationType", puzzle.relationType);
    puzzle.meta.put("layout", puzzle.layout);
    puzzle.meta.put("rows", gridSpec.rows);
    puzzle.meta.put("cols", gridSpec.cols);
    MemoryMatchValidator.validatePuzzle(puzzle);
    return puzzle;
  }

  private static List<MemoryMaterial> buildMemoryMaterials(
      List<WordToFind> selectedWords,
      int pairTarget,
      String meaningLanguage,
      CombinationProfile combination) {

    List<MemoryMaterial> items = new ArrayList<>();
    Set<String> seenForms = new LinkedHashSet<>();
    Set<String> seenTranslations = new LinkedHashSet<>();

    for (WordToFind word : selectedWords) {
      if (items.size() >= pairTarget) {
        break;
      }
      if (word == null) {
        continue;
      }

      String baseForm = blankToNull(word.getBaseForm());
      String displayForm = firstNonBlank(word.getDisplayForm(), baseForm);
      String translation = firstNonBlank(word.getTranslation(), word.getTranslationEn());
      if (baseForm == null || displayForm == null || translation == null) {
        continue;
      }

      String baseNormalized = normalizeMemoryValue(displayForm);
      String translationNormalized = normalizeMemoryValue(translation);
      if (baseNormalized.isBlank() || translationNormalized.isBlank()) {
        continue;
      }
      if (baseNormalized.equals(translationNormalized)) {
        continue;
      }
      if (seenForms.contains(baseNormalized) || seenTranslations.contains(translationNormalized)) {
        continue;
      }

      MemoryMaterial material = new MemoryMaterial();
      material.baseForm = baseForm;
      material.displayForm = displayForm;
      material.baseNormalized = baseNormalized;
      material.translation = translation;
      material.translationNormalized = translationNormalized;
      material.translationEn = word.getTranslationEn();
      material.slug = word.getSlug();
      material.partOfSpeech = word.getPartOfSpeech();
      material.phonetic = word.getPhonetic();
      material.extraInfo = firstNonBlank(word.getExtraInfo(), memoryExtraInfo(combination));
      material.root = combination.radical;
      if ("noun".equalsIgnoreCase(word.getPartOfSpeech())) {
        if (combination.nounNumberMode != NumberMode.PLURAL_ONLY) {
          material.singular = baseForm;
        }
        if (combination.nounNumberMode == NumberMode.PLURAL_ONLY) {
          material.plural = baseForm;
        }
      }

      items.add(material);
      seenForms.add(baseNormalized);
      seenTranslations.add(translationNormalized);
    }

    return items;
  }

  private static MemoryMatchJsonModels.CardV1 memoryCard(
      String id,
      String pairId,
      String kind,
      String value,
      String display,
      String normalized,
      String label,
      MemoryMaterial material,
      CombinationProfile combination) {

    MemoryMatchJsonModels.CardV1 card = new MemoryMatchJsonModels.CardV1();
    card.id = id;
    card.pairId = pairId;
    card.kind = kind;
    card.value = value;
    card.display = display;
    card.normalized = normalized;
    card.label = label;
    card.hint = "translation".equals(kind)
        ? firstNonBlank(material.displayForm, material.baseForm)
        : firstNonBlank(material.translation, material.translationEn);

    card.entryRef = new MemoryMatchJsonModels.EntryRefV1();
    card.entryRef.slug = material.slug;
    card.entryRef.partOfSpeech = material.partOfSpeech;
    card.entryRef.phonetic = material.phonetic;
    card.entryRef.extraInfo = material.extraInfo;
    card.entryRef.translation = material.translation;
    card.entryRef.translationEn = material.translationEn;
    card.entryRef.root = combination.radical;
    card.entryRef.singular = material.singular;
    card.entryRef.plural = material.plural;

    card.meta = new LinkedHashMap<>();
    card.meta.put("display", display);
    card.meta.put("translation", material.translation);
    card.meta.put("translationEn", material.translationEn);
    card.meta.put("partOfSpeech", material.partOfSpeech);
    card.meta.put("extraInfo", material.extraInfo);
    card.meta.put("root", material.root);
    return card;
  }

  private static MemoryGridSpec buildMemoryGridSpec(int totalCards) {
    int cardCount = Math.max(4, totalCards);
    int cols = Math.max(4, (int) Math.ceil(Math.sqrt(cardCount)));
    while (cols < cardCount && (cardCount % cols) != 0) {
      cols++;
    }
    int rows = (int) Math.ceil((double) cardCount / cols);
    return new MemoryGridSpec(rows, cols);
  }

  private static ScrabbleLikeJsonModels.PuzzleV1 buildScrabbleLikePuzzle(
      Connection conn,
      LanguageProfile language,
      CombinationProfile combination,
      int targetChallengeCount,
      String meaningLanguage,
      EditionTier editionTier,
      String difficulty,
      Random random,
      int index) throws Exception {

    int safeChallengeTarget = normalizeScrabbleChallengeTarget(targetChallengeCount);
    List<WordToFind> selectedWords = selectWordsFromDb(
        conn,
        language,
        combination,
        safeChallengeTarget + EXTRA_WORD_BUFFER,
        meaningLanguage,
        random);
    if (selectedWords.isEmpty()) {
      return null;
    }

    List<ScrabbleMaterial> materials = buildScrabbleMaterials(selectedWords, safeChallengeTarget, combination);
    if (materials.size() < 3) {
      return null;
    }

    List<ScrabbleLikeJsonModels.ChallengeV1> challenges = new ArrayList<>();
    int totalScore = 0;
    int challengeIndex = 1;

    for (ScrabbleMaterial material : materials) {
      ScrabbleLikeJsonModels.ChallengeV1 challenge = new ScrabbleLikeJsonModels.ChallengeV1();
      challenge.id = "challenge-" + challengeIndex++;
      challenge.kind = "word-build";
      challenge.answer = material.baseForm;
      challenge.display = material.displayForm;
      challenge.normalized = material.normalized;
      challenge.translation = material.translation;
      challenge.translationEn = material.translationEn;
      challenge.phonetic = material.phonetic;
      challenge.label = scrabbleChallengeLabel(combination, material.partOfSpeech);
      challenge.slotCount = material.normalized.length();
      challenge.score = material.score;
      challenge.rack = buildScrabbleRack(material, random);
      challenge.entryRef = new ScrabbleLikeJsonModels.EntryRefV1();
      challenge.entryRef.slug = material.slug;
      challenge.entryRef.partOfSpeech = material.partOfSpeech;
      challenge.entryRef.phonetic = material.phonetic;
      challenge.entryRef.extraInfo = material.extraInfo;
      challenge.entryRef.translation = material.translation;
      challenge.entryRef.translationEn = material.translationEn;
      challenge.entryRef.root = material.root;
      challenge.entryRef.singular = material.singular;
      challenge.entryRef.plural = material.plural;

      challenge.meta = new LinkedHashMap<>();
      challenge.meta.put("display", material.displayForm);
      challenge.meta.put("translation", material.translation);
      challenge.meta.put("translationEn", material.translationEn);
      challenge.meta.put("partOfSpeech", material.partOfSpeech);
      challenge.meta.put("extraInfo", material.extraInfo);
      challenge.meta.put("score", material.score);
      challenge.meta.put("rackSize", challenge.rack.size());
      challenge.meta.put("root", material.root);
      challenges.add(challenge);
      totalScore += material.score;
    }

    ScrabbleLikeJsonModels.PuzzleV1 puzzle = new ScrabbleLikeJsonModels.PuzzleV1();
    String publicProfileId = publicProfileToken(combination);
    puzzle.id = language.code + "-scrabble-" + publicProfileId + "-" + index;
    puzzle.language = language.code;
    puzzle.mode = publicProfileId;
    puzzle.difficulty = difficulty;
    puzzle.title = titleFor(language, combination, "scrabble", editionTier, meaningLanguage);
    puzzle.theme = themeFor(combination);
    puzzle.relationType = scrabbleRelationType(combination);
    puzzle.layout = "stacked-racks";
    puzzle.challengeCount = challenges.size();
    puzzle.totalScore = totalScore;
    puzzle.challenges = challenges;
    puzzle.meta = buildCommonMeta(language, combination, "scrabble", editionTier, difficulty, meaningLanguage);
    puzzle.meta.put("createdAt", Instant.now().toString());
    puzzle.meta.put("puzzleNumber", index);
    puzzle.meta.put("challengeCount", challenges.size());
    puzzle.meta.put("placedEntries", challenges.size());
    puzzle.meta.put("totalScore", totalScore);
    puzzle.meta.put("relationType", puzzle.relationType);
    puzzle.meta.put("layout", puzzle.layout);
    ScrabbleLikeValidator.validatePuzzle(puzzle);
    return puzzle;
  }

  private static List<ScrabbleMaterial> buildScrabbleMaterials(
      List<WordToFind> selectedWords,
      int challengeTarget,
      CombinationProfile combination) {

    List<ScrabbleMaterial> items = new ArrayList<>();
    Set<String> seenForms = new LinkedHashSet<>();

    for (WordToFind word : selectedWords) {
      if (items.size() >= challengeTarget) {
        break;
      }
      if (word == null) {
        continue;
      }

      String baseForm = blankToNull(word.getBaseForm());
      String displayForm = firstNonBlank(word.getDisplayForm(), baseForm);
      String normalized = normalizeScrabbleValue(displayForm);
      if (baseForm == null || displayForm == null || normalized.length() < 3 || normalized.length() > 12) {
        continue;
      }
      if (seenForms.contains(normalized)) {
        continue;
      }

      ScrabbleMaterial material = new ScrabbleMaterial();
      material.baseForm = baseForm;
      material.displayForm = displayForm;
      material.normalized = normalized;
      material.translation = firstNonBlank(word.getTranslation(), word.getTranslationEn());
      material.translationEn = word.getTranslationEn();
      material.slug = word.getSlug();
      material.partOfSpeech = word.getPartOfSpeech();
      material.phonetic = word.getPhonetic();
      material.extraInfo = firstNonBlank(word.getExtraInfo(), memoryExtraInfo(combination));
      material.root = combination.radical;
      if ("noun".equalsIgnoreCase(word.getPartOfSpeech())) {
        if (combination.nounNumberMode != NumberMode.PLURAL_ONLY) {
          material.singular = baseForm;
        }
        if (combination.nounNumberMode == NumberMode.PLURAL_ONLY) {
          material.plural = baseForm;
        }
      }
      material.score = scoreScrabbleWord(normalized);

      items.add(material);
      seenForms.add(normalized);
    }

    return items;
  }

  private static List<ScrabbleLikeJsonModels.TileV1> buildScrabbleRack(ScrabbleMaterial material, Random random) {
    List<ScrabbleLikeJsonModels.TileV1> rack = new ArrayList<>();
    char[] letters = material.normalized.toCharArray();
    for (int index = 0; index < letters.length; index += 1) {
      char letter = letters[index];
      ScrabbleLikeJsonModels.TileV1 tile = new ScrabbleLikeJsonModels.TileV1();
      tile.id = "tile-" + (index + 1);
      tile.letter = String.valueOf(letter);
      tile.normalized = tile.letter;
      tile.points = scrabbleLetterPoints(letter);
      rack.add(tile);
    }
    Collections.shuffle(rack, random);
    return rack;
  }

  private static int normalizeScrabbleChallengeTarget(int targetChallengeCount) {
    int safe = targetChallengeCount > 0 ? targetChallengeCount : DEFAULT_MAX_ENTRIES;
    return Math.max(4, safe);
  }

  private static String normalizeScrabbleValue(String value) {
    return normalizeGridWord(value);
  }

  private static int scoreScrabbleWord(String normalized) {
    int score = 0;
    for (int index = 0; index < normalized.length(); index += 1) {
      score += scrabbleLetterPoints(normalized.charAt(index));
    }
    if (normalized.length() >= 7) {
      score += 5;
    } else if (normalized.length() >= 5) {
      score += 2;
    }
    return Math.max(1, score);
  }

  private static int scrabbleLetterPoints(char letter) {
    switch (Character.toUpperCase(letter)) {
      case 'A':
      case 'E':
      case 'I':
      case 'N':
      case 'O':
      case 'R':
      case 'S':
      case 'T':
      case 'U':
        return 1;
      case 'B':
      case 'D':
      case 'G':
      case 'K':
      case 'L':
      case 'M':
      case 'P':
        return 2;
      case 'F':
      case 'H':
      case 'J':
      case 'V':
      case 'W':
      case 'Y':
        return 3;
      case 'C':
      case 'Q':
      case 'X':
      case 'Z':
      default:
        return 4;
    }
  }

  private static MorphoAnagramJsonModels.PuzzleV1 buildMorphoAnagramPuzzle(
      Connection conn,
      LanguageProfile language,
      CombinationProfile combination,
      int targetChallengeCount,
      String meaningLanguage,
      EditionTier editionTier,
      String difficulty,
      Random random,
      int index) throws Exception {

    int safeChallengeTarget = normalizeAnagramChallengeTarget(targetChallengeCount);
    List<WordToFind> selectedWords = selectWordsFromDb(
        conn,
        language,
        combination,
        safeChallengeTarget + EXTRA_WORD_BUFFER,
        meaningLanguage,
        random);
    if (selectedWords.isEmpty()) {
      return null;
    }

    List<AnagramMaterial> materials = buildAnagramMaterials(selectedWords, safeChallengeTarget, combination);
    if (materials.size() < 3) {
      return null;
    }

    List<MorphoAnagramJsonModels.ChallengeV1> challenges = new ArrayList<>();
    int totalScore = 0;
    int challengeIndex = 1;

    for (AnagramMaterial material : materials) {
      MorphoAnagramJsonModels.ChallengeV1 challenge = new MorphoAnagramJsonModels.ChallengeV1();
      challenge.id = "challenge-" + challengeIndex++;
      challenge.kind = "segment-anagram";
      challenge.answer = material.baseForm;
      challenge.display = material.displayForm;
      challenge.normalized = material.normalized;
      challenge.translation = material.translation;
      challenge.translationEn = material.translationEn;
      challenge.phonetic = material.phonetic;
      challenge.label = anagramChallengeLabel(combination, material.partOfSpeech);
      challenge.segmentCount = material.pieces.size();
      challenge.score = material.score;
      challenge.pieces = buildAnagramPieces(material, random);
      challenge.solutionOrder = new ArrayList<>(material.solutionOrder);
      challenge.entryRef = new MorphoAnagramJsonModels.EntryRefV1();
      challenge.entryRef.slug = material.slug;
      challenge.entryRef.partOfSpeech = material.partOfSpeech;
      challenge.entryRef.phonetic = material.phonetic;
      challenge.entryRef.extraInfo = material.extraInfo;
      challenge.entryRef.translation = material.translation;
      challenge.entryRef.translationEn = material.translationEn;
      challenge.entryRef.root = material.root;
      challenge.entryRef.singular = material.singular;
      challenge.entryRef.plural = material.plural;

      challenge.meta = new LinkedHashMap<>();
      challenge.meta.put("display", material.displayForm);
      challenge.meta.put("translation", material.translation);
      challenge.meta.put("translationEn", material.translationEn);
      challenge.meta.put("partOfSpeech", material.partOfSpeech);
      challenge.meta.put("extraInfo", material.extraInfo);
      challenge.meta.put("score", material.score);
      challenge.meta.put("segmentCount", material.pieces.size());
      challenge.meta.put("root", material.root);
      challenge.meta.put("roles", new ArrayList<>(material.roles));
      challenges.add(challenge);
      totalScore += material.score;
    }

    MorphoAnagramJsonModels.PuzzleV1 puzzle = new MorphoAnagramJsonModels.PuzzleV1();
    String publicProfileId = publicProfileToken(combination);
    puzzle.id = language.code + "-anagram-" + publicProfileId + "-" + index;
    puzzle.language = language.code;
    puzzle.mode = publicProfileId;
    puzzle.difficulty = difficulty;
    puzzle.title = titleFor(language, combination, "anagram", editionTier, meaningLanguage);
    puzzle.theme = themeFor(combination);
    puzzle.relationType = anagramRelationType(combination);
    puzzle.layout = "stacked-segments";
    puzzle.challengeCount = challenges.size();
    puzzle.totalScore = totalScore;
    puzzle.challenges = challenges;
    puzzle.meta = buildCommonMeta(language, combination, "anagram", editionTier, difficulty, meaningLanguage);
    puzzle.meta.put("createdAt", Instant.now().toString());
    puzzle.meta.put("puzzleNumber", index);
    puzzle.meta.put("challengeCount", challenges.size());
    puzzle.meta.put("placedEntries", challenges.size());
    puzzle.meta.put("totalScore", totalScore);
    puzzle.meta.put("relationType", puzzle.relationType);
    puzzle.meta.put("layout", puzzle.layout);
    MorphoAnagramValidator.validatePuzzle(puzzle);
    return puzzle;
  }

  private static List<AnagramMaterial> buildAnagramMaterials(
      List<WordToFind> selectedWords,
      int challengeTarget,
      CombinationProfile combination) {

    List<AnagramMaterial> items = new ArrayList<>();
    Set<String> seenForms = new LinkedHashSet<>();

    for (WordToFind word : selectedWords) {
      if (items.size() >= challengeTarget) {
        break;
      }
      if (word == null) {
        continue;
      }

      String baseForm = blankToNull(word.getBaseForm());
      String displayForm = firstNonBlank(word.getDisplayForm(), baseForm);
      String normalized = normalizeAnagramValue(displayForm);
      if (baseForm == null || displayForm == null || normalized.length() < 4 || normalized.length() > 14) {
        continue;
      }
      if (seenForms.contains(normalized)) {
        continue;
      }

      List<SegmentChunk> chunks = buildAnagramSegments(normalized, combination);
      if (chunks.size() < 2) {
        continue;
      }

      AnagramMaterial material = new AnagramMaterial();
      material.baseForm = baseForm;
      material.displayForm = displayForm;
      material.normalized = normalized;
      material.translation = firstNonBlank(word.getTranslation(), word.getTranslationEn());
      material.translationEn = word.getTranslationEn();
      material.slug = word.getSlug();
      material.partOfSpeech = word.getPartOfSpeech();
      material.phonetic = word.getPhonetic();
      material.extraInfo = firstNonBlank(word.getExtraInfo(), memoryExtraInfo(combination));
      material.root = combination.radical;
      if ("noun".equalsIgnoreCase(word.getPartOfSpeech())) {
        if (combination.nounNumberMode != NumberMode.PLURAL_ONLY) {
          material.singular = baseForm;
        }
        if (combination.nounNumberMode == NumberMode.PLURAL_ONLY) {
          material.plural = baseForm;
        }
      }

      int segmentIndex = 1;
      for (SegmentChunk chunk : chunks) {
        AnagramPieceMaterial piece = new AnagramPieceMaterial();
        piece.id = "piece-" + segmentIndex++;
        piece.text = chunk.value;
        piece.normalized = chunk.value;
        piece.role = chunk.role;
        piece.points = scoreAnagramSegment(chunk.value, chunk.role);
        material.pieces.add(piece);
        material.solutionOrder.add(piece.id);
        material.roles.add(chunk.role);
        material.score += piece.points;
      }

      if (material.score <= 0) {
        material.score = Math.max(1, normalized.length());
      }

      items.add(material);
      seenForms.add(normalized);
    }

    return items;
  }

  private static List<MorphoAnagramJsonModels.PieceV1> buildAnagramPieces(AnagramMaterial material, Random random) {
    List<MorphoAnagramJsonModels.PieceV1> pieces = new ArrayList<>();
    for (AnagramPieceMaterial pieceMaterial : material.pieces) {
      MorphoAnagramJsonModels.PieceV1 piece = new MorphoAnagramJsonModels.PieceV1();
      piece.id = pieceMaterial.id;
      piece.text = pieceMaterial.text;
      piece.normalized = pieceMaterial.normalized;
      piece.role = pieceMaterial.role;
      piece.points = pieceMaterial.points;
      pieces.add(piece);
    }
    Collections.shuffle(pieces, random);
    return pieces;
  }

  private static List<SegmentChunk> buildAnagramSegments(String normalized, CombinationProfile combination) {
    String word = normalizeAnagramValue(normalized);
    if (word.length() < 2) {
      return List.of();
    }

    List<SegmentChunk> chunks = new ArrayList<>();
    String root = normalizeAnagramValue(combination.radical);
    int rootIndex = (!root.isBlank() && root.length() >= 2) ? word.indexOf(root) : -1;

    if (rootIndex >= 0) {
      String prefix = word.substring(0, rootIndex);
      String rootValue = word.substring(rootIndex, rootIndex + root.length());
      String suffix = word.substring(rootIndex + root.length());

      chunks.addAll(segmentChunk(prefix, "prefix", true));
      chunks.addAll(segmentChunk(rootValue, "root", false));
      chunks.addAll(segmentChunk(suffix, "suffix", true));
    } else {
      chunks.addAll(segmentChunk(word, "segment", false));
    }

    chunks.removeIf((chunk) -> chunk == null || chunk.value == null || chunk.value.isBlank());
    return rebalanceAnagramSegments(chunks);
  }

  private static List<SegmentChunk> segmentChunk(String value, String role, boolean allowSingleLetter) {
    String normalized = normalizeAnagramValue(value);
    if (normalized.isBlank()) {
      return List.of();
    }

    if (allowSingleLetter && normalized.length() <= 2) {
      return List.of(new SegmentChunk(normalized, role));
    }
    if (!allowSingleLetter && normalized.length() <= 3) {
      return List.of(new SegmentChunk(normalized, role));
    }

    List<Integer> sizes = computeAnagramChunkSizes(normalized.length());
    List<SegmentChunk> parts = new ArrayList<>(sizes.size());
    int offset = 0;
    for (Integer size : sizes) {
      int next = Math.min(normalized.length(), offset + size);
      if (next > offset) {
        parts.add(new SegmentChunk(normalized.substring(offset, next), role));
      }
      offset = next;
    }
    return parts;
  }

  private static List<SegmentChunk> rebalanceAnagramSegments(List<SegmentChunk> chunks) {
    if (chunks.size() >= 2) {
      return chunks;
    }
    if (chunks.isEmpty()) {
      return chunks;
    }

    SegmentChunk only = chunks.get(0);
    if (only.value.length() < 4) {
      return chunks;
    }

    List<SegmentChunk> split = new ArrayList<>();
    List<Integer> sizes = computeAnagramChunkSizes(only.value.length());
    int offset = 0;
    for (Integer size : sizes) {
      int next = Math.min(only.value.length(), offset + size);
      if (next > offset) {
        split.add(new SegmentChunk(only.value.substring(offset, next), only.role));
      }
      offset = next;
    }
    return split;
  }

  private static List<Integer> computeAnagramChunkSizes(int length) {
    List<Integer> sizes = new ArrayList<>();
    if (length <= 3) {
      sizes.add(length);
      return sizes;
    }

    int quotient = length / 3;
    int remainder = length % 3;

    if (remainder == 0) {
      for (int index = 0; index < quotient; index += 1) {
        sizes.add(3);
      }
      return sizes;
    }

    if (remainder == 1) {
      if (quotient <= 1) {
        sizes.add(2);
        sizes.add(2);
        return sizes;
      }
      sizes.add(2);
      sizes.add(2);
      for (int index = 0; index < quotient - 1; index += 1) {
        sizes.add(3);
      }
      return sizes;
    }

    sizes.add(2);
    for (int index = 0; index < quotient; index += 1) {
      sizes.add(3);
    }
    return sizes;
  }

  private static int normalizeAnagramChallengeTarget(int targetChallengeCount) {
    int safe = targetChallengeCount > 0 ? targetChallengeCount : DEFAULT_MAX_ENTRIES;
    return Math.max(4, safe);
  }

  private static String normalizeAnagramValue(String value) {
    return normalizeGridWord(value);
  }

  private static int scoreAnagramSegment(String normalizedSegment, String role) {
    int base = Math.max(1, normalizeAnagramValue(normalizedSegment).length());
    if ("root".equals(role)) {
      return base + 2;
    }
    if ("prefix".equals(role) || "suffix".equals(role)) {
      return base + 1;
    }
    return base;
  }

  private static WordSearchJsonModels.PuzzleV1 toWordSearchJsonPuzzle(
      WordSearchPuzzle puzzle,
      LanguageProfile language,
      CombinationProfile combination,
      EditionTier editionTier,
      String difficulty,
      String meaningLanguageCode,
      int index) {

    WordSearchJsonModels.PuzzleV1 json = new WordSearchJsonModels.PuzzleV1();
    String publicProfileId = publicProfileToken(combination);
    json.id = (puzzle.getId() != null && !puzzle.getId().isBlank())
        ? puzzle.getId()
      : puzzle.getLanguageCode() + "-wordsearch-" + publicProfileId + "-" + index;

    json.language = puzzle.getLanguageCode();
    json.mode = publicProfileId;
    json.difficulty = difficulty;
    json.title = puzzle.getTitle();
    json.theme = puzzle.getTheme();
    json.rows = puzzle.getRows();
    json.cols = puzzle.getCols();

    List<String> gridLines = new ArrayList<>(puzzle.getGrid().length);
    for (char[] row : puzzle.getGrid()) {
      gridLines.add(new String(row));
    }
    json.grid = gridLines;

    List<WordSearchJsonModels.EntryV1> entries = new ArrayList<>();
    if (puzzle.getWords() != null) {
      for (WordToFind w : puzzle.getWords()) {
        WordSearchJsonModels.EntryV1 e = new WordSearchJsonModels.EntryV1();
        e.base = w.getBaseForm();
        e.display = w.getDisplayForm();
        e.translation = w.getTranslation();
        e.translationEn = w.getTranslationEn();
        e.phonetic = w.getPhonetic();
        e.slug = w.getSlug();
        e.partOfSpeech = w.getPartOfSpeech();
        e.extraInfo = w.getExtraInfo();
        e.semanticTags = SemanticTagger.guessTags(e.translation);
        entries.add(e);
      }
    }
    json.entries = entries;

    List<WordSearchJsonModels.PlacementV1> placements = new ArrayList<>();
    for (WordPlacement p : puzzle.getPlacements()) {
      WordSearchJsonModels.PlacementV1 pl = new WordSearchJsonModels.PlacementV1();
      pl.word = p.getWord();
      pl.row = p.getRow();
      pl.col = p.getCol();
      pl.direction = p.getDirection().name();
      pl.length = (p.getWord() != null) ? p.getWord().length() : 0;
      placements.add(pl);
    }
    json.placements = placements;

    Map<String, Object> meta = buildCommonMeta(language, combination, "wordsearch", editionTier, difficulty, meaningLanguageCode);
    meta.put("createdAt", Instant.now().toString());
    meta.put("puzzleNumber", index);
    enrichWordsearchMeta(meta, entries, puzzle.getRows(), puzzle.getCols());
    json.meta = meta;
    return json;
  }

  private static CrosswordJsonModels.PuzzleV1 buildCrosswordPuzzle(
      List<WordToFind> words,
      LanguageProfile language,
      CombinationProfile combination,
      int rows,
      int cols,
      int entryLimit,
      String meaningLanguage,
      EditionTier editionTier,
      String difficulty,
      int index) {

    char[][] grid = new char[rows][cols];
    for (int r = 0; r < rows; r++) {
      for (int c = 0; c < cols; c++) {
        grid[r][c] = '#';
      }
    }

    List<CrossPlacedWord> placed = new ArrayList<>();
    List<WordToFind> orderedWords = orderWordsForDenseCrossword(words, entryLimit);

    WordToFind anchorWord = findCrosswordAnchor(orderedWords, rows, cols);
    if (anchorWord != null) {
      String answer = normalizeGridWord(anchorWord.getBaseForm());
      placeCrosswordAnchor(grid, answer, anchorWord, placed);
    }

    for (WordToFind w : orderedWords) {
      if (placed.size() >= entryLimit) {
        break;
      }
      if (isAlreadyPlaced(placed, w)) {
        continue;
      }

      String answer = normalizeGridWord(w.getBaseForm());
      if (answer.length() < 3) {
        continue;
      }

      CrossPlacedWord candidate = tryPlaceWithCrossing(grid, answer, w);
      if (candidate == null && placed.isEmpty()) {
        // On n'autorise un placement libre que pour le tout premier mot.
        candidate = tryPlaceSimple(grid, answer, w);
      }
      if (candidate != null) {
        placed.add(candidate);
      }
    }

    CrosswordJsonModels.PuzzleV1 puzzle = new CrosswordJsonModels.PuzzleV1();
    String publicProfileId = publicProfileToken(combination);
    puzzle.id = language.code + "-cross-" + publicProfileId + "-" + index;
    puzzle.language = language.code;
    puzzle.mode = publicProfileId;
    puzzle.difficulty = difficulty;
    puzzle.title = titleFor(language, combination, "crossword", editionTier, meaningLanguage);
    puzzle.theme = themeFor(combination);
    puzzle.rows = rows;
    puzzle.cols = cols;

    List<String> gridLines = new ArrayList<>(rows);
    for (int r = 0; r < rows; r++) {
      gridLines.add(new String(grid[r]));
    }
    puzzle.grid = gridLines;

    List<CrosswordJsonModels.EntryV1> entries = new ArrayList<>();
    Set<String> nominalClasses = new LinkedHashSet<>();
    Set<String> semanticDomains = new LinkedHashSet<>();

    Map<String, Integer> startNumbers = buildCrosswordStartNumbers(placed);
    List<CrossPlacedWord> orderedPlaced = sortPlacedWordsForExport(placed);

    for (CrossPlacedWord pw : orderedPlaced) {
      String answer = normalizeGridWord(pw.word.getBaseForm());
      int number = startNumbers.getOrDefault(startKey(pw.row, pw.col), 0);

      CrosswordJsonModels.EntryV1 e = new CrosswordJsonModels.EntryV1();
      e.id = number + (pw.across ? "-A" : "-D");
      e.number = number;
      e.direction = pw.across ? "ACROSS" : "DOWN";
      e.row = pw.row;
      e.col = pw.col;
      e.answer = answer;
      e.display = pw.word.getDisplayForm();
      e.clue = pw.word.getTranslation();
      e.translation = pw.word.getTranslation();
      e.translationEn = pw.word.getTranslationEn();
      e.phonetic = pw.word.getPhonetic();
      e.slug = pw.word.getSlug();
      e.partOfSpeech = pw.word.getPartOfSpeech();
      e.extraInfo = pw.word.getExtraInfo();
      e.semanticTags = SemanticTagger.guessTags(e.translation);

      if (e.extraInfo != null && !e.extraInfo.isBlank()) {
        nominalClasses.add(e.extraInfo);
      }
      if (e.semanticTags != null) {
        semanticDomains.addAll(e.semanticTags);
      }

      entries.add(e);
    }

    puzzle.entries = entries;

    Map<String, Object> meta = buildCommonMeta(language, combination, "crossword", editionTier, difficulty, meaningLanguage);
    meta.put("createdAt", Instant.now().toString());
    meta.put("puzzleNumber", index);
    enrichCrosswordMeta(meta, entries, nominalClasses, semanticDomains);
    int filledCells = countFilledGridCells(puzzle.grid, '#');
    meta.put("placedEntries", entries.size());
    meta.put("filledCells", filledCells);
    meta.put("fillRatio", computeFillRatio(filledCells, rows * cols));
    puzzle.meta = meta;

    return puzzle;
  }

  private static CrosswordJsonModels.PuzzleV1 buildBestGeneratedCrosswordPuzzle(
      Connection conn,
      LanguageProfile language,
      CombinationProfile combination,
      int rows,
      int cols,
      int maxEntries,
      String meaningLanguage,
      EditionTier editionTier,
      String difficulty,
      Random random,
      int index) throws Exception {

    CrosswordJsonModels.PuzzleV1 best = null;
    int candidateTarget = Math.max(maxEntries + EXTRA_WORD_BUFFER, maxEntries * 2);

    for (int attempt = 0; attempt < DENSE_GENERATION_ATTEMPTS; attempt++) {
      List<WordToFind> selectedWords = selectWordsFromDb(
          conn,
          language,
          combination,
          candidateTarget,
          meaningLanguage,
          random);
      if (selectedWords.isEmpty()) {
        continue;
      }

      Collections.shuffle(selectedWords, random);
      CrosswordJsonModels.PuzzleV1 candidate = buildCrosswordPuzzle(
          selectedWords,
          language,
          combination,
          rows,
          cols,
          maxEntries,
          meaningLanguage,
          editionTier,
          difficulty,
          index);

      if (isBetterCrosswordPuzzle(candidate, best)) {
        best = candidate;
      }

      if (countCrosswordEntries(candidate) >= maxEntries) {
        break;
      }
    }

    if (best != null && !CrosswordGridQuality.whiteCellsOrthogonallyConnected(best)) {
      CrosswordJsonModels.PuzzleV1 bestConnected = null;
      int extra = Math.max(30, DENSE_GENERATION_ATTEMPTS * 4);
      for (int attempt = DENSE_GENERATION_ATTEMPTS; attempt < DENSE_GENERATION_ATTEMPTS + extra; attempt++) {
        List<WordToFind> selectedWords = selectWordsFromDb(
            conn,
            language,
            combination,
            candidateTarget,
            meaningLanguage,
            random);
        if (selectedWords.isEmpty()) {
          continue;
        }
        Collections.shuffle(selectedWords, random);
        CrosswordJsonModels.PuzzleV1 candidate = buildCrosswordPuzzle(
            selectedWords,
            language,
            combination,
            rows,
            cols,
            maxEntries,
            meaningLanguage,
            editionTier,
            difficulty,
            index);
        if (!CrosswordGridQuality.whiteCellsOrthogonallyConnected(candidate)) {
          continue;
        }
        if (bestConnected == null || isBetterCrosswordPuzzle(candidate, bestConnected)) {
          bestConnected = candidate;
        }
      }
      if (bestConnected != null) {
        best = bestConnected;
      }
    }

    if (best != null && countCrosswordEntries(best) == 0) {
      return null;
    }
    return best;
  }

  private static List<WordToFind> orderWordsForDenseCrossword(List<WordToFind> words, int entryLimit) {
    List<WordToFind> ordered = new ArrayList<>(words != null ? words : List.of());
    ordered.sort(
        Comparator
            .comparingInt((WordToFind word) -> normalizeGridWord(word != null ? word.getBaseForm() : "").length())
            .reversed()
            .thenComparing(word -> String.valueOf(word != null ? word.getBaseForm() : "")));

    if (ordered.size() > entryLimit + 6) {
      return new ArrayList<>(ordered.subList(0, entryLimit + 6));
    }
    return ordered;
  }

  private static WordToFind findCrosswordAnchor(List<WordToFind> words, int rows, int cols) {
    if (words == null) {
      return null;
    }

    for (WordToFind word : words) {
      String answer = normalizeGridWord(word != null ? word.getBaseForm() : "");
      if (answer.length() >= 3 && (answer.length() <= cols || answer.length() <= rows)) {
        return word;
      }
    }

    return null;
  }

  private static void placeCrosswordAnchor(
      char[][] grid,
      String answer,
      WordToFind word,
      List<CrossPlacedWord> placed) {

    CrossPlacedWord anchor = tryPlaceCenteredAcross(grid, answer, word);
    if (anchor == null) {
      anchor = tryPlaceCenteredDown(grid, answer, word);
    }
    if (anchor == null) {
      anchor = tryPlaceSimple(grid, answer, word);
    }
    if (anchor != null) {
      placed.add(anchor);
    }
  }

  private static CrossPlacedWord tryPlaceCenteredAcross(char[][] grid, String answer, WordToFind word) {
    char[] letters = answer.toCharArray();
    int rows = grid.length;
    int cols = grid[0].length;
    if (letters.length > cols) {
      return null;
    }

    int centerRow = rows / 2;
    int centeredStartCol = Math.max(0, (cols - letters.length) / 2);
    for (int offset = 0; offset < rows; offset++) {
      int upperRow = centerRow - offset;
      if (upperRow >= 0 && canPlaceAcross(grid, letters, upperRow, centeredStartCol)) {
        placeAcross(grid, answer, upperRow, centeredStartCol);
        return new CrossPlacedWord(word, upperRow, centeredStartCol, true);
      }

      if (offset == 0) {
        continue;
      }

      int lowerRow = centerRow + offset;
      if (lowerRow < rows && canPlaceAcross(grid, letters, lowerRow, centeredStartCol)) {
        placeAcross(grid, answer, lowerRow, centeredStartCol);
        return new CrossPlacedWord(word, lowerRow, centeredStartCol, true);
      }
    }

    return null;
  }

  private static CrossPlacedWord tryPlaceCenteredDown(char[][] grid, String answer, WordToFind word) {
    char[] letters = answer.toCharArray();
    int rows = grid.length;
    int cols = grid[0].length;
    if (letters.length > rows) {
      return null;
    }

    int centerCol = cols / 2;
    int centeredStartRow = Math.max(0, (rows - letters.length) / 2);
    for (int offset = 0; offset < cols; offset++) {
      int leftCol = centerCol - offset;
      if (leftCol >= 0 && canPlaceDown(grid, letters, centeredStartRow, leftCol)) {
        placeDown(grid, answer, centeredStartRow, leftCol);
        return new CrossPlacedWord(word, centeredStartRow, leftCol, false);
      }

      if (offset == 0) {
        continue;
      }

      int rightCol = centerCol + offset;
      if (rightCol < cols && canPlaceDown(grid, letters, centeredStartRow, rightCol)) {
        placeDown(grid, answer, centeredStartRow, rightCol);
        return new CrossPlacedWord(word, centeredStartRow, rightCol, false);
      }
    }

    return null;
  }

  private static boolean isBetterCrosswordPuzzle(
      CrosswordJsonModels.PuzzleV1 candidate,
      CrosswordJsonModels.PuzzleV1 currentBest) {

    if (candidate == null) {
      return false;
    }
    if (currentBest == null) {
      return true;
    }

    boolean cConn = CrosswordGridQuality.whiteCellsOrthogonallyConnected(candidate);
    boolean bConn = CrosswordGridQuality.whiteCellsOrthogonallyConnected(currentBest);
    if (cConn != bConn) {
      return cConn;
    }

    int candidateEntries = countCrosswordEntries(candidate);
    int currentEntries = countCrosswordEntries(currentBest);
    if (candidateEntries != currentEntries) {
      return candidateEntries > currentEntries;
    }

    return countCrosswordFilledLetters(candidate) > countCrosswordFilledLetters(currentBest);
  }

  private static int countCrosswordEntries(CrosswordJsonModels.PuzzleV1 puzzle) {
    return puzzle != null && puzzle.entries != null ? puzzle.entries.size() : 0;
  }

  private static int countCrosswordFilledLetters(CrosswordJsonModels.PuzzleV1 puzzle) {
    if (puzzle == null || puzzle.grid == null) {
      return 0;
    }

    int count = 0;
    for (String row : puzzle.grid) {
      if (row == null) {
        continue;
      }
      for (int i = 0; i < row.length(); i++) {
        if (row.charAt(i) != '#') {
          count++;
        }
      }
    }
    return count;
  }

  private static boolean isAlreadyPlaced(List<CrossPlacedWord> placed, WordToFind word) {
    String normalized = normalizeGridWord(word.getBaseForm());
    for (CrossPlacedWord p : placed) {
      if (normalizeGridWord(p.word.getBaseForm()).equals(normalized)) {
        return true;
      }
    }
    return false;
  }

  private static CrossPlacedWord tryPlaceWithCrossing(char[][] grid, String answer, WordToFind word) {
    int rows = grid.length;
    int cols = grid[0].length;
    char[] letters = answer.toCharArray();

    for (int r = 0; r < rows; r++) {
      for (int c = 0; c < cols; c++) {
        char existing = grid[r][c];
        if (existing == '#') {
          continue;
        }

        for (int i = 0; i < letters.length; i++) {
          if (letters[i] != existing) {
            continue;
          }

          int startCol = c - i;
          if (startCol >= 0 && startCol + letters.length <= cols && canPlaceAcross(grid, letters, r, startCol)) {
            placeAcross(grid, answer, r, startCol);
            return new CrossPlacedWord(word, r, startCol, true);
          }

          int startRow = r - i;
          if (startRow >= 0 && startRow + letters.length <= rows && canPlaceDown(grid, letters, startRow, c)) {
            placeDown(grid, answer, startRow, c);
            return new CrossPlacedWord(word, startRow, c, false);
          }
        }
      }
    }

    return null;
  }

  private static CrossPlacedWord tryPlaceSimple(char[][] grid, String answer, WordToFind word) {
    int rows = grid.length;
    int cols = grid[0].length;
    char[] letters = answer.toCharArray();

    for (int r = 0; r < rows; r++) {
      for (int startCol = 0; startCol + letters.length <= cols; startCol++) {
        if (canPlaceAcross(grid, letters, r, startCol)) {
          placeAcross(grid, answer, r, startCol);
          return new CrossPlacedWord(word, r, startCol, true);
        }
      }
    }

    for (int c = 0; c < cols; c++) {
      for (int startRow = 0; startRow + letters.length <= rows; startRow++) {
        if (canPlaceDown(grid, letters, startRow, c)) {
          placeDown(grid, answer, startRow, c);
          return new CrossPlacedWord(word, startRow, c, false);
        }
      }
    }

    return null;
  }

  private static boolean canPlaceAcross(char[][] grid, char[] letters, int row, int startCol) {
    int rows = grid.length;
    int cols = grid[0].length;
    int len = letters.length;
    if (startCol < 0 || startCol + len > cols) {
      return false;
    }
    if (startCol > 0 && grid[row][startCol - 1] != '#') {
      return false;
    }
    if (startCol + len < cols && grid[row][startCol + len] != '#') {
      return false;
    }
    for (int i = 0; i < len; i++) {
      int c = startCol + i;
      char g = grid[row][c];
      if (g != '#' && g != letters[i]) {
        return false;
      }
      char need = letters[i];
      if (g == '#') {
        if (row > 0 && grid[row - 1][c] != '#') {
          return false;
        }
        if (row + 1 < rows && grid[row + 1][c] != '#') {
          return false;
        }
      } else if (row > 0 && grid[row - 1][c] != '#' && grid[row - 1][c] != need) {
        return false;
      } else if (row + 1 < rows && grid[row + 1][c] != '#' && grid[row + 1][c] != need) {
        return false;
      }
    }
    return true;
  }

  private static boolean canPlaceDown(char[][] grid, char[] letters, int startRow, int col) {
    int rows = grid.length;
    int cols = grid[0].length;
    int len = letters.length;
    if (startRow < 0 || startRow + len > rows) {
      return false;
    }
    if (startRow > 0 && grid[startRow - 1][col] != '#') {
      return false;
    }
    if (startRow + len < rows && grid[startRow + len][col] != '#') {
      return false;
    }
    for (int i = 0; i < len; i++) {
      int row = startRow + i;
      char g = grid[row][col];
      if (g != '#' && g != letters[i]) {
        return false;
      }
      char need = letters[i];
      if (g == '#') {
        if (col > 0 && grid[row][col - 1] != '#') {
          return false;
        }
        if (col + 1 < cols && grid[row][col + 1] != '#') {
          return false;
        }
      } else if (col > 0 && grid[row][col - 1] != '#' && grid[row][col - 1] != need) {
        return false;
      } else if (col + 1 < cols && grid[row][col + 1] != '#' && grid[row][col + 1] != need) {
        return false;
      }
    }
    return true;
  }

  private static List<CrossPlacedWord> sortPlacedWordsForExport(List<CrossPlacedWord> placed) {
    List<CrossPlacedWord> ordered = new ArrayList<>(placed);
    ordered.sort((a, b) -> {
      int rowCompare = Integer.compare(a.row, b.row);
      if (rowCompare != 0) {
        return rowCompare;
      }
      int colCompare = Integer.compare(a.col, b.col);
      if (colCompare != 0) {
        return colCompare;
      }
      return Boolean.compare(a.across, b.across) * -1;
    });
    return ordered;
  }

  private static Map<String, Integer> buildCrosswordStartNumbers(List<CrossPlacedWord> placed) {
    Map<String, Integer> numbering = new HashMap<>();
    int number = 1;
    for (CrossPlacedWord pw : sortPlacedWordsForExport(placed)) {
      String key = startKey(pw.row, pw.col);
      if (!numbering.containsKey(key)) {
        numbering.put(key, number++);
      }
    }
    return numbering;
  }

  private static String startKey(int row, int col) {
    return row + ":" + col;
  }

  private static void placeAcross(char[][] grid, String answer, int row, int startCol) {
    char[] letters = answer.toCharArray();
    for (int i = 0; i < letters.length; i++) {
      grid[row][startCol + i] = letters[i];
    }
  }

  private static void placeDown(char[][] grid, String answer, int startRow, int col) {
    char[] letters = answer.toCharArray();
    for (int i = 0; i < letters.length; i++) {
      grid[startRow + i][col] = letters[i];
    }
  }

  private static boolean isEnglishMeaning(String meaningLanguage) {
    String ml = meaningLanguage == null ? "fr" : meaningLanguage.trim().toLowerCase(Locale.ROOT);
    return ml.startsWith("en");
  }

  private static String localizedProfileLabel(CombinationProfile combination, String meaningLanguage) {
    return isEnglishMeaning(meaningLanguage) ? profileLabelEnglish(combination) : profileLabel(combination);
  }

  private static String localizedPuzzleTypeLabel(String puzzleType, String meaningLanguage) {
    return isEnglishMeaning(meaningLanguage) ? puzzleTypeLabelEnglish(puzzleType) : puzzleTypeLabel(puzzleType);
  }

  private static String titleFor(
      LanguageProfile language,
      CombinationProfile combination,
      String puzzleType,
      EditionTier editionTier,
      String meaningLanguage) {

    String baseTitle =
        languageLabel(language)
            + " - "
            + localizedPuzzleTypeLabel(puzzleType, meaningLanguage)
            + " - "
            + localizedProfileLabel(combination, meaningLanguage);
    return baseTitle + " - " + editionLabel(editionTier);
  }

  private static String themeFor(CombinationProfile combination) {
    return profileLabel(combination);
  }

  private static String storefrontProfileLabel(CombinationProfile combination) {
    if (combination.morphologyProfile == MorphologyProfileMode.NOMINAL_CLASS) {
      if (combination.nounNumberMode == NumberMode.PLURAL_ONLY) {
        return "classes nominales au pluriel";
      }
      if (combination.nounNumberMode == NumberMode.SINGULAR_ONLY) {
        return "classes nominales au singulier";
      }
      return "classes nominales";
    }
    if (combination.morphologyProfile == MorphologyProfileMode.RADICAL_NOUNS) {
      return nounProfileLabel(combination.nounNumberMode)
          + " partageant "
          + sharedLettersDescriptor(combination)
          + " avec "
          + displayRadical(combination.radical).toLowerCase(Locale.ROOT);
    }
    if (combination.morphologyProfile == MorphologyProfileMode.RADICAL_VERBS) {
      return "verbes partageant "
          + sharedLettersDescriptor(combination)
          + " avec "
          + displayRadical(combination.radical).toLowerCase(Locale.ROOT);
    }
    if (combination.includeNouns && combination.includeVerbs) {
      return "noms et verbes";
    }
    if (combination.includeVerbs) {
      return "verbes";
    }
    if (combination.nounNumberMode == NumberMode.PLURAL_ONLY) {
      return "noms au pluriel";
    }
    if (combination.nounNumberMode == NumberMode.SINGULAR_ONLY) {
      return "noms";
    }
    return "noms";
  }

  private static String storefrontProfileObject(CombinationProfile combination) {
    if (combination.morphologyProfile == MorphologyProfileMode.NOMINAL_CLASS) {
      if (combination.nounNumberMode == NumberMode.PLURAL_ONLY) {
        return "les classes nominales au pluriel";
      }
      if (combination.nounNumberMode == NumberMode.SINGULAR_ONLY) {
        return "les classes nominales au singulier";
      }
      return "les classes nominales";
    }
    if (combination.morphologyProfile == MorphologyProfileMode.RADICAL_NOUNS) {
      return "les "
          + nounProfileLabel(combination.nounNumberMode)
          + " partageant "
          + sharedLettersDescriptor(combination)
          + " avec "
          + displayRadical(combination.radical).toLowerCase(Locale.ROOT);
    }
    if (combination.morphologyProfile == MorphologyProfileMode.RADICAL_VERBS) {
      return "les verbes partageant "
          + sharedLettersDescriptor(combination)
          + " avec "
          + displayRadical(combination.radical).toLowerCase(Locale.ROOT);
    }
    if (combination.includeNouns && combination.includeVerbs) {
      return "les noms et les verbes";
    }
    if (combination.includeVerbs) {
      return "les verbes";
    }
    if (combination.nounNumberMode == NumberMode.PLURAL_ONLY) {
      return "les noms au pluriel";
    }
    return "les noms";
  }

  private static String meaningAudienceLabel(String meaningLanguage) {
    String ml = meaningLanguage == null ? "fr" : meaningLanguage.trim().toLowerCase(Locale.ROOT);
    if (ml.startsWith("en")) {
      return "anglais";
    }
    if (ml.startsWith("es")) {
      return "espagnol";
    }
    if (ml.startsWith("pt")) {
      return "portugais";
    }
    return "français";
  }

  private static boolean includesPluralGameForms(CombinationProfile combination) {
    return combination != null
        && combination.includeNouns
        && combination.nounNumberMode != NumberMode.SINGULAR_ONLY;
  }

  private static String translationUsageNoteFor(CombinationProfile combination, String meaningLanguage) {
    if (!includesPluralGameForms(combination)) {
      return null;
    }
    String ml = meaningLanguage == null ? "fr" : meaningLanguage.trim().toLowerCase(Locale.ROOT);
    if (ml.startsWith("en")) {
      return "When a plural form is played, the meaning stays phrased in the singular.";
    }
    return "Quand une forme plurielle est jouée, le sens reste donné au singulier.";
  }

  private static String difficultyStorefrontLabel(String difficulty) {
    String raw = difficulty == null ? "" : difficulty.trim().toLowerCase(Locale.ROOT);
    if (raw.isEmpty()) {
      return "";
    }
    if (raw.equals("easy") || raw.equals("debutant")) {
      return "accessible";
    }
    if (raw.equals("medium") || raw.equals("intermediate")) {
      return "équilibré";
    }
    if (raw.equals("hard") || raw.equals("expert")) {
      return "soutenu";
    }
    return raw;
  }

  private static String countedLabel(int count, String singular, String plural) {
    return count + " " + (count == 1 ? singular : plural);
  }

  private static String puzzleCountLabel(String puzzleType, int puzzleCount) {
    int safeCount = Math.max(0, puzzleCount);
    switch (puzzleType.toLowerCase(Locale.ROOT)) {
      case "crossword":
        return countedLabel(safeCount, "grille de mots croisés", "grilles de mots croisés");
      case "wordsearch":
        return countedLabel(safeCount, "grille de mots mêlés", "grilles de mots mêlés");
      case "arrowword":
        return countedLabel(safeCount, "grille de mots fléchés", "grilles de mots fléchés");
      case "domino":
        return countedLabel(safeCount, "jeu de dominos", "jeux de dominos");
      case "memory":
        return countedLabel(safeCount, "partie de memory", "parties de memory");
      case "scrabble":
        return countedLabel(safeCount, "jeu de lettres", "jeux de lettres");
      case "anagram":
        return countedLabel(safeCount, "jeu d'anagrammes", "jeux d'anagrammes");
      default:
        return countedLabel(safeCount, "jeu de langue", "jeux de langue");
    }
  }

  private static String stationerySubtitleFor(
      CombinationProfile combination,
      EditionTier editionTier,
      String meaningLanguage) {

    if (isEnglishMeaning(meaningLanguage)) {
      return String.format(
          Locale.ROOT,
          "%s · %s · meanings in English",
          editionLabel(editionTier),
          storefrontProfileLabelEnglish(combination));
    }
    return String.format(
        Locale.ROOT,
        "%s · %s · sens %s",
        editionLabel(editionTier),
        storefrontProfileLabel(combination),
        meaningAudienceLabel(meaningLanguage));
  }

  private static String technicalDescriptionFor(
      LanguageProfile language,
      CombinationProfile combination,
      String puzzleType,
      EditionTier editionTier,
      String difficulty) {

    String editionLabel = editionTierKey(editionTier);
    return "Pack " + editionLabel + " de " + puzzleTypeLabel(puzzleType).toLowerCase(Locale.ROOT)
        + " généré depuis la base " + language.source
        + ", profil " + profileLabel(combination).toLowerCase(Locale.ROOT)
        + ", niveau " + difficulty + ".";
  }

  private static String descriptionFor(
      LanguageProfile language,
      CombinationProfile combination,
      String puzzleType,
      EditionTier editionTier,
      String difficulty,
      String meaningLanguage,
      int puzzleCount) {

    if (isEnglishMeaning(meaningLanguage)) {
      return descriptionForEnglish(
          language, combination, puzzleType, editionTier, difficulty, puzzleCount);
    }

    StringBuilder out = new StringBuilder();
    out.append("Un cahier ")
        .append(storefrontEditionDescriptor(editionTier))
        .append(" de ")
        .append(puzzleCountLabel(puzzleType, puzzleCount))
        .append(" pour travailler ")
        .append(storefrontProfileObject(combination))
        .append(" en ")
        .append(languageLabel(language).toLowerCase(Locale.ROOT))
        .append('.');

    if ("domino".equalsIgnoreCase(puzzleType)) {
      out.append(
          " Chaque partie est courte, visuelle et pensée pour faire mémoriser plus vite les correspondances utiles.");
    } else {
      out.append(
          " Le format reste simple à reprendre au fil de la semaine pour jouer, réviser et progresser sans jargon inutile.");
    }

    String storefrontDifficulty = difficultyStorefrontLabel(difficulty);
    if (!storefrontDifficulty.isBlank()) {
      out.append(" Le niveau reste ").append(storefrontDifficulty).append('.');
    }
    out.append(" Les repères restent en ").append(meaningAudienceLabel(meaningLanguage)).append('.');
    return out.toString();
  }

  private static String descriptionForEnglish(
      LanguageProfile language,
      CombinationProfile combination,
      String puzzleType,
      EditionTier editionTier,
      String difficulty,
      int puzzleCount) {

    StringBuilder out = new StringBuilder();
    out.append("A ")
        .append(storefrontEditionDescriptor(editionTier))
        .append(" workbook with ")
        .append(puzzleCountLabelEnglish(puzzleType, puzzleCount))
        .append(" to practise ")
        .append(storefrontProfileObjectEnglish(combination))
        .append(" in ")
        .append(languageLabel(language).toLowerCase(Locale.ROOT))
        .append('.');

    if ("domino".equalsIgnoreCase(puzzleType)) {
      out.append(
          " Each session is short and visual, designed to help you memorise the most useful matches faster.");
    } else {
      out.append(
          " The format stays easy to pick up through the week: play, review, and progress without unnecessary jargon.");
    }

    String storefrontDifficulty = difficultyStorefrontLabelEnglish(difficulty);
    if (!storefrontDifficulty.isBlank()) {
      out.append(" The level stays ").append(storefrontDifficulty).append('.');
    }
    out.append(" Meanings and clues are in English.");
    return out.toString();
  }

  private static String buildPackId(
      LanguageProfile language,
      CombinationProfile combination,
      String gameType,
      EditionTier editionTier) {

    // Identifiant pack unifié : segment "batch" (détails dans meta.exportCadence / meta.scope).
    return language.code + "-" + publicProfileToken(combination) + "-" + gameType + "-batch-" + LocalDate.now();
  }

  private static Map<String, Object> buildPackMeta(
      LanguageProfile language,
      CombinationProfile combination,
      String gameType,
      EditionTier editionTier,
      String difficulty,
      String meaningLanguage,
      int puzzleCount) {

    Map<String, Object> meta = buildCommonMeta(language, combination, gameType, editionTier, difficulty, meaningLanguage);
    meta.put("scope", "longoka-batch");
    meta.put("exportCadence", "weekly");
    meta.put("profileLabel", localizedProfileLabel(combination, meaningLanguage));
    meta.put("puzzleCount", puzzleCount);
    meta.put("targets", List.of("web", "jsx", "indesign"));
    meta.put("generatedOn", LocalDate.now().toString());
    meta.put("createdAt", Instant.now().toString());
    meta.put("schemaVersion", 1);
    meta.put("backofficeDescription", technicalDescriptionFor(language, combination, gameType, editionTier, difficulty));
    return meta;
  }

  private static Map<String, Object> buildCommonMeta(
      LanguageProfile language,
      CombinationProfile combination,
      String gameType,
      EditionTier editionTier,
      String difficulty,
      String meaningLanguage) {

    LocalDate publicationStart = LocalDate.now();
    LocalDate publicationEnd = publicationStart.plusDays(resolveFeaturedWindowDays() - 1L);
    LocalDate archiveAfter = publicationStart.plusDays(resolveArchiveWindowDays() - 1L);

    Map<String, Object> meta = new LinkedHashMap<>();
    meta.put("source", language.source);
    meta.put("language", language.code);
    meta.put("meaningLanguage", meaningLanguage);
    meta.put("gameType", gameType);
    meta.put("editionTier", editionTierKey(editionTier));
    meta.put("editionLabel", editionLabel(editionTier));
    meta.put("difficulty", difficulty);
    meta.put("series", "Longoka Games");
    String publicProfileId = publicProfileToken(combination);
    String editorialCode = buildProvisionalId(language, combination, gameType, editionTier);
    meta.put("bookCode", editorialCode);
    meta.put("provisionalId", editorialCode);
    meta.put("editorialFamily", language.code + "-" + publicProfileId + "-" + gameType + "-" + editionTierKey(editionTier));
    meta.put("lexicalProfile", combination.lexicalProfile);
    meta.put("publicProfileId", publicProfileId);
    meta.put("profileLabel", localizedProfileLabel(combination, meaningLanguage));
    meta.put("morphologyProfile", morphologyProfileKey(combination.morphologyProfile));
    if (combination.includeNouns) {
      meta.put("numberPolicy", numberPolicyKey(combination.nounNumberMode));
    }
    String translationUsageNote = translationUsageNoteFor(combination, meaningLanguage);
    if (translationUsageNote != null) {
      meta.put("translationUsageNote", translationUsageNote);
    }
    if (blankToNull(combination.nominalClassName) != null) {
      meta.put("nominalClassName", combination.nominalClassName);
    }
    if (blankToNull(combination.radical) != null) {
      meta.put("radical", combination.radical);
      meta.put("radicalLabel", displayRadical(combination.radical));
      meta.put("sharedLetterTarget", sharedLetterTarget(combination));
    }

    // Bloc "book" : metadonnees editoriales stables, consommees par InDesign et Longoka.
    Map<String, Object> book = new LinkedHashMap<>();
    book.put("series", meta.get("series"));
    book.put("publisher", "Editions Longoka");
    book.put("imprint", "Longoka Games");
    book.put("website", "https://longoka.com");
    book.put("language", language.code);
    book.put("meaningLanguage", meaningLanguage);
    book.put("gameType", gameType);
    book.put("editionTier", editionTierKey(editionTier));
    book.put("editionLabel", editionLabel(editionTier));
    book.put("difficulty", difficulty);
    book.put("bookCode", meta.get("bookCode"));
    book.put("createdAt", Instant.now().toString());
    book.put("generatedOn", publicationStart.toString());
    book.put("publishedAt", publicationStart.toString());
    book.put("publicationStart", publicationStart.toString());
    book.put("publicationEnd", publicationEnd.toString());
    book.put("archiveAfter", archiveAfter.toString());
    book.put("isbn", resolveReleaseIsbn(String.valueOf(meta.get("bookCode"))));
    meta.put("book", book);

    return meta;
  }

  private static int resolveFeaturedWindowDays() {
    String featuredEnv = System.getenv("LONGOKA_GAME_BOOK_FEATURED_DAYS");
    if (featuredEnv != null && !featuredEnv.isBlank()) {
      try {
        return Math.max(1, Integer.parseInt(featuredEnv.trim()));
      } catch (NumberFormatException ignored) {
      }
    }
    String env = System.getenv("LONGOKA_GAME_BOOK_WINDOW_DAYS");
    if (env != null && !env.isBlank()) {
      try {
        return Math.max(1, Integer.parseInt(env.trim()));
      } catch (NumberFormatException ignored) {
      }
    }
    String prop = System.getProperty("longoka.game.book.window.days");
    if (prop != null && !prop.isBlank()) {
      try {
        return Math.max(1, Integer.parseInt(prop.trim()));
      } catch (NumberFormatException ignored) {
      }
    }
    return 8;
  }

  private static int resolveArchiveWindowDays() {
    String env = System.getenv("LONGOKA_GAME_BOOK_ARCHIVE_DAYS");
    if (env != null && !env.isBlank()) {
      try {
        return Math.max(1, Integer.parseInt(env.trim()));
      } catch (NumberFormatException ignored) {
      }
    }
    String prop = System.getProperty("longoka.game.book.archive.days");
    if (prop != null && !prop.isBlank()) {
      try {
        return Math.max(1, Integer.parseInt(prop.trim()));
      } catch (NumberFormatException ignored) {
      }
    }
    return 30;
  }

  /**
   * ISBN reel pour une sortie imprimee : variable d'environnement {@code LONGOKA_BOOK_ISBN}
   * ou propriete systeme {@code longoka.book.isbn}. Sinon ISBN-13 fictif mais checksum valide.
   */
  private static String resolveReleaseIsbn(String seed) {
    String env = System.getenv("LONGOKA_BOOK_ISBN");
    if (env != null && !env.isBlank()) {
      return env.trim();
    }
    String prop = System.getProperty("longoka.book.isbn");
    if (prop != null && !prop.isBlank()) {
      return prop.trim();
    }
    return buildFictionalIsbn13(seed);
  }

  /**
   * Met a jour {@code meta.book} une fois le titre du pack connu (exports boutiques / InDesign).
   * Renseigne aussi {@code translationsSingularOnly} et {@code includesPluralGameForms} (Longoka).
   */
  private static void finalizePackBookMeta(
      Map<String, Object> packMeta,
      String packTitle,
      String packDescription,
      String packId,
      int puzzleCount,
      EditionTier editionTier,
      String meaningLanguage) {

    @SuppressWarnings("unchecked")
    Map<String, Object> book = (Map<String, Object>) packMeta.get("book");
    if (book == null) {
      return;
    }
    String ml = meaningLanguage == null ? "fr" : meaningLanguage.trim().toLowerCase(Locale.ROOT);
    boolean english = ml.startsWith("en");
    book.put("meaningLanguage", english ? "en" : "fr");
    book.put("title", packTitle != null ? packTitle : "");
    book.put("subtitle", stationerySubtitleFor(metaToCombination(packMeta), editionTier, meaningLanguage));
    book.put("description", packDescription != null ? packDescription : "");
    book.put("pages", estimateBookPageCount(puzzleCount));
    book.put("trimSize", "6x9in");
    book.put("printVariant", "pro-softcover");
    book.put("binding", "perfect-bound");

    Map<String, Object> stationery = new LinkedHashMap<>();
    stationery.put("name", packTitle);
    stationery.put("subtitle", book.get("subtitle"));
    stationery.put("description", packDescription);
    stationery.put("publisher", book.get("publisher"));
    stationery.put("contentLanguage", packMeta.get("language"));
    stationery.put("meaningLanguage", book.get("meaningLanguage"));
    stationery.put("format", "print:6x9in");
    stationery.put("pageCount", book.get("pages"));
    stationery.put("brand", "Longoka Games");
    stationery.put("requiresShipping", 1);
    stationery.put("isDigital", 0);
    stationery.put("categorySlug", "games");
    stationery.put("thumbnailStrategy", "generated-svg-cover");
    stationery.put("skuHint", packMeta.get("bookCode"));
    stationery.put("bookCode", packMeta.get("bookCode"));
    stationery.put("publicationStart", book.get("publicationStart"));
    stationery.put("publicationEnd", book.get("publicationEnd"));
    stationery.put("archiveAfter", book.get("archiveAfter"));
    packMeta.put("stationery", stationery);

    Map<String, Object> editorial = new LinkedHashMap<>();
    String bookCode = String.valueOf(packMeta.getOrDefault("bookCode", ""));
    editorial.put("bookKey", bookCode);
    editorial.put("title", packTitle);
    editorial.put("summary", packDescription);
    editorial.put("language", packMeta.get("language"));
    editorial.put("meaningLanguage", book.get("meaningLanguage"));
    editorial.put("editionTier", packMeta.get("editionTier"));
    editorial.put("editionLabel", packMeta.get("editionLabel"));
    editorial.put("editorialStatus", "ready");
    editorial.put("catalogVisibility", "private");
    editorial.put("sourceType", "game_pack");
    editorial.put("sourceKey", packId);
    editorial.put("stationeryCategorySlug", "games");
    editorial.put(
      "variantBlueprints",
      List.of(
        Map.of(
          "kind", "physical",
          "variantKey", bookCode + ":physical",
          "skuHint", bookCode + "-PRINT",
          "requiresShipping", 1,
          "isDigital", 0),
        Map.of(
          "kind", "pdf",
          "variantKey", bookCode + ":pdf",
          "skuHint", bookCode + "-PDF",
          "requiresShipping", 0,
          "isDigital", 1)));
    packMeta.put("editorial", editorial);

    packMeta.put("translationsSingularOnly", PackMeaningMeta.defaultTranslationsSingularOnly());
    packMeta.put(
        "includesPluralGameForms",
        PackMeaningMeta.resolveIncludesPluralGameForms(
            null,
            stringMeta(packMeta.get("numberPolicy")),
            stringMeta(packMeta.get("morphologyProfile")),
            stringMeta(packMeta.get("relationType")),
            stringMeta(packMeta.get("lexicalProfile")),
            firstNonBlank(stringMeta(packMeta.get("publicProfileId")), stringMeta(packMeta.get("profileId"))),
            stringMeta(packMeta.get("profileLabel")),
            packTitle,
            packDescription,
            packId));
  }

  private static String stringMeta(Object value) {
    return value == null ? null : String.valueOf(value);
  }

  private static CombinationProfile metaToCombination(Map<String, Object> packMeta) {
    String morphologyProfile = stringMeta(packMeta.get("morphologyProfile"));
    String lexicalProfile = stringMeta(packMeta.get("lexicalProfile"));
    String numberPolicy = stringMeta(packMeta.get("numberPolicy"));
    Integer nominalClassId = packMeta.get("nominalClassId") instanceof Number
        ? ((Number) packMeta.get("nominalClassId")).intValue()
        : null;
    List<Integer> nominalClassIds = new ArrayList<>();
    Object nominalClassIdsValue = packMeta.get("nominalClassIds");
    if (nominalClassIdsValue instanceof List<?>) {
      for (Object value : (List<?>) nominalClassIdsValue) {
        if (value instanceof Number) {
          nominalClassIds.add(((Number) value).intValue());
        }
      }
    }
    if (nominalClassIds.isEmpty() && nominalClassId != null) {
      nominalClassIds.add(nominalClassId);
    }
    String nominalClassName = stringMeta(packMeta.get("nominalClassName"));
    String radical = stringMeta(packMeta.get("radical"));

    MorphologyProfileMode morphology = MorphologyProfileMode.GENERAL;
    if ("nominal-class".equalsIgnoreCase(morphologyProfile)) {
      morphology = MorphologyProfileMode.NOMINAL_CLASS;
    } else if ("radical-nouns".equalsIgnoreCase(morphologyProfile)) {
      morphology = MorphologyProfileMode.RADICAL_NOUNS;
    } else if ("radical-verbs".equalsIgnoreCase(morphologyProfile)) {
      morphology = MorphologyProfileMode.RADICAL_VERBS;
    }

    NumberMode numberMode = NumberMode.SINGULAR_OR_PLURAL;
    if ("plural-only".equalsIgnoreCase(numberPolicy)) {
      numberMode = NumberMode.PLURAL_ONLY;
    } else if ("singular-only".equalsIgnoreCase(numberPolicy)) {
      numberMode = NumberMode.SINGULAR_ONLY;
    }

    boolean includeVerbs = "verbs".equalsIgnoreCase(lexicalProfile) || "mixed".equalsIgnoreCase(lexicalProfile);
    boolean includeNouns = "nouns".equalsIgnoreCase(lexicalProfile) || "mixed".equalsIgnoreCase(lexicalProfile) || morphology == MorphologyProfileMode.NOMINAL_CLASS || morphology == MorphologyProfileMode.RADICAL_NOUNS;

    return new CombinationProfile(
      firstNonBlank(stringMeta(packMeta.get("publicProfileId")), stringMeta(packMeta.get("profileId"))),
        includeNouns,
        includeVerbs,
        numberMode,
        lexicalProfile,
        morphology,
        nominalClassId,
          nominalClassIds,
        nominalClassName,
        radical);
  }

  private static int estimateBookPageCount(int puzzleCount) {
    if (puzzleCount <= 0) {
      return 12;
    }
    int frontMatter = 6;
    int puzzlePages = puzzleCount * 2;
    int solutionPages = Math.max(2, (int) Math.ceil(puzzleCount / 2.0));
    return Math.min(600, frontMatter + puzzlePages + solutionPages);
  }

  /**
   * ISBN-13 fictif (mais checksum valide) deterministe a partir d'un identifiant editorial.
   * Prefixe 979 (usage courant), puis 9 chiffres derives, puis checksum.
   */
  private static String buildFictionalIsbn13(String seed) {
    String base = seed == null ? "" : seed;
    int hash = Math.abs(base.hashCode());
    // 9 chiffres (padding) -> total 12 chiffres avec prefixe 979.
    String nine = String.format(Locale.ROOT, "%09d", hash % 1_000_000_000);
    String twelve = "979" + nine;
    int sum = 0;
    for (int i = 0; i < twelve.length(); i++) {
      int digit = twelve.charAt(i) - '0';
      sum += (i % 2 == 0) ? digit : (digit * 3);
    }
    int check = (10 - (sum % 10)) % 10;
    return twelve + check;
  }

  private static void enrichWordsearchMeta(
      Map<String, Object> meta,
      List<WordSearchJsonModels.EntryV1> entries,
      int rows,
      int cols) {

    Set<String> nominalClasses = new LinkedHashSet<>();
    Set<String> semanticDomains = new LinkedHashSet<>();
    int nounCount = 0;
    int verbCount = 0;

    for (WordSearchJsonModels.EntryV1 e : entries) {
      if ("noun".equalsIgnoreCase(e.partOfSpeech)) {
        nounCount++;
      } else if ("verb".equalsIgnoreCase(e.partOfSpeech)) {
        verbCount++;
      }
      if (e.extraInfo != null && !e.extraInfo.isBlank()) {
        nominalClasses.add(e.extraInfo);
      }
      if (e.semanticTags != null) {
        semanticDomains.addAll(e.semanticTags);
      }
    }

    meta.put("entryCount", entries.size());
    meta.put("placedEntries", entries.size());
    meta.put("nounCount", nounCount);
    meta.put("verbCount", verbCount);
    meta.put("filledCells", rows * cols);
    meta.put("fillRatio", 1.0d);
    if (!nominalClasses.isEmpty()) {
      meta.put("nominalClasses", new ArrayList<>(nominalClasses));
    }
    if (!semanticDomains.isEmpty()) {
      meta.put("semanticDomains", new ArrayList<>(semanticDomains));
    }
  }

  private static void enrichCrosswordMeta(
      Map<String, Object> meta,
      List<CrosswordJsonModels.EntryV1> entries,
      Set<String> nominalClasses,
      Set<String> semanticDomains) {

    int nounCount = 0;
    int verbCount = 0;
    for (CrosswordJsonModels.EntryV1 e : entries) {
      if ("noun".equalsIgnoreCase(e.partOfSpeech)) {
        nounCount++;
      } else if ("verb".equalsIgnoreCase(e.partOfSpeech)) {
        verbCount++;
      }
    }

    meta.put("entryCount", entries.size());
    meta.put("nounCount", nounCount);
    meta.put("verbCount", verbCount);
    if (!nominalClasses.isEmpty()) {
      meta.put("nominalClasses", new ArrayList<>(nominalClasses));
    }
    if (!semanticDomains.isEmpty()) {
      meta.put("semanticDomains", new ArrayList<>(semanticDomains));
    }
  }

  private static boolean supportsMorphoDomino(CombinationProfile combination) {
    return combination != null
        && combination.morphologyProfile != null
        && combination.morphologyProfile != MorphologyProfileMode.GENERAL;
  }

  private static int normalizeDominoTileTarget(int targetTileCount) {
    int safe = targetTileCount > 0 ? targetTileCount : DEFAULT_MAX_ENTRIES;
    if ((safe % 2) != 0) {
      safe -= 1;
    }
    return Math.max(6, safe);
  }

  private static int normalizeMemoryPairTarget(int targetPairCount) {
    int safe = targetPairCount > 0 ? targetPairCount : DEFAULT_MAX_ENTRIES;
    return Math.max(4, safe);
  }

  private static String dominoRelationType(CombinationProfile combination) {
    if (combination.morphologyProfile == MorphologyProfileMode.NOMINAL_CLASS) {
      return "class-membership";
    }
    return "radical-family";
  }

  private static String dominoAnchorKind(CombinationProfile combination) {
    return combination.morphologyProfile == MorphologyProfileMode.NOMINAL_CLASS ? "class" : "radical";
  }

  private static String dominoAnchorValue(CombinationProfile combination) {
    if (combination.morphologyProfile == MorphologyProfileMode.NOMINAL_CLASS) {
      if (combination.nominalClassIds != null && combination.nominalClassIds.size() > 1) {
        return joinClassIds(combination.nominalClassIds, "+");
      }
      return String.valueOf(combination.nominalClassId);
    }
    return blankToNull(combination.radical) != null ? combination.radical : profileLabel(combination);
  }

  private static String dominoAnchorDisplay(CombinationProfile combination) {
    if (combination.morphologyProfile == MorphologyProfileMode.NOMINAL_CLASS) {
      return nominalClassLabel(combination);
    }
    return dominoAnchorLabel(combination) + " " + displayRadical(combination.radical);
  }

  private static String dominoAnchorNormalized(CombinationProfile combination) {
    if (combination.morphologyProfile == MorphologyProfileMode.NOMINAL_CLASS) {
      if (combination.nominalClassIds != null && combination.nominalClassIds.size() > 1) {
        return "CLASS_" + joinClassIds(combination.nominalClassIds, "_");
      }
      return "CLASS_" + combination.nominalClassId;
    }
    return "RADICAL_" + normalizeDominoValue(combination.radical);
  }

  private static String dominoAnchorLabel(CombinationProfile combination) {
    switch (combination.morphologyProfile) {
      case NOMINAL_CLASS:
        return "Classe nominale";
      case RADICAL_VERBS:
        return "Lettres communes";
      case RADICAL_NOUNS:
      default:
        return "Lettres communes";
    }
  }

  private static String dominoFormLabel(CombinationProfile combination) {
    if (combination.includeVerbs) {
      return "Verbe";
    }
    return combination.nounNumberMode == NumberMode.PLURAL_ONLY ? "Forme plurielle" : "Forme singuliere";
  }

  private static String memoryRelationType(CombinationProfile combination) {
    if (combination.morphologyProfile == MorphologyProfileMode.NOMINAL_CLASS) {
      return "class-filtered-form-translation";
    }
    if (combination.morphologyProfile == MorphologyProfileMode.RADICAL_NOUNS
        || combination.morphologyProfile == MorphologyProfileMode.RADICAL_VERBS) {
      return "shared-letters-form-translation";
    }
    return "form-translation";
  }

  private static String memoryFormLabel(CombinationProfile combination) {
    if (combination.includeVerbs && !combination.includeNouns) {
      return "Verbe";
    }
    if (combination.includeNouns && combination.includeVerbs) {
      return "Mot";
    }
    return combination.nounNumberMode == NumberMode.PLURAL_ONLY ? "Forme plurielle" : "Forme singuliere";
  }

  private static String memoryTranslationLabel(String meaningLanguage) {
    String lang = blankToNull(meaningLanguage);
    if (lang == null) {
      return "Sens";
    }
    if ("fr".equalsIgnoreCase(lang)) {
      return "Sens FR";
    }
    if ("en".equalsIgnoreCase(lang)) {
      return "Meaning EN";
    }
    return "Sens " + lang.toUpperCase(Locale.ROOT);
  }

  private static String memoryExtraInfo(CombinationProfile combination) {
    if (combination.morphologyProfile == MorphologyProfileMode.NOMINAL_CLASS) {
      return nominalClassLabel(combination);
    }
    if (blankToNull(combination.radical) != null) {
      return sharedLettersBadge(combination);
    }
    return null;
  }

  private static String scrabbleRelationType(CombinationProfile combination) {
    if (combination.morphologyProfile == MorphologyProfileMode.NOMINAL_CLASS) {
      return "class-filtered-word-build";
    }
    if (combination.morphologyProfile == MorphologyProfileMode.RADICAL_NOUNS
        || combination.morphologyProfile == MorphologyProfileMode.RADICAL_VERBS) {
      return "shared-letters-word-build";
    }
    return "word-build";
  }

  private static String scrabbleChallengeLabel(CombinationProfile combination, String partOfSpeech) {
    if ("verb".equalsIgnoreCase(partOfSpeech)) {
      return "Verbe";
    }
    if ("noun".equalsIgnoreCase(partOfSpeech)) {
      if (combination.nounNumberMode == NumberMode.PLURAL_ONLY) {
        return "Forme plurielle";
      }
      if (combination.nounNumberMode == NumberMode.SINGULAR_ONLY) {
        return "Forme singuliere";
      }
      return "Nom";
    }
    if (combination.includeNouns && combination.includeVerbs) {
      return "Mot";
    }
    return memoryFormLabel(combination);
  }

  private static String anagramRelationType(CombinationProfile combination) {
    if (combination.morphologyProfile == MorphologyProfileMode.NOMINAL_CLASS) {
      return "class-filtered-segment-anagram";
    }
    if (combination.morphologyProfile == MorphologyProfileMode.RADICAL_NOUNS
        || combination.morphologyProfile == MorphologyProfileMode.RADICAL_VERBS) {
      return "shared-letters-segment-anagram";
    }
    return "segment-anagram";
  }

  private static String anagramChallengeLabel(CombinationProfile combination, String partOfSpeech) {
    if ("verb".equalsIgnoreCase(partOfSpeech)) {
      return "Segments du verbe";
    }
    if ("noun".equalsIgnoreCase(partOfSpeech)) {
      if (combination.nounNumberMode == NumberMode.PLURAL_ONLY) {
        return "Segments du pluriel";
      }
      if (combination.nounNumberMode == NumberMode.SINGULAR_ONLY) {
        return "Segments du singulier";
      }
      return "Segments du nom";
    }
    if (combination.includeNouns && combination.includeVerbs) {
      return "Segments du mot";
    }
    return "Segments";
  }

  private static String languageLabel(LanguageProfile language) {
    return language.code.equals("kg") ? "Kikongo" : "Lingala";
  }

  private static String puzzleTypeLabel(String puzzleType) {
    if ("crossword".equals(puzzleType)) {
      return "Mots croisés";
    }
    if ("arrowword".equals(puzzleType)) {
      return "Mots fléchés";
    }
    if ("domino".equals(puzzleType)) {
      return "Dominos morphologiques";
    }
    if ("memory".equals(puzzleType)) {
      return "Memory match";
    }
    if ("scrabble".equals(puzzleType)) {
      return "Scrabble-like";
    }
    if ("anagram".equals(puzzleType)) {
      return "Anagrammes morphologiques";
    }
    return "Mots mêlés";
  }

  private static String puzzleTypeLabelEnglish(String puzzleType) {
    if ("crossword".equals(puzzleType)) {
      return "Crosswords";
    }
    if ("arrowword".equals(puzzleType)) {
      return "Arrowwords";
    }
    if ("domino".equals(puzzleType)) {
      return "Morphological dominoes";
    }
    if ("memory".equals(puzzleType)) {
      return "Memory match";
    }
    if ("scrabble".equals(puzzleType)) {
      return "Scrabble-like";
    }
    if ("anagram".equals(puzzleType)) {
      return "Morphological anagrams";
    }
    return "Word searches";
  }

  private static String profileLabelEnglish(CombinationProfile combination) {
    if (combination.morphologyProfile == MorphologyProfileMode.NOMINAL_CLASS) {
      return nominalClassLabelEnglish(combination) + " - " + numberPolicyLabelEnglish(combination.nounNumberMode);
    }
    if (combination.morphologyProfile == MorphologyProfileMode.RADICAL_NOUNS) {
      return capitalize(nounProfileLabelEnglish(combination.nounNumberMode))
          + " sharing "
          + sharedLettersDescriptor(combination)
          + " with "
          + displayRadical(combination.radical);
    }
    if (combination.morphologyProfile == MorphologyProfileMode.RADICAL_VERBS) {
      return "Verbs sharing "
          + sharedLettersDescriptor(combination)
          + " with "
          + displayRadical(combination.radical);
    }
    if (combination.includeNouns && combination.includeVerbs) {
      return "Verbs + " + nounProfileLabelEnglish(combination.nounNumberMode);
    }
    if (combination.includeVerbs) {
      return "Verbs";
    }
    return nounProfileLabelEnglish(combination.nounNumberMode);
  }

  private static String nominalClassLabelEnglish(CombinationProfile combination) {
    String label = blankToNull(combination.nominalClassName);
    boolean usableLabel = label != null && !isGenericNominalClassLabel(label, combination);
    if (combination.nominalClassIds != null && combination.nominalClassIds.size() > 1) {
      return usableLabel ? "Nominal classes " + label : "Targeted nominal classes";
    }
    if (!usableLabel) {
      return "Targeted nominal class";
    }
    return "Nominal class " + label;
  }

  private static String numberPolicyLabelEnglish(NumberMode mode) {
    switch (mode) {
      case SINGULAR_ONLY:
        return "singular";
      case PLURAL_ONLY:
        return "plural";
      case SINGULAR_OR_PLURAL:
      default:
        return "singular or plural";
    }
  }

  private static String nounProfileLabelEnglish(NumberMode numberMode) {
    switch (numberMode) {
      case SINGULAR_ONLY:
        return "singular nouns";
      case PLURAL_ONLY:
        return "plural nouns";
      case SINGULAR_OR_PLURAL:
      default:
        return "singular or plural nouns";
    }
  }

  private static String storefrontProfileLabelEnglish(CombinationProfile combination) {
    if (combination.morphologyProfile == MorphologyProfileMode.NOMINAL_CLASS) {
      if (combination.nounNumberMode == NumberMode.PLURAL_ONLY) {
        return "plural nominal classes";
      }
      if (combination.nounNumberMode == NumberMode.SINGULAR_ONLY) {
        return "singular nominal classes";
      }
      return "nominal classes";
    }
    if (combination.morphologyProfile == MorphologyProfileMode.RADICAL_NOUNS) {
      return nounProfileLabelEnglish(combination.nounNumberMode)
          + " sharing "
          + sharedLettersDescriptor(combination)
          + " with "
          + displayRadical(combination.radical).toLowerCase(Locale.ROOT);
    }
    if (combination.morphologyProfile == MorphologyProfileMode.RADICAL_VERBS) {
      return "verbs sharing "
          + sharedLettersDescriptor(combination)
          + " with "
          + displayRadical(combination.radical).toLowerCase(Locale.ROOT);
    }
    if (combination.includeNouns && combination.includeVerbs) {
      return "nouns and verbs";
    }
    if (combination.includeVerbs) {
      return "verbs";
    }
    if (combination.nounNumberMode == NumberMode.PLURAL_ONLY) {
      return "plural nouns";
    }
    if (combination.nounNumberMode == NumberMode.SINGULAR_ONLY) {
      return "nouns";
    }
    return "nouns";
  }

  private static String storefrontProfileObjectEnglish(CombinationProfile combination) {
    if (combination.morphologyProfile == MorphologyProfileMode.NOMINAL_CLASS) {
      if (combination.nounNumberMode == NumberMode.PLURAL_ONLY) {
        return "plural nominal classes";
      }
      if (combination.nounNumberMode == NumberMode.SINGULAR_ONLY) {
        return "singular nominal classes";
      }
      return "nominal classes";
    }
    if (combination.morphologyProfile == MorphologyProfileMode.RADICAL_NOUNS) {
      return nounProfileLabelEnglish(combination.nounNumberMode)
          + " sharing "
          + sharedLettersDescriptor(combination)
          + " with "
          + displayRadical(combination.radical).toLowerCase(Locale.ROOT);
    }
    if (combination.morphologyProfile == MorphologyProfileMode.RADICAL_VERBS) {
      return "verbs sharing "
          + sharedLettersDescriptor(combination)
          + " with "
          + displayRadical(combination.radical).toLowerCase(Locale.ROOT);
    }
    if (combination.includeNouns && combination.includeVerbs) {
      return "nouns and verbs";
    }
    if (combination.includeVerbs) {
      return "verbs";
    }
    if (combination.nounNumberMode == NumberMode.PLURAL_ONLY) {
      return "plural nouns";
    }
    return "nouns";
  }

  private static String puzzleCountLabelEnglish(String puzzleType, int puzzleCount) {
    int safeCount = Math.max(0, puzzleCount);
    switch (puzzleType.toLowerCase(Locale.ROOT)) {
      case "crossword":
        return countedLabel(safeCount, "crossword grid", "crossword grids");
      case "wordsearch":
        return countedLabel(safeCount, "word-search grid", "word-search grids");
      case "arrowword":
        return countedLabel(safeCount, "arrowword grid", "arrowword grids");
      case "domino":
        return countedLabel(safeCount, "domino game", "domino games");
      case "memory":
        return countedLabel(safeCount, "memory session", "memory sessions");
      case "scrabble":
        return countedLabel(safeCount, "letter game", "letter games");
      case "anagram":
        return countedLabel(safeCount, "anagram game", "anagram games");
      default:
        return countedLabel(safeCount, "language game", "language games");
    }
  }

  private static String difficultyStorefrontLabelEnglish(String difficulty) {
    String raw = difficulty == null ? "" : difficulty.trim().toLowerCase(Locale.ROOT);
    if (raw.isEmpty()) {
      return "";
    }
    if (raw.equals("easy") || raw.equals("debutant")) {
      return "accessible";
    }
    if (raw.equals("medium") || raw.equals("intermediate")) {
      return "balanced";
    }
    if (raw.equals("hard") || raw.equals("expert")) {
      return "challenging";
    }
    return raw;
  }

  private static String profileLabel(CombinationProfile combination) {
    if (combination.morphologyProfile == MorphologyProfileMode.NOMINAL_CLASS) {
      return nominalClassLabel(combination) + " - " + numberPolicyLabel(combination.nounNumberMode);
    }
    if (combination.morphologyProfile == MorphologyProfileMode.RADICAL_NOUNS) {
      return capitalize(nounProfileLabel(combination.nounNumberMode))
          + " partageant "
          + sharedLettersDescriptor(combination)
          + " avec "
          + displayRadical(combination.radical);
    }
    if (combination.morphologyProfile == MorphologyProfileMode.RADICAL_VERBS) {
      return "Verbes partageant "
          + sharedLettersDescriptor(combination)
          + " avec "
          + displayRadical(combination.radical);
    }
    if (combination.includeNouns && combination.includeVerbs) {
      return "Verbes + " + nounProfileLabel(combination.nounNumberMode);
    }
    if (combination.includeVerbs) {
      return "Verbes";
    }
    return nounProfileLabel(combination.nounNumberMode);
  }

  private static String nounProfileLabel(NumberMode numberMode) {
    switch (numberMode) {
      case SINGULAR_ONLY:
        return "noms singuliers";
      case PLURAL_ONLY:
        return "noms pluriels";
      case SINGULAR_OR_PLURAL:
      default:
        return "noms singuliers ou pluriels";
    }
  }

  private static String publicNumberModeToken(NumberMode mode) {
    switch (mode) {
      case SINGULAR_ONLY:
        return "singular";
      case PLURAL_ONLY:
        return "plural";
      case SINGULAR_OR_PLURAL:
      default:
        return "sing-or-plur";
    }
  }

  private static String stableOpaqueProfileSuffix(String raw) {
    String value = blankToNull(raw);
    if (value == null) {
      return "profile";
    }
    return Integer.toUnsignedString(value.hashCode(), 36);
  }

  private static String publicProfileToken(CombinationProfile combination) {
    if (combination == null) {
      return "profile";
    }
    if (combination.morphologyProfile == MorphologyProfileMode.NOMINAL_CLASS) {
      String label = blankToNull(combination.nominalClassName);
      if (label != null && !isGenericNominalClassLabel(label, combination)) {
        return "nominal-class-" + slugToken(label) + "-" + publicNumberModeToken(combination.nounNumberMode);
      }
      String scope = combination.nominalClassIds != null && combination.nominalClassIds.size() > 1
          ? "group"
          : "targeted";
      return "nominal-class-"
          + scope
          + "-"
          + publicNumberModeToken(combination.nounNumberMode)
          + "-"
          + stableOpaqueProfileSuffix(combination.id);
    }
    return slugToken(combination.id);
  }

  private static boolean isGenericNominalClassLabel(String label, CombinationProfile combination) {
    String normalized = blankToNull(label == null ? null : label.trim().toLowerCase(Locale.ROOT));
    if (normalized == null) {
      return true;
    }
    if (combination.nominalClassId != null && normalized.equals("class-" + combination.nominalClassId)) {
      return true;
    }
    return normalized.matches("class[-_ ]?\\d+");
  }

  private static String nominalClassLabel(CombinationProfile combination) {
    String label = blankToNull(combination.nominalClassName);
    boolean usableLabel = label != null && !isGenericNominalClassLabel(label, combination);
    if (combination.nominalClassIds != null && combination.nominalClassIds.size() > 1) {
      return usableLabel ? "Classes nominales " + label : "Classes nominales ciblées";
    }
    if (!usableLabel) {
      return "Classe nominale ciblée";
    }
    return "Classe nominale " + label;
  }

  private static String numberPolicyLabel(NumberMode mode) {
    switch (mode) {
      case SINGULAR_ONLY:
        return "singulier";
      case PLURAL_ONLY:
        return "pluriel";
      case SINGULAR_OR_PLURAL:
      default:
        return "singulier ou pluriel";
    }
  }

  private static String numberPolicyKey(NumberMode mode) {
    switch (mode) {
      case SINGULAR_ONLY:
        return "singular-only";
      case PLURAL_ONLY:
        return "plural-only";
      case SINGULAR_OR_PLURAL:
      default:
        return "singular-or-plural";
    }
  }

  private static String morphologyProfileKey(MorphologyProfileMode mode) {
    switch (mode) {
      case NOMINAL_CLASS:
        return "nominal-class";
      case RADICAL_NOUNS:
        return "radical-nouns";
      case RADICAL_VERBS:
        return "radical-verbs";
      case GENERAL:
      default:
        return "general";
    }
  }

  private static String displayRadical(String radical) {
    String value = blankToNull(radical);
    if (value == null) {
      return "RADICAL";
    }
    return value.toUpperCase(Locale.ROOT);
  }

  private static String normalizeRadicalLabel(String radical) {
    return blankToNull(radical == null ? null : radical.trim());
  }

  private static String nounSharedLetterSource(LexWord word, String selectedForm) {
    return firstNonBlank(word.getRoot(), selectedForm, word.getSingular(), word.getPlural());
  }

  private static boolean matchesSharedLetterProfile(String candidateValue, CombinationProfile combination) {
    if (combination == null || blankToNull(combination.radical) == null) {
      return true;
    }
    int required = sharedLetterTarget(combination);
    if (required <= 0) {
      return true;
    }
    return countSharedLetters(candidateValue, combination.radical) >= required;
  }

  private static int sharedLetterTarget(CombinationProfile combination) {
    String radical = combination == null ? null : blankToNull(combination.radical);
    if (radical == null) {
      return 0;
    }
    int uniqueCount = normalizedLetterSet(radical).size();
    if (uniqueCount <= 2) {
      return uniqueCount;
    }
    List<Integer> options = new ArrayList<>();
    options.add(2);
    if (uniqueCount >= 3) {
      options.add(3);
    }
    if (!options.contains(uniqueCount)) {
      options.add(uniqueCount);
    }
    int index = Math.floorMod(radical.toUpperCase(Locale.ROOT).hashCode(), options.size());
    return options.get(index);
  }

  private static String sharedLettersDescriptor(CombinationProfile combination) {
    int required = sharedLetterTarget(combination);
    int uniqueCount = normalizedLetterSet(combination == null ? null : combination.radical).size();
    if (required <= 0) {
      return "des lettres";
    }
    if (uniqueCount > 0 && required >= uniqueCount) {
      return "toutes les lettres";
    }
    return required + (required > 1 ? " lettres" : " lettre");
  }

  private static String sharedLettersBadge(CombinationProfile combination) {
    if (combination == null || blankToNull(combination.radical) == null) {
      return null;
    }
    return sharedLettersDescriptor(combination) + " avec " + displayRadical(combination.radical);
  }

  private static int countSharedLetters(String left, String right) {
    Set<Character> leftLetters = normalizedLetterSet(left);
    Set<Character> rightLetters = normalizedLetterSet(right);
    int shared = 0;
    for (Character letter : leftLetters) {
      if (rightLetters.contains(letter)) {
        shared += 1;
      }
    }
    return shared;
  }

  private static Set<Character> normalizedLetterSet(String value) {
    Set<Character> letters = new LinkedHashSet<>();
    String normalized = normalizeGridWord(value);
    for (int i = 0; i < normalized.length(); i++) {
      letters.add(normalized.charAt(i));
    }
    return letters;
  }

  private static String capitalize(String value) {
    String raw = blankToNull(value);
    if (raw == null) {
      return "";
    }
    return raw.substring(0, 1).toUpperCase(Locale.ROOT) + raw.substring(1);
  }

  private static String slugToken(String value) {
    String base = normalizeRadicalLabel(value);
    if (base == null) {
      return "profile";
    }
    String folded = Normalizer.normalize(base, Normalizer.Form.NFD)
        .replaceAll("\\p{M}+", "")
        .toLowerCase(Locale.ROOT)
        .replaceAll("[^a-z0-9]+", "-")
        .replaceAll("(^-+|-+$)", "");
    return folded.isBlank() ? "profile" : folded;
  }

  private static String normalizeGridWord(String baseForm) {
    if (baseForm == null) {
      return "";
    }

    String folded = Normalizer.normalize(baseForm, Normalizer.Form.NFD)
        .replaceAll("\\p{M}+", "")
        .toUpperCase(Locale.ROOT);

    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < folded.length(); i++) {
      char ch = folded.charAt(i);
      if (ch >= 'A' && ch <= 'Z') {
        sb.append(ch);
      }
    }
    return sb.toString();
  }

  private static String normalizeDominoValue(String value) {
    if (value == null) {
      return "";
    }

    String folded = Normalizer.normalize(value, Normalizer.Form.NFD)
        .replaceAll("\\p{M}+", "")
        .toUpperCase(Locale.ROOT);

    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < folded.length(); i++) {
      char ch = folded.charAt(i);
      if ((ch >= 'A' && ch <= 'Z') || (ch >= '0' && ch <= '9')) {
        sb.append(ch);
      }
    }
    return sb.toString();
  }

  private static String normalizeMemoryValue(String value) {
    return normalizeDominoValue(value);
  }

  private static Connection openLexConnection(String languageCode) throws Exception {
    return "kg".equalsIgnoreCase(languageCode)
        ? DbConfig.openKikongoLexConnection()
        : DbConfig.openLingalaLexConnection();
  }

  private static String firstMeaning(List<LexMeaning> meanings) {
    if (meanings == null || meanings.isEmpty()) {
      return null;
    }
    for (LexMeaning m : meanings) {
      if (m == null) {
        continue;
      }
      String text = blankToNull(m.getMeaning());
      if (text != null) {
        return text;
      }
    }
    return null;
  }

  private static String firstNonBlank(String... candidates) {
    if (candidates == null) {
      return null;
    }
    for (String c : candidates) {
      String value = blankToNull(c);
      if (value != null) {
        return value;
      }
    }
    return null;
  }

  private static String blankToNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value;
  }

  private static int parseIntArg(String[] args, String key, int defaultValue) {
    String raw = parseStringArg(args, key, null);
    if (raw == null) {
      return defaultValue;
    }
    try {
      return Integer.parseInt(raw);
    } catch (NumberFormatException ex) {
      return defaultValue;
    }
  }

  private static String parseStringArg(String[] args, String key, String defaultValue) {
    if (args == null || args.length == 0) {
      return defaultValue;
    }
    for (int i = 0; i < args.length - 1; i++) {
      if (key.equalsIgnoreCase(args[i])) {
        return args[i + 1];
      }
    }
    return defaultValue;
  }

  private static Set<String> parseProfileIdsArg(String[] args, String key) {
    String raw = parseStringArg(args, key, null);
    if (raw == null || raw.isBlank()) {
      return Set.of();
    }
    LinkedHashSet<String> profileIds = new LinkedHashSet<>();
    String normalized = raw.replace(';', ',').replace('/', ',');
    String[] tokens = normalized.split("[,\\s]+");
    for (String token : tokens) {
      String value = token == null ? "" : token.trim();
      if (!value.isEmpty()) {
        profileIds.add(value);
      }
    }
    return profileIds;
  }

  private static String joinClassIds(List<Integer> classIds, String separator) {
    if (classIds == null || classIds.isEmpty()) {
      return "";
    }
    StringBuilder out = new StringBuilder();
    for (int i = 0; i < classIds.size(); i++) {
      if (i > 0) {
        out.append(separator);
      }
      out.append(classIds.get(i));
    }
    return out.toString();
  }

  private static List<LanguageProfile> parseLanguageProfiles(String raw) {
    LinkedHashSet<LanguageProfile> selected = new LinkedHashSet<>();
    String value = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);

    if (value.isBlank()) {
      Collections.addAll(selected, LanguageProfile.values());
      return new ArrayList<>(selected);
    }

    String normalized = value.replace(';', ',').replace('/', ',');
    String[] tokens = normalized.split("[,\\s]+");
    for (String token : tokens) {
      String item = token == null ? "" : token.trim().toLowerCase(Locale.ROOT);
      if (item.isBlank()) {
        continue;
      }
      if ("both".equals(item) || "all".equals(item) || "tous".equals(item) || "toutes".equals(item)) {
        Collections.addAll(selected, LanguageProfile.values());
        return new ArrayList<>(selected);
      }
      if ("kg".equals(item) || "kikongo".equals(item) || "lexikongo".equals(item)) {
        selected.add(LanguageProfile.KG);
        continue;
      }
      if ("ln".equals(item) || "lingala".equals(item) || "lexilingala".equals(item)) {
        selected.add(LanguageProfile.LN);
      }
    }

    if (selected.isEmpty()) {
      Collections.addAll(selected, LanguageProfile.values());
    }
    return new ArrayList<>(selected);
  }

  private static String formatLanguageSelection(List<LanguageProfile> languages) {
    List<String> codes = new ArrayList<>();
    if (languages != null) {
      for (LanguageProfile language : languages) {
        if (language != null) {
          codes.add(language.code);
        }
      }
    }
    if (codes.isEmpty()) {
      return DEFAULT_LANGUAGE_MODE;
    }
    return String.join(",", codes);
  }

  private static EditionTier parseEditionTier(String raw) {
    if (raw == null) {
      return EditionTier.STANDARD;
    }

    String value = raw.trim().toLowerCase(Locale.ROOT);
    if ("mulongoki".equals(value) || "standard".equals(value)) {
      return EditionTier.STANDARD;
    }
    if ("premium".equals(value)
      || "pro".equals(value)
        || "expert".equals(value)
        || "haut-de-gamme".equals(value)
        || "hautdegamme".equals(value)
        || "high-end".equals(value)
        || "highend".equals(value)) {
      return EditionTier.PREMIUM;
    }
    return EditionTier.STANDARD;
  }

  private static String resolveDifficulty(String raw, EditionTier editionTier) {
    String value = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
    if (value.isBlank() || "auto".equals(value) || "mulongoki".equals(value)) {
      return editionTier == EditionTier.PREMIUM ? "expert" : "easy";
    }
    if ("facile".equals(value) || "easy".equals(value)) {
      return "easy";
    }
    if ("intermediate".equals(value) || "intermédiaire".equals(value) || "intermediaire".equals(value)
        || "normal".equals(value) || "medium".equals(value)) {
      return "intermediate";
    }
    if ("hard".equals(value) || "difficile".equals(value)) {
      return "hard";
    }
    if ("expert".equals(value) || "premium".equals(value) || "pro".equals(value)) {
      return "expert";
    }
    return editionTier == EditionTier.PREMIUM ? "expert" : "easy";
  }

  private static String editionTierKey(EditionTier editionTier) {
    return "mulongoki";
  }

  private static String editionLabel(EditionTier editionTier) {
    return "Mulongoki";
  }

  private static String storefrontEditionDescriptor(EditionTier editionTier) {
    return "Mulongoki";
  }

  private static String buildProvisionalId(
      LanguageProfile language,
      CombinationProfile combination,
      String gameType,
      EditionTier editionTier) {

    return "LG-"
        + language.code.toUpperCase(Locale.ROOT)
        + "-"
        + gameTypeCode(gameType)
        + "-"
        + publicProfileToken(combination).toUpperCase(Locale.ROOT)
        + "-"
      + "MULONGOKI"
        + "-V1";
  }

  private static String gameTypeCode(String gameType) {
    if ("crossword".equalsIgnoreCase(gameType)) {
      return "CW";
    }
    if ("arrowword".equalsIgnoreCase(gameType)) {
      return "AW";
    }
    if ("domino".equalsIgnoreCase(gameType)) {
      return "DM";
    }
    if ("memory".equalsIgnoreCase(gameType)) {
      return "MM";
    }
    if ("scrabble".equalsIgnoreCase(gameType)) {
      return "SC";
    }
    if ("anagram".equalsIgnoreCase(gameType)) {
      return "AN";
    }
    return "WS";
  }

  private static int countFilledGridCells(List<String> grid, char blankMarker) {
    if (grid == null) {
      return 0;
    }

    int count = 0;
    for (String row : grid) {
      if (row == null) {
        continue;
      }
      for (int index = 0; index < row.length(); index += 1) {
        if (row.charAt(index) != blankMarker) {
          count += 1;
        }
      }
    }
    return count;
  }

  private static double computeFillRatio(int filledCells, int totalCells) {
    if (totalCells <= 0) {
      return 0.0d;
    }
    double ratio = (double) filledCells / (double) totalCells;
    return Math.round(ratio * 1000.0d) / 1000.0d;
  }

  private static boolean shouldGenerateWordsearch(PuzzleTypeMode mode) {
    return mode == PuzzleTypeMode.BOTH || mode == PuzzleTypeMode.WORDSEARCH_ONLY;
  }

  private static boolean shouldGenerateCrossword(PuzzleTypeMode mode) {
    return mode == PuzzleTypeMode.BOTH || mode == PuzzleTypeMode.CROSSWORD_ONLY;
  }

  private static boolean shouldGenerateArrowword(PuzzleTypeMode mode) {
    return mode == PuzzleTypeMode.BOTH || mode == PuzzleTypeMode.ARROWWORD_ONLY;
  }

  private static boolean shouldGenerateDomino(PuzzleTypeMode mode) {
    return mode == PuzzleTypeMode.BOTH || mode == PuzzleTypeMode.DOMINO_ONLY;
  }

  private static boolean shouldGenerateMemory(PuzzleTypeMode mode) {
    return mode == PuzzleTypeMode.BOTH || mode == PuzzleTypeMode.MEMORY_ONLY;
  }

  private static boolean shouldGenerateScrabble(PuzzleTypeMode mode) {
    return mode == PuzzleTypeMode.BOTH || mode == PuzzleTypeMode.SCRABBLE_ONLY;
  }

  private static boolean shouldGenerateAnagram(PuzzleTypeMode mode) {
    return mode == PuzzleTypeMode.BOTH || mode == PuzzleTypeMode.ANAGRAM_ONLY;
  }

  private static PuzzleTypeMode parsePuzzleTypeMode(String raw) {
    if (raw == null) {
      return PuzzleTypeMode.BOTH;
    }
    String value = raw.trim().toLowerCase(Locale.ROOT);
    if ("wordsearch".equals(value) || "mots-meles".equals(value) || "motsmeles".equals(value)) {
      return PuzzleTypeMode.WORDSEARCH_ONLY;
    }
    if ("crossword".equals(value) || "mots-croises".equals(value) || "motscroises".equals(value)) {
      return PuzzleTypeMode.CROSSWORD_ONLY;
    }
    if ("arrowword".equals(value) || "mots-fleches".equals(value) || "motsfleches".equals(value)) {
      return PuzzleTypeMode.ARROWWORD_ONLY;
    }
    if ("domino".equals(value) || "dominos".equals(value) || "morpho-domino".equals(value) || "morphodomino".equals(value)) {
      return PuzzleTypeMode.DOMINO_ONLY;
    }
    if ("memory".equals(value) || "memory-match".equals(value) || "memorymatch".equals(value) || "memoire".equals(value)) {
      return PuzzleTypeMode.MEMORY_ONLY;
    }
    if ("scrabble".equals(value) || "scrabble-like".equals(value) || "scrabblelike".equals(value) || "construction".equals(value)) {
      return PuzzleTypeMode.SCRABBLE_ONLY;
    }
    if ("anagram".equals(value) || "anagrams".equals(value) || "anagramme".equals(value) || "anagrammes".equals(value)
        || "morpho-anagram".equals(value) || "morphoanagram".equals(value)) {
      return PuzzleTypeMode.ANAGRAM_ONLY;
    }
    return PuzzleTypeMode.BOTH;
  }

  private static final class CrossPlacedWord {
    final WordToFind word;
    final int row;
    final int col;
    final boolean across;

    CrossPlacedWord(WordToFind word, int row, int col, boolean across) {
      this.word = word;
      this.row = row;
      this.col = col;
      this.across = across;
    }
  }

  private static final class DominoMaterial {
    String baseForm;
    String displayForm;
    String normalized;
    String translation;
    String translationEn;
    String slug;
    String partOfSpeech;
    String phonetic;
    String extraInfo;
  }

  private static final class MemoryMaterial {
    String baseForm;
    String displayForm;
    String baseNormalized;
    String translation;
    String translationNormalized;
    String translationEn;
    String slug;
    String partOfSpeech;
    String phonetic;
    String extraInfo;
    String root;
    String singular;
    String plural;
  }

  private static final class MemoryGridSpec {
    final int rows;
    final int cols;

    MemoryGridSpec(int rows, int cols) {
      this.rows = rows;
      this.cols = cols;
    }
  }

  private static final class ScrabbleMaterial {
    String baseForm;
    String displayForm;
    String normalized;
    String translation;
    String translationEn;
    String slug;
    String partOfSpeech;
    String phonetic;
    String extraInfo;
    String root;
    String singular;
    String plural;
    int score;
  }

  private static final class SegmentChunk {
    final String value;
    final String role;

    SegmentChunk(String value, String role) {
      this.value = value;
      this.role = role;
    }
  }

  private static final class AnagramPieceMaterial {
    String id;
    String text;
    String normalized;
    String role;
    int points;
  }

  private static final class AnagramMaterial {
    String baseForm;
    String displayForm;
    String normalized;
    String translation;
    String translationEn;
    String slug;
    String partOfSpeech;
    String phonetic;
    String extraInfo;
    String root;
    String singular;
    String plural;
    int score;
    List<AnagramPieceMaterial> pieces = new ArrayList<>();
    List<String> solutionOrder = new ArrayList<>();
    List<String> roles = new ArrayList<>();
  }
}
