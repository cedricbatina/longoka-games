package com.longoka.games.lexikongo;

import com.longoka.games.app.DbConfig;
import com.longoka.games.puzzles.domino.MorphoDominoValidator;
import com.longoka.games.puzzles.domino.json.MorphoDominoJsonModels;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Premier moteur de dominos morphologiques Kikongo.
 *
 * Phase 1:
 * - classes nominales
 * - singulier seul
 * - pluriel seul
 * - layout "chain"
 */
public final class KikongoMorphoDominoService {

  private static final int DEFAULT_TILE_TARGET = 12;

  public enum NumberPolicy {
    SINGULAR_ONLY("singular-only", "singulier"),
    PLURAL_ONLY("plural-only", "pluriel");

    final String id;
    final String label;

    NumberPolicy(String id, String label) {
      this.id = id;
      this.label = label;
    }
  }

  public MorphoDominoJsonModels.PackV1 generateNominalClassPack(
      NumberPolicy numberPolicy,
      int puzzleCount,
      int targetTileCount,
      String meaningLanguage) throws SQLException {

    int safePuzzleCount = Math.max(1, puzzleCount);
    int safeTileTarget = normalizeTileTarget(targetTileCount);

    try (Connection conn = DbConfig.openKikongoLexConnection()) {
      LexikongoWordRepository repo = new LexikongoWordRepository(conn);
      List<NominalClass> classes = repo.listAvailableNominalClasses(
          Math.max(3, safeTileTarget / 2),
          numberPolicy == NumberPolicy.SINGULAR_ONLY,
          numberPolicy == NumberPolicy.PLURAL_ONLY);

      if (classes.isEmpty()) {
        throw new IllegalStateException("Aucune classe nominale exploitable pour les dominos.");
      }

      List<MorphoDominoJsonModels.PuzzleV1> puzzles = new ArrayList<>();
      for (int i = 0; i < safePuzzleCount; i++) {
        NominalClass nominalClass = classes.get(i % classes.size());
        MorphoDominoJsonModels.PuzzleV1 puzzle = generateNominalClassPuzzle(
            conn,
            repo,
            nominalClass,
            numberPolicy,
            safeTileTarget,
            meaningLanguage,
            i + 1);
        puzzles.add(puzzle);
      }

      MorphoDominoJsonModels.PackV1 pack = new MorphoDominoJsonModels.PackV1();
      pack.language = "kg";
      pack.packId = "kg-morpho-domino-class-" + numberPolicy.id + "-" + LocalDate.now();
      pack.title = "Kikongo - dominos morphologiques (" + numberPolicy.label + ")";
      pack.description = "Pack genere automatiquement : "
          + safePuzzleCount + " puzzles de dominos morphologiques par classes nominales.";
      pack.meaningLanguage = meaningLanguage;
      pack.puzzles = puzzles;
      pack.meta = new LinkedHashMap<>();
      pack.meta.put("gameType", "morpho-domino");
      pack.meta.put("lexicalProfile", "nouns");
      pack.meta.put("morphologyProfile", "nominal-class");
      pack.meta.put("numberPolicy", numberPolicy.id);
      pack.meta.put("relationType", "class-membership");
      pack.meta.put("layout", "chain");
      pack.meta.put("tileTarget", safeTileTarget);

      MorphoDominoValidator.validatePack(pack);
      return pack;
    }
  }

  public MorphoDominoJsonModels.PuzzleV1 generateNominalClassPuzzle(
      int classId,
      NumberPolicy numberPolicy,
      int targetTileCount,
      String meaningLanguage,
      int index) throws SQLException {

    try (Connection conn = DbConfig.openKikongoLexConnection()) {
      LexikongoWordRepository repo = new LexikongoWordRepository(conn);
      List<NominalClass> classes = repo.listAvailableNominalClasses(
          1,
          numberPolicy == NumberPolicy.SINGULAR_ONLY,
          numberPolicy == NumberPolicy.PLURAL_ONLY);

      NominalClass nominalClass = classes.stream()
          .filter(item -> item != null && item.getClassId() == classId)
          .findFirst()
          .orElseThrow(() -> new IllegalArgumentException("Classe nominale introuvable: " + classId));

      return generateNominalClassPuzzle(conn, repo, nominalClass, numberPolicy, targetTileCount, meaningLanguage, index);
    }
  }

