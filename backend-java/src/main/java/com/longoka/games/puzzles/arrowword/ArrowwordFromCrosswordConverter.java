package com.longoka.games.puzzles.arrowword;

import com.longoka.games.puzzles.arrowword.json.ArrowwordJsonModels;
import com.longoka.games.puzzles.crossword.json.CrosswordJsonModels;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Convertit une grille de mots croisés classique en mots fléchés.
 *
 * Principe :
 * - on décale la grille de lettres d'une case vers le bas et la droite
 * - la colonne 0 et la ligne 0 deviennent disponibles pour les cases-indices
 * - une entrée horizontale reçoit son indice à gauche
 * - une entrée verticale reçoit son indice au-dessus
 *
 * Cette conversion conserve les croisements du crossword source tout en
 * produisant un format orienté cellule, exploitable côté app et print.
 */
public final class ArrowwordFromCrosswordConverter {

  private ArrowwordFromCrosswordConverter() {
    // utilitaire
  }

  public static ArrowwordJsonModels.PuzzleV1 convertPuzzle(
    CrosswordJsonModels.PuzzleV1 crossword,
    String meaningLanguage
  ) {
    if (crossword == null) {
      throw new IllegalArgumentException("crossword null");
    }
    if (crossword.rows < 1 || crossword.cols < 1) {
      throw new IllegalArgumentException("dimensions crossword invalides");
    }

    ArrowwordJsonModels.PuzzleV1 puzzle = new ArrowwordJsonModels.PuzzleV1();
    puzzle.id = String.valueOf(crossword.id).replace("cross", "arrow");
    puzzle.language = crossword.language;
    puzzle.mode = crossword.mode;
    puzzle.difficulty = crossword.difficulty;
    puzzle.title = toArrowwordTitle(crossword.title);
    puzzle.theme = crossword.theme;
    puzzle.rows = crossword.rows + 1;
    puzzle.cols = crossword.cols + 1;

    List<List<ArrowwordJsonModels.CellV1>> cells = blockedGrid(puzzle.rows, puzzle.cols);

    // 1) Reporter les lettres du crossword, décalées de +1/+1.
    if (crossword.grid != null) {
      for (int row = 0; row < crossword.grid.size() && row < crossword.rows; row++) {
        String line = crossword.grid.get(row);
        if (line == null) {
          continue;
        }
        for (int col = 0; col < Math.min(line.length(), crossword.cols); col++) {
          char ch = line.charAt(col);
          if (ch == '#') {
            continue;
          }
          cells.get(row + 1).set(col + 1, letterCell(String.valueOf(ch)));
        }
      }
    }

    // 2) Convertir les entrées en cellules indices + entries arrowword.
    List<ArrowwordJsonModels.EntryV1> entries = new ArrayList<>();
    if (crossword.entries != null) {
      for (CrosswordJsonModels.EntryV1 entry : crossword.entries) {
        if (entry == null) {
          continue;
        }

        int startRow = entry.row + 1;
        int startCol = entry.col + 1;
        String direction = "ACROSS".equalsIgnoreCase(entry.direction) ? "RIGHT" : "DOWN";
        int clueRow = "RIGHT".equals(direction) ? startRow : startRow - 1;
        int clueCol = "RIGHT".equals(direction) ? startCol - 1 : startCol;

        mergeClueCell(cells, clueRow, clueCol, direction, entry);
        entries.add(toArrowEntry(entry, direction, clueRow, clueCol, startRow, startCol));
      }
    }

    puzzle.cells = cells;
    puzzle.entries = entries;

    Map<String, Object> meta = new LinkedHashMap<>();
    if (crossword.meta != null && !crossword.meta.isEmpty()) {
      meta.putAll(crossword.meta);
    }
    meta.put("sourceFormat", "longoka.crossword.pack.v1");
    meta.put("derivedFrom", crossword.id);
    meta.put("meaningLanguage", meaningLanguage);
    meta.put("render", Map.of(
      "strategy", "crossword-derived-v2",
      "supportsSharedClueCells", true,
      "printReady", true,
      "webReady", true,
      "topRowReservedForDownClues", true,
      "leftColReservedForAcrossClues", true
    ));
    puzzle.meta = meta;

    ArrowwordValidator.validatePuzzle(puzzle);
    return puzzle;
  }

