package com.longoka.games.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.longoka.games.puzzles.memory.MemoryMatchValidator;
import com.longoka.games.puzzles.memory.json.MemoryMatchJsonModels;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Exporteur de pack memory-match Natikongo.
 *
 * Le principe retenu ici suit la logique de la fonte Longoka Natikongo:
 * - les glyphes de lettres reutilisent directement les touches ASCII du clavier
 * - les chiffres simples reutilisent 0-9
 * - le front peut appliquer la police en lisant meta.fontFamily sur les cartes glyphes
 */
public final class NatikongoMemoryMatchPackExporter {

  private static final String FONT_FAMILY = "Longoka Natikongo";
  private static final String FONT_FAMILY_FINE = "Longoka Natikongo Fine";
  private static final String GLYPH_ASSET_BASE = "/images/natikongo-keyboard/characters/natikongo-glyph-sources/";

  private static final List<GlyphEntry> LETTERS = List.of(
      glyph("a", "A", "Voyelle A"),
      glyph("b", "B", "Consonne B"),
      glyph("d", "D", "Consonne D"),
      glyph("e", "E", "Voyelle E"),
      glyph("f", "F", "Consonne F"),
      glyph("g", "G", "Consonne G"),
      glyph("h", "H", "Consonne H"),
      glyph("i", "I", "Voyelle I"),
      glyph("j", "J", "Consonne J"),
      glyph("k", "K", "Consonne K"),
      glyph("l", "L", "Consonne L"),
      glyph("m", "M", "Consonne M"),
      glyph("n", "N", "Consonne N"),
      glyph("o", "O", "Voyelle O"),
      glyph("p", "P", "Consonne P"),
      glyph("r", "R", "Consonne R"),
      glyph("s", "S", "Consonne S"),
      glyph("t", "T", "Consonne T"),
      glyph("u", "U", "Voyelle U"),
      glyph("v", "V", "Consonne V"),
      glyph("w", "W", "Consonne W"),
      glyph("y", "Y", "Consonne Y"),
      glyph("z", "Z", "Consonne Z")
  );

  private static final List<GlyphEntry> DIGITS = List.of(
      number("0", "0", "Zero"),
      number("1", "1", "Un"),
      number("2", "2", "Deux"),
      number("3", "3", "Trois"),
      number("4", "4", "Quatre"),
      number("5", "5", "Cinq"),
      number("6", "6", "Six"),
      number("7", "7", "Sept"),
      number("8", "8", "Huit"),
      number("9", "9", "Neuf")
  );

  private NatikongoMemoryMatchPackExporter() {
    // utilitaire
  }

  public static void exportBasicsPack(String outputPath) throws Exception {
    MemoryMatchJsonModels.PackV1 pack = buildBasicsPack();
    MemoryMatchValidator.validatePack(pack);

    ObjectMapper mapper = new ObjectMapper();
    mapper.enable(SerializationFeature.INDENT_OUTPUT);

    Path out = Path.of(outputPath);
    if (out.getParent() != null) {
      Files.createDirectories(out.getParent());
    }

    mapper.writeValue(out.toFile(), pack);
    System.out.println("✅ Export JSON (memory-match Natikongo) termine : " + out.toAbsolutePath());
  }

  public static MemoryMatchJsonModels.PackV1 buildBasicsPack() {
    MemoryMatchJsonModels.PackV1 pack = new MemoryMatchJsonModels.PackV1();
    pack.language = "natikongo";
    pack.packId = "natikongo-memory-basics-" + LocalDate.now();
    pack.title = "Natikongo - Memory match des glyphes";
    pack.description = "Pack de base pour reconnaitre les glyphes Natikongo des lettres et des chiffres.";
    pack.meaningLanguage = "fr";
    pack.meta = new LinkedHashMap<>();
    pack.meta.put("courseSlug", "apprenez-lecriture-natikongo");
    pack.meta.put("fontFamily", FONT_FAMILY);
    pack.meta.put("fontFamilyFine", FONT_FAMILY_FINE);
    pack.meta.put("glyphAssetBase", GLYPH_ASSET_BASE);
    pack.meta.put("scriptType", "african-writing-system");
    pack.meta.put("notes", List.of(
        "Les cartes de type glyph doivent etre rendues avec la police Longoka Natikongo.",
        "Les touches x, c et q sont reservees comme cles muettes de composition pour les nombres composes.",
        "Ce premier pack couvre l'alphabet de base et les chiffres 0-9."
    ));

    List<MemoryMatchJsonModels.PuzzleV1> puzzles = new ArrayList<>();
    puzzles.add(buildPuzzle(
        "natikongo-alphabet-core",
        "Alphabet de base",
        "alphabet",
        "glyph-to-latin",
        "standard",
        6,
        8,
        LETTERS,
        "facile"
    ));
    puzzles.add(buildPuzzle(
        "natikongo-digits-core",
        "Chiffres 0 a 9",
        "numbers",
        "glyph-to-arabic",
        "compact",
        4,
        5,
        DIGITS,
        "facile"
    ));

    pack.puzzles = puzzles;
    return pack;
  }