  private MorphoDominoJsonModels.PuzzleV1 generateNominalClassPuzzle(
      Connection conn,
      LexikongoWordRepository repo,
      NominalClass nominalClass,
      NumberPolicy numberPolicy,
      int targetTileCount,
      String meaningLanguage,
      int index) throws SQLException {

    int safeTileTarget = normalizeTileTarget(targetTileCount);
    int wordTarget = Math.max(3, safeTileTarget / 2);
    int fetchLimit = Math.max(wordTarget * 4, wordTarget + 10);

    List<LexWord> source = repo.findRandomWordsByClassId(
        nominalClass.getClassId(),
        fetchLimit,
        meaningLanguage);

    List<WordDominoMaterial> materials = buildWordMaterials(source, numberPolicy, meaningLanguage, wordTarget);
    if (materials.size() < 3) {
      throw new IllegalStateException(
          "Pas assez de mots exploitables pour la classe " + nominalClass.getClassId() + " (" + numberPolicy.id + ")");
    }

    List<MorphoDominoJsonModels.TileV1> tiles = new ArrayList<>();
    List<String> solutionOrder = new ArrayList<>();
    String classValue = String.valueOf(nominalClass.getClassId());
    String classDisplay = classDisplay(nominalClass);
    String classNormalized = "CLASS_" + nominalClass.getClassId();

    int tileIndex = 1;
    for (WordDominoMaterial material : materials) {
      MorphoDominoJsonModels.TileV1 first = tile(
          "tile-" + tileIndex++,
          side("class", classValue, classDisplay, classNormalized, "Classe nominale"),
          side("form", material.formValue, material.formDisplay, material.formNormalized, numberLabel(numberPolicy)),
          material);
      tiles.add(first);
      solutionOrder.add(first.id);

      MorphoDominoJsonModels.TileV1 second = tile(
          "tile-" + tileIndex++,
          side("form", material.formValue, material.formDisplay, material.formNormalized, numberLabel(numberPolicy)),
          side("class", classValue, classDisplay, classNormalized, "Classe nominale"),
          material);
      tiles.add(second);
      solutionOrder.add(second.id);
    }

    MorphoDominoJsonModels.PuzzleV1 puzzle = new MorphoDominoJsonModels.PuzzleV1();
    puzzle.id = "kg-domino-class-" + nominalClass.getClassId() + "-" + numberPolicy.id + "-" + index;
    puzzle.language = "kg";
    puzzle.mode = "nouns";
    puzzle.difficulty = "easy";
    puzzle.title = "Classe " + nominalClass.getClassId() + " - " + numberPolicy.label;
    puzzle.theme = "classes nominales";
    puzzle.relationType = "class-membership";
    puzzle.layout = "chain";
    puzzle.tiles = tiles;
    puzzle.solutionOrder = solutionOrder;
    puzzle.meta = new LinkedHashMap<>();
    puzzle.meta.put("nominalClassId", nominalClass.getClassId());
    puzzle.meta.put("nominalClassName", nominalClass.getClassName());
    puzzle.meta.put("numberPolicy", numberPolicy.id);
    puzzle.meta.put("lexicalProfile", "nouns");
    puzzle.meta.put("morphologyProfile", "nominal-class");
    puzzle.meta.put("wordCount", materials.size());
    puzzle.meta.put("tileCount", tiles.size());
    puzzle.meta.put("meaningLanguage", meaningLanguage);

    MorphoDominoValidator.validatePuzzle(puzzle);
    return puzzle;
  }

  private List<WordDominoMaterial> buildWordMaterials(
      List<LexWord> source,
      NumberPolicy numberPolicy,
      String meaningLanguage,
      int wordTarget) {

    List<WordDominoMaterial> out = new ArrayList<>();
    Set<String> seenForms = new LinkedHashSet<>();

    for (LexWord word : source) {
      if (out.size() >= wordTarget) {
        break;
      }
      if (word == null) {
        continue;
      }

      String form = selectForm(word, numberPolicy);
      String normalized = normalizeValue(form);
      if (normalized.isBlank() || seenForms.contains(normalized)) {
        continue;
      }

      String translation = firstNonBlank(
          word.joinMeaningsForLanguage(meaningLanguage, " ; "),
          word.getFrenchMeaningsJoined(),
          word.getEnglishMeaningsJoined(),
          firstMeaning(word.getMeanings()));
      if (translation == null || translation.isBlank()) {
        continue;
      }

      WordDominoMaterial material = new WordDominoMaterial();
      material.word = word;
      material.formValue = form.trim();
      material.formDisplay = form.trim();
      material.formNormalized = normalized;
      material.translation = translation.trim();
      material.translationEn = blankToNull(word.getEnglishMeaningsJoined());

      out.add(material);
      seenForms.add(normalized);
    }

    return out;
  }

