package com.longoka.games.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.longoka.games.puzzles.arrowword.ArrowwordValidator;
import com.longoka.games.puzzles.arrowword.json.ArrowwordJsonModels;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Premier export de démonstration pour un format "mots fléchés".
 * Ici la grille est volontairement manuelle pour valider le schéma, pas encore le solveur.
 */
public final class KikongoArrowwordJsonExportTool {

  private KikongoArrowwordJsonExportTool() {
    // utilitaire
  }

  public static void main(String[] args) throws Exception {
    System.out.println("== Export JSON (démo) : Mots fléchés Kikongo ==");

    String meaningLanguageCode = "fr";
    String languageCode = "kg";
    String outputPath = "target/kikongo-arrowword-demo.v1.json";

    ArrowwordJsonModels.PackV1 pack = new ArrowwordJsonModels.PackV1();
    pack.language = languageCode;
    pack.packId = "kg-arrow-demo-" + LocalDate.now();
    pack.title = "Kikongo – mots fléchés (démo)";
    pack.description = "Prototype manuel de pack mots fléchés pour valider le format JSON.";
    pack.meaningLanguage = meaningLanguageCode;
    pack.puzzles = List.of(
      buildAcrossDemoPuzzle(languageCode),
      buildMixedDirectionsDemoPuzzle(languageCode)
    );

    ArrowwordValidator.validatePack(pack);

    ObjectMapper mapper = new ObjectMapper();
    mapper.enable(SerializationFeature.INDENT_OUTPUT);

    Path out = Path.of(outputPath);
    if (out.getParent() != null) {
      Files.createDirectories(out.getParent());
    }

    mapper.writeValue(out.toFile(), pack);
    System.out.println("✅ Export JSON (mots fléchés démo) terminé : " + out.toAbsolutePath());
  }

  private static ArrowwordJsonModels.PuzzleV1 buildAcrossDemoPuzzle(String languageCode) {
    ArrowwordJsonModels.PuzzleV1 puzzle = new ArrowwordJsonModels.PuzzleV1();
    puzzle.id = "kg-arrow-demo-1";
    puzzle.language = languageCode;
    puzzle.mode = "demo";
    puzzle.difficulty = "easy";
    puzzle.title = "Mots fléchés Kikongo (démo 1)";
    puzzle.theme = "indices en ligne";
    puzzle.rows = 8;
    puzzle.cols = 8;

    List<List<ArrowwordJsonModels.CellV1>> cells = new ArrayList<>();
    cells.add(row(
      clueSingle("Demeure", "RIGHT", "1-R"),
      letter("N"), letter("Z"), letter("O"),
      block(), block(), block(), block()
    ));
    cells.add(row(
      block(),
      clueSingle("Personne", "RIGHT", "2-R"),
      letter("M"), letter("U"), letter("N"), letter("T"), letter("U"),
      block()
    ));
    cells.add(row(
      block(),
      clueSingle("Nouvelles", "RIGHT", "3-R"),
      letter("M"), letter("A"), letter("M"), letter("B"), letter("U"),
      block()
    ));
    cells.add(row(
      block(),
      clueSingle("Argent", "RIGHT", "4-R"),
      letter("M"), letter("B"), letter("O"), letter("N"), letter("G"), letter("O")
    ));
    for (int i = 0; i < 4; i++) {
      cells.add(row(block(), block(), block(), block(), block(), block(), block(), block()));
    }

    puzzle.cells = cells;
    puzzle.entries = List.of(
      entry("1-R", "RIGHT", 0, 0, 0, 1, "NZO", "Nzo", "Demeure", "House", "noun", "n-zi", List.of("habitation")),
      entry("2-R", "RIGHT", 1, 1, 1, 2, "MUNTU", "Muntu", "Personne", "Person", "noun", null, List.of("human")),
      entry("3-R", "RIGHT", 2, 1, 2, 2, "MAMBU", "Mambu", "Nouvelles", "News", "noun", null, List.of("information")),
      entry("4-R", "RIGHT", 3, 1, 3, 2, "MBONGO", "Mbongo", "Argent", "Money", "noun", null, List.of("economy"))
    );

    Map<String, Object> meta = new HashMap<>();
    meta.put("source", "arrowword-demo-manual");
    meta.put("note", "Prototype orienté schéma, sans solveur automatique.");
    meta.put("meaningLanguage", "fr");
    puzzle.meta = meta;

    return puzzle;
  }