  private static MemoryMatchJsonModels.PuzzleV1 buildPuzzle(
      String id,
      String title,
      String theme,
      String relationType,
      String layout,
      int rows,
      int cols,
      List<GlyphEntry> entries,
      String difficulty) {

    List<MemoryMatchJsonModels.CardV1> cards = new ArrayList<>();
    for (GlyphEntry entry : entries) {
      cards.add(buildGlyphCard(id, entry));
      cards.add(buildReferenceCard(id, entry));
    }

    MemoryMatchJsonModels.PuzzleV1 puzzle = new MemoryMatchJsonModels.PuzzleV1();
    puzzle.id = id;
    puzzle.language = "natikongo";
    puzzle.mode = "memory-match";
    puzzle.difficulty = difficulty;
    puzzle.title = title;
    puzzle.theme = theme;
    puzzle.relationType = relationType;
    puzzle.layout = layout;
    puzzle.rows = rows;
    puzzle.cols = cols;
    puzzle.pairCount = entries.size();
    puzzle.cards = cards;
    puzzle.meta = new LinkedHashMap<>();
    puzzle.meta.put("fontFamily", FONT_FAMILY);
    puzzle.meta.put("themeLabel", title);
    puzzle.meta.put("cardKinds", List.of("glyph", "reference"));

    return puzzle;
  }

  private static MemoryMatchJsonModels.CardV1 buildGlyphCard(String puzzleId, GlyphEntry entry) {
    MemoryMatchJsonModels.CardV1 card = new MemoryMatchJsonModels.CardV1();
    card.id = puzzleId + "-glyph-" + entry.id;
    card.pairId = puzzleId + "-pair-" + entry.id;
    card.kind = "glyph";
    card.value = entry.input;
    card.display = entry.input;
    card.normalized = normalize(entry.reference);
    card.label = "Glyphe Natikongo";
    card.hint = entry.hint;
    card.entryRef = new MemoryMatchJsonModels.EntryRefV1();
    card.entryRef.slug = entry.id;
    card.entryRef.translation = entry.reference;
    card.entryRef.extraInfo = entry.hint;
    card.meta = new LinkedHashMap<>();
    card.meta.put("renderType", "font");
    card.meta.put("fontFamily", FONT_FAMILY);
    card.meta.put("svgAssetPath", GLYPH_ASSET_BASE + entry.assetFileName);
    card.meta.put("referenceDisplay", entry.reference);
    card.meta.put("group", entry.group);
    return card;
  }

  private static MemoryMatchJsonModels.CardV1 buildReferenceCard(String puzzleId, GlyphEntry entry) {
    MemoryMatchJsonModels.CardV1 card = new MemoryMatchJsonModels.CardV1();
    card.id = puzzleId + "-reference-" + entry.id;
    card.pairId = puzzleId + "-pair-" + entry.id;
    card.kind = "reference";
    card.value = entry.reference;
    card.display = entry.reference;
    card.normalized = normalize(entry.reference);
    card.label = entry.group.equals("number") ? "Nombre arabe" : "Lettre latine";
    card.hint = entry.hint;
    card.entryRef = new MemoryMatchJsonModels.EntryRefV1();
    card.entryRef.slug = entry.id;
    card.entryRef.translation = entry.reference;
    card.entryRef.extraInfo = entry.hint;
    card.meta = new LinkedHashMap<>();
    card.meta.put("renderType", "text");
    card.meta.put("group", entry.group);
    return card;
  }

  private static GlyphEntry glyph(String id, String reference, String hint) {
    return new GlyphEntry(id, id, reference, hint, id + ".svg", "letter");
  }

  private static GlyphEntry number(String id, String reference, String hint) {
    return new GlyphEntry(id, id, reference, hint, id + ".svg", "number");
  }

  private static String normalize(String value) {
    return value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
  }

  private record GlyphEntry(
      String id,
      String input,
      String reference,
      String hint,
      String assetFileName,
      String group) {
  }
}