  private String selectForm(LexWord word, NumberPolicy numberPolicy) {
    if (word == null) {
      return null;
    }

    return numberPolicy == NumberPolicy.PLURAL_ONLY
        ? blankToNull(word.getPlural())
        : blankToNull(word.getSingular());
  }

  private MorphoDominoJsonModels.TileV1 tile(
      String id,
      MorphoDominoJsonModels.SideV1 left,
      MorphoDominoJsonModels.SideV1 right,
      WordDominoMaterial material) {

    MorphoDominoJsonModels.TileV1 tile = new MorphoDominoJsonModels.TileV1();
    tile.id = id;
    tile.left = left;
    tile.right = right;
    tile.entryRef = new MorphoDominoJsonModels.EntryRefV1();
    tile.entryRef.wordId = material.word.getWordId();
    tile.entryRef.slug = material.word.getSlug();
    tile.entryRef.partOfSpeech = "noun";
    tile.entryRef.phonetic = material.word.getPhonetic();
    tile.entryRef.extraInfo = material.word.getNominalClass() != null ? material.word.getNominalClass().getClassName() : null;
    tile.entryRef.singular = material.word.getSingular();
    tile.entryRef.plural = material.word.getPlural();
    tile.entryRef.root = material.word.getRoot();
    tile.entryRef.translation = material.translation;
    tile.entryRef.translationEn = material.translationEn;

    tile.meta = new LinkedHashMap<>();
    tile.meta.put("nominalClassId", material.word.getNominalClass() != null ? material.word.getNominalClass().getClassId() : null);
    tile.meta.put("nominalClassName", material.word.getNominalClass() != null ? material.word.getNominalClass().getClassName() : null);
    tile.meta.put("slug", material.word.getSlug());
    return tile;
  }

  private MorphoDominoJsonModels.SideV1 side(
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

  private int normalizeTileTarget(int targetTileCount) {
    int safe = targetTileCount > 0 ? targetTileCount : DEFAULT_TILE_TARGET;
    if ((safe % 2) != 0) {
      safe -= 1;
    }
    return Math.max(6, safe);
  }

  private String classDisplay(NominalClass nominalClass) {
    if (nominalClass == null) {
      return "Classe";
    }
    String name = blankToNull(nominalClass.getClassName());
    if (name == null) {
      return "Classe " + nominalClass.getClassId();
    }
    return "Classe " + nominalClass.getClassId() + " (" + name + ")";
  }

  private String numberLabel(NumberPolicy numberPolicy) {
    return numberPolicy == NumberPolicy.PLURAL_ONLY ? "Forme plurielle" : "Forme singuliere";
  }

  private String normalizeValue(String value) {
    if (value == null) {
      return "";
    }
    String upper = value.trim().toUpperCase(Locale.ROOT);
    StringBuilder sb = new StringBuilder(upper.length());
    for (int i = 0; i < upper.length(); i++) {
      char ch = upper.charAt(i);
      if ((ch >= 'A' && ch <= 'Z') || (ch >= '0' && ch <= '9')) {
        sb.append(ch);
      }
    }
    return sb.toString();
  }

  private String blankToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private String firstMeaning(List<LexMeaning> meanings) {
    if (meanings == null) {
      return null;
    }
    for (LexMeaning meaning : meanings) {
      if (meaning == null) {
        continue;
      }
      String text = blankToNull(meaning.getMeaning());
      if (text != null) {
        return text;
      }
    }
    return null;
  }

  private String firstNonBlank(String... values) {
    for (String value : values) {
      String normalized = blankToNull(value);
      if (normalized != null) {
        return normalized;
      }
    }
    return null;
  }

  private static final class WordDominoMaterial {
    LexWord word;
    String formValue;
    String formDisplay;
    String formNormalized;
    String translation;
    String translationEn;
  }
}