  private static ArrowwordJsonModels.PuzzleV1 buildMixedDirectionsDemoPuzzle(String languageCode) {
    ArrowwordJsonModels.PuzzleV1 puzzle = new ArrowwordJsonModels.PuzzleV1();
    puzzle.id = "kg-arrow-demo-2";
    puzzle.language = languageCode;
    puzzle.mode = "demo";
    puzzle.difficulty = "easy";
    puzzle.title = "Mots fléchés Kikongo (démo 2)";
    puzzle.theme = "droite + bas";
    puzzle.rows = 8;
    puzzle.cols = 8;

    List<List<ArrowwordJsonModels.CellV1>> cells = new ArrayList<>();
    cells.add(row(
      block(),
      clueSingle("Demeure", "DOWN", "1-D"),
      block(), block(), block(), block(),
      clueSingle("Route", "DOWN", "2-D"),
      block()
    ));
    cells.add(row(
      block(),
      letter("N"),
      block(), block(), block(), block(),
      letter("N"),
      block()
    ));
    cells.add(row(
      block(),
      letter("Z"),
      block(), block(), block(), block(),
      letter("Z"),
      block()
    ));
    cells.add(row(
      block(),
      letter("O"),
      block(), block(), block(), block(),
      letter("I"),
      block()
    ));
    cells.add(row(
      block(), block(), block(), block(), block(), block(),
      letter("L"),
      block()
    ));
    cells.add(row(
      clueSingle("Nouvelles", "RIGHT", "3-R"),
      letter("M"), letter("A"), letter("M"), letter("B"), letter("U"),
      letter("A"),
      block()
    ));
    cells.add(row(block(), block(), block(), block(), block(), block(), block(), block()));
    cells.add(row(block(), block(), block(), block(), block(), block(), block(), block()));

    puzzle.cells = cells;
    puzzle.entries = List.of(
      entry("1-D", "DOWN", 0, 1, 1, 1, "NZO", "Nzo", "Demeure", "House", "noun", "n-zi", List.of("habitation")),
      entry("2-D", "DOWN", 0, 6, 1, 6, "NZILA", "Nzila", "Route", "Road", "noun", null, List.of("movement")),
      entry("3-R", "RIGHT", 5, 0, 5, 1, "MAMBU", "Mambu", "Nouvelles", "News", "noun", null, List.of("information"))
    );

    Map<String, Object> meta = new HashMap<>();
    meta.put("source", "arrowword-demo-manual");
    meta.put("note", "Prototype mixte avec entrées horizontales et verticales.");
    meta.put("meaningLanguage", "fr");
    puzzle.meta = meta;

    return puzzle;
  }

  private static ArrowwordJsonModels.EntryV1 entry(
    String id,
    String direction,
    int clueRow,
    int clueCol,
    int startRow,
    int startCol,
    String answer,
    String display,
    String translation,
    String translationEn,
    String partOfSpeech,
    String extraInfo,
    List<String> semanticTags
  ) {
    ArrowwordJsonModels.EntryV1 entry = new ArrowwordJsonModels.EntryV1();
    entry.id = id;
    entry.direction = direction;
    entry.clueRow = clueRow;
    entry.clueCol = clueCol;
    entry.startRow = startRow;
    entry.startCol = startCol;
    entry.answer = answer;
    entry.display = display;
    entry.translation = translation;
    entry.translationEn = translationEn;
    entry.partOfSpeech = partOfSpeech;
    entry.extraInfo = extraInfo;
    entry.semanticTags = semanticTags;
    return entry;
  }

  private static List<ArrowwordJsonModels.CellV1> row(ArrowwordJsonModels.CellV1... cells) {
    return Arrays.asList(cells);
  }

  private static ArrowwordJsonModels.CellV1 block() {
    ArrowwordJsonModels.CellV1 cell = new ArrowwordJsonModels.CellV1();
    cell.kind = "block";
    return cell;
  }

  private static ArrowwordJsonModels.CellV1 letter(String solution) {
    ArrowwordJsonModels.CellV1 cell = new ArrowwordJsonModels.CellV1();
    cell.kind = "letter";
    cell.solution = solution;
    return cell;
  }

  private static ArrowwordJsonModels.CellV1 clueSingle(String clue, String direction, String entryId) {
    ArrowwordJsonModels.CellV1 cell = new ArrowwordJsonModels.CellV1();
    cell.kind = "clue";
    cell.clue = clue;
    cell.arrows = List.of(direction);
    cell.entryIds = List.of(entryId);
    cell.clues = Map.of(direction, clue);
    return cell;
  }
}
