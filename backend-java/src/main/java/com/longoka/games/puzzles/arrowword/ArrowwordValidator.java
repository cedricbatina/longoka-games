package com.longoka.games.puzzles.arrowword;

import com.longoka.games.puzzles.arrowword.json.ArrowwordJsonModels;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Validateur minimal pour les grilles "mots fléchés".
 * Suffisant pour sécuriser un premier prototype JSON.
 */
public final class ArrowwordValidator {

  private ArrowwordValidator() {
    // utilitaire
  }

  public static void validatePack(ArrowwordJsonModels.PackV1 pack) {
    require(pack != null, "pack null");
    require("longoka.arrowword.pack.v1".equals(pack.format), "format pack invalide");
    require(pack.version == 1, "version pack invalide");
    require(pack.puzzles != null && !pack.puzzles.isEmpty(), "pack sans puzzles");

    for (ArrowwordJsonModels.PuzzleV1 puzzle : pack.puzzles) {
      validatePuzzle(puzzle);
    }
  }

  public static void validatePuzzle(ArrowwordJsonModels.PuzzleV1 puzzle) {
    require(puzzle != null, "puzzle null");
    require(puzzle.rows > 0 && puzzle.cols > 0, "dimensions puzzle invalides");
    require(puzzle.cells != null && puzzle.cells.size() == puzzle.rows, "nombre de lignes invalide");
    require(puzzle.entries != null && !puzzle.entries.isEmpty(), "puzzle sans entrées");

    for (int row = 0; row < puzzle.rows; row++) {
      List<ArrowwordJsonModels.CellV1> line = puzzle.cells.get(row);
      require(line != null && line.size() == puzzle.cols, "nombre de colonnes invalide à la ligne " + row);
      for (int col = 0; col < puzzle.cols; col++) {
        ArrowwordJsonModels.CellV1 cell = line.get(col);
        require(cell != null, "cellule nulle à " + coord(row, col));
        String kind = normalize(cell.kind);
        require(Set.of("BLOCK", "LETTER", "CLUE").contains(kind), "kind cellule invalide à " + coord(row, col));

        if ("LETTER".equals(kind)) {
          require(!normalize(cell.solution).isEmpty(), "lettre manquante à " + coord(row, col));
        }
        if ("CLUE".equals(kind)) {
          require(cell.entryIds != null && !cell.entryIds.isEmpty(), "case indice sans entryIds à " + coord(row, col));
          require(cell.arrows != null && !cell.arrows.isEmpty(), "case indice sans arrows à " + coord(row, col));
          require(hasClueText(cell), "case indice sans texte à " + coord(row, col));
        }
      }
    }

    Map<String, ArrowwordJsonModels.EntryV1> entriesById = new HashMap<>();
    for (ArrowwordJsonModels.EntryV1 entry : puzzle.entries) {
      require(entry != null, "entrée nulle");
      require(!normalize(entry.id).isEmpty(), "entrée sans id");
      require(!entriesById.containsKey(entry.id), "id entrée dupliqué: " + entry.id);
      entriesById.put(entry.id, entry);
    }

    for (int row = 0; row < puzzle.rows; row++) {
      for (int col = 0; col < puzzle.cols; col++) {
        ArrowwordJsonModels.CellV1 cell = puzzle.cells.get(row).get(col);
        if (!"CLUE".equals(normalize(cell.kind))) {
          continue;
        }
        for (String entryId : cell.entryIds) {
          require(entriesById.containsKey(entryId), "entryId inconnu " + entryId + " à " + coord(row, col));
        }
      }
    }

    Set<String> usedLetterCoords = new HashSet<>();
    for (ArrowwordJsonModels.EntryV1 entry : puzzle.entries) {
      validateEntry(puzzle, entry, usedLetterCoords);
    }

    for (int row = 0; row < puzzle.rows; row++) {
      for (int col = 0; col < puzzle.cols; col++) {
        ArrowwordJsonModels.CellV1 cell = puzzle.cells.get(row).get(col);
        if ("LETTER".equals(normalize(cell.kind))) {
          require(
            usedLetterCoords.contains(coord(row, col)),
            "case lettre orpheline à " + coord(row, col)
          );
        }
      }
    }

    require(
      letterCellsOrthogonallyConnected(puzzle),
      "grille mots fléchés : cases LETTRE en plusieurs îlots (composantes orthogonales disjointes)"
    );
  }