  private static void mergeClueCell(
    List<List<ArrowwordJsonModels.CellV1>> cells,
    int row,
    int col,
    String direction,
    CrosswordJsonModels.EntryV1 entry
  ) {
    ArrowwordJsonModels.CellV1 cell = cells.get(row).get(col);
    if (!"clue".equals(String.valueOf(cell.kind))) {
      cell.kind = "clue";
      cell.solution = null;
      cell.clue = null;
      cell.arrows = new ArrayList<>();
      cell.entryIds = new ArrayList<>();
      cell.clues = new LinkedHashMap<>();
      cell.meta = new LinkedHashMap<>();
    }

    String fullClue = firstNonBlank(entry.clue, entry.translation, entry.translationEn, entry.display, entry.answer);
    String shortClue = shortenClue(fullClue);

    if (!cell.arrows.contains(direction)) {
      cell.arrows.add(direction);
    }
    if (!cell.entryIds.contains(entry.id)) {
      cell.entryIds.add(entry.id);
    }
    cell.clues.put(direction, shortClue);
    if (cell.clue == null || cell.clue.isBlank()) {
      cell.clue = shortClue;
    }

    @SuppressWarnings("unchecked")
    Map<String, Object> meta = (Map<String, Object>) cell.meta;
    if (meta == null) {
      meta = new LinkedHashMap<>();
      cell.meta = meta;
    }
    @SuppressWarnings("unchecked")
    Map<String, String> fullClues = (Map<String, String>) meta.computeIfAbsent("fullClues", key -> new LinkedHashMap<String, String>());
    fullClues.put(direction, fullClue);
  }

  private static ArrowwordJsonModels.EntryV1 toArrowEntry(
    CrosswordJsonModels.EntryV1 entry,
    String direction,
    int clueRow,
    int clueCol,
    int startRow,
    int startCol
  ) {
    ArrowwordJsonModels.EntryV1 target = new ArrowwordJsonModels.EntryV1();
    target.id = entry.id;
    target.direction = direction;
    target.clueRow = clueRow;
    target.clueCol = clueCol;
    target.startRow = startRow;
    target.startCol = startCol;
    target.answer = entry.answer;
    target.display = entry.display;
    target.translation = firstNonBlank(entry.translation, entry.clue);
    target.translationEn = entry.translationEn;
    target.phonetic = entry.phonetic;
    target.slug = entry.slug;
    target.partOfSpeech = entry.partOfSpeech;
    target.extraInfo = entry.extraInfo;
    target.semanticTags = entry.semanticTags;
    return target;
  }

  private static List<List<ArrowwordJsonModels.CellV1>> blockedGrid(int rows, int cols) {
    List<List<ArrowwordJsonModels.CellV1>> matrix = new ArrayList<>();
    for (int row = 0; row < rows; row++) {
      List<ArrowwordJsonModels.CellV1> line = new ArrayList<>();
      for (int col = 0; col < cols; col++) {
        line.add(blockCell());
      }
      matrix.add(line);
    }
    return matrix;
  }

  private static ArrowwordJsonModels.CellV1 blockCell() {
    ArrowwordJsonModels.CellV1 cell = new ArrowwordJsonModels.CellV1();
    cell.kind = "block";
    return cell;
  }

  private static ArrowwordJsonModels.CellV1 letterCell(String solution) {
    ArrowwordJsonModels.CellV1 cell = new ArrowwordJsonModels.CellV1();
    cell.kind = "letter";
    cell.solution = solution;
    return cell;
  }

  private static String shortenClue(String value) {
    String clue = normalizeSpaces(String.valueOf(value).trim());
    if (clue.isBlank()) {
      return "";
    }

    int split = indexOfFirst(clue, ';');
    if (split > 18) {
      String primary = clue.substring(0, split).trim();
      if (!primary.isBlank()) {
        clue = primary;
      }
    }

    int parenthesis = clue.indexOf('(');
    if (parenthesis > 18 && parenthesis <= 52) {
      clue = clue.substring(0, parenthesis).trim();
    }

    if (clue.length() <= 48) {
      return clue;
    }

    int cut = lastWhitespaceBefore(clue, 46);
    if (cut >= 18) {
      return clue.substring(0, cut).trim() + "…";
    }

    return clue.substring(0, 45).trim() + "…";
  }

  private static String toArrowwordTitle(String value) {
    String title = String.valueOf(value == null ? "" : value).trim();
    if (title.isBlank()) {
      return "Mots fléchés";
    }

    title = title.replace("croisés", "fléchés");
    title = title.replace("Croisés", "Fléchés");
    title = title.replace("croises", "fleches");
    title = title.replace("Croises", "Fleches");
    title = title.replace("crossword", "arrowword");
    title = title.replace("Crossword", "Arrowword");
    return title;
  }

  private static int indexOfFirst(String text, char... candidates) {
    int index = -1;
    for (char candidate : candidates) {
      int found = text.indexOf(candidate);
      if (found >= 0 && (index < 0 || found < index)) {
        index = found;
      }
    }
    return index;
  }

  private static int lastWhitespaceBefore(String text, int index) {
    int safeIndex = Math.min(index, text.length() - 1);
    for (int cursor = safeIndex; cursor >= 0; cursor--) {
      if (Character.isWhitespace(text.charAt(cursor))) {
        return cursor;
      }
    }
    return -1;
  }

  private static String normalizeSpaces(String value) {
    return String.valueOf(value == null ? "" : value)
      .replace('\n', ' ')
      .replace('\r', ' ')
      .replaceAll("\\s+", " ")
      .trim();
  }

  private static String firstNonBlank(String... values) {
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        return value.trim();
      }
    }
    return "";
  }
}