  /**
   * Toutes les cases {@code LETTER} doivent former une seule composante orthogonale
   * (équivalent « zone blanche » du mots croisé source — pas de solution en îlots).
   */
  public static boolean letterCellsOrthogonallyConnected(ArrowwordJsonModels.PuzzleV1 puzzle) {
    if (puzzle == null || puzzle.cells == null || puzzle.rows < 1 || puzzle.cols < 1) {
      return true;
    }
    int rows = puzzle.rows;
    int cols = puzzle.cols;
    int startR = -1;
    int startC = -1;
    int letters = 0;
    for (int r = 0; r < rows; r++) {
      for (int c = 0; c < cols; c++) {
        ArrowwordJsonModels.CellV1 cell = puzzle.cells.get(r).get(c);
        if ("LETTER".equals(normalize(cell.kind))) {
          letters++;
          if (startR < 0) {
            startR = r;
            startC = c;
          }
        }
      }
    }
    if (letters <= 1) {
      return true;
    }

    boolean[][] vis = new boolean[rows][cols];
    Deque<int[]> q = new ArrayDeque<>();
    vis[startR][startC] = true;
    q.add(new int[] { startR, startC });
    int seen = 0;
    int[] dr = { -1, 1, 0, 0 };
    int[] dc = { 0, 0, -1, 1 };

    while (!q.isEmpty()) {
      int[] p = q.poll();
      int r = p[0];
      int c = p[1];
      if (!"LETTER".equals(normalize(puzzle.cells.get(r).get(c).kind))) {
        continue;
      }
      seen++;
      for (int k = 0; k < 4; k++) {
        int nr = r + dr[k];
        int nc = c + dc[k];
        if (nr < 0 || nr >= rows || nc < 0 || nc >= cols || vis[nr][nc]) {
          continue;
        }
        if (!"LETTER".equals(normalize(puzzle.cells.get(nr).get(nc).kind))) {
          continue;
        }
        vis[nr][nc] = true;
        q.add(new int[] { nr, nc });
      }
    }
    return seen == letters;
  }

  private static void validateEntry(
    ArrowwordJsonModels.PuzzleV1 puzzle,
    ArrowwordJsonModels.EntryV1 entry,
    Set<String> usedLetterCoords
  ) {
    String direction = normalize(entry.direction).toUpperCase(Locale.ROOT);
    require(Set.of("RIGHT", "DOWN").contains(direction), "direction invalide pour " + entry.id);

    require(inBounds(puzzle, entry.clueRow, entry.clueCol), "case indice hors grille pour " + entry.id);
    ArrowwordJsonModels.CellV1 clueCell = puzzle.cells.get(entry.clueRow).get(entry.clueCol);
    require("CLUE".equals(normalize(clueCell.kind)), "case indice invalide pour " + entry.id);
    require(clueCell.entryIds != null && clueCell.entryIds.contains(entry.id), "case indice non liée à " + entry.id);
    require(clueCell.arrows != null && clueCell.arrows.stream().map(ArrowwordValidator::normalize).anyMatch(direction::equalsIgnoreCase),
      "direction absente de la case indice pour " + entry.id);

    String answer = normalize(entry.answer);
    require(!answer.isEmpty(), "réponse vide pour " + entry.id);

    int dRow = "DOWN".equals(direction) ? 1 : 0;
    int dCol = "RIGHT".equals(direction) ? 1 : 0;

    for (int i = 0; i < answer.length(); i++) {
      int row = entry.startRow + (dRow * i);
      int col = entry.startCol + (dCol * i);
      require(inBounds(puzzle, row, col), "entrée hors grille pour " + entry.id + " à " + coord(row, col));

      ArrowwordJsonModels.CellV1 cell = puzzle.cells.get(row).get(col);
      require("LETTER".equals(normalize(cell.kind)), "case attendue lettre pour " + entry.id + " à " + coord(row, col));

      String actual = normalize(cell.solution);
      String expected = String.valueOf(answer.charAt(i));
      require(expected.equals(actual), "lettre incohérente pour " + entry.id + " à " + coord(row, col));

      usedLetterCoords.add(coord(row, col));
    }
  }

  private static boolean inBounds(ArrowwordJsonModels.PuzzleV1 puzzle, int row, int col) {
    return row >= 0 && col >= 0 && row < puzzle.rows && col < puzzle.cols;
  }

  private static boolean hasClueText(ArrowwordJsonModels.CellV1 cell) {
    if (!normalize(cell.clue).isEmpty()) {
      return true;
    }
    if (cell.clues == null || cell.clues.isEmpty()) {
      return false;
    }
    for (String value : cell.clues.values()) {
      if (!normalize(value).isEmpty()) {
        return true;
      }
    }
    return false;
  }

  private static String normalize(String value) {
    return String.valueOf(value == null ? "" : value).trim().toUpperCase(Locale.ROOT);
  }

  private static String coord(int row, int col) {
    return "(" + row + "," + col + ")";
  }

  private static void require(boolean condition, String message) {
    if (!condition) {
      throw new IllegalArgumentException(message);
    }
  }
}
