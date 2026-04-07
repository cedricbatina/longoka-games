package com.longoka.games.lexikongo;

import com.longoka.games.puzzles.arrowword.ArrowwordFromCrosswordConverter;
import com.longoka.games.puzzles.arrowword.json.ArrowwordJsonModels;
import com.longoka.games.puzzles.crossword.json.CrosswordJsonModels;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Générateur de mots fléchés Kikongo basé sur le moteur crossword existant.
 *
 * On part d'une vraie grille de mots croisés pour conserver :
 * - les croisements
 * - la cohérence lexicale
 * - un socle unique entre web et print
 *
 * Puis on convertit cette grille en format orienté "clue cells".
 */
public final class KikongoArrowwordService {

  private final KikongoCrosswordService crosswordService = new KikongoCrosswordService();
  private static final int DEFAULT_ATTEMPTS = 6;

  public ArrowwordJsonModels.PuzzleV1 generateArrowword(
    String mode,
    int rows,
    int cols,
    int maxEntries,
    String meaningLanguage,
    int index
  ) throws SQLException {
    if (rows < 6 || cols < 6) {
      throw new IllegalArgumentException("Une grille arrowword nécessite au moins 6x6.");
    }

    ArrowwordJsonModels.PuzzleV1 puzzle = null;
    CrosswordJsonModels.PuzzleV1 bestCrossword = null;
    int attempts = Math.max(3, Math.min(DEFAULT_ATTEMPTS, maxEntries));

    for (int attempt = 0; attempt < attempts; attempt++) {
      CrosswordJsonModels.PuzzleV1 candidateCrossword = crosswordService.generateCrossword(
        mode,
        rows - 1,
        cols - 1,
        maxEntries,
        meaningLanguage,
        index + (attempt * 1000)
      );
      ArrowwordJsonModels.PuzzleV1 candidate = ArrowwordFromCrosswordConverter.convertPuzzle(candidateCrossword, meaningLanguage);

      if (isBetterArrowwordCandidate(candidate, puzzle)) {
        bestCrossword = candidateCrossword;
        puzzle = candidate;
      }

      if (countEntries(candidate) >= maxEntries) {
        break;
      }
    }

    if (puzzle == null) {
      CrosswordJsonModels.PuzzleV1 crossword = crosswordService.generateCrossword(
        mode,
        rows - 1,
        cols - 1,
        maxEntries,
        meaningLanguage,
        index
      );
      bestCrossword = crossword;
      puzzle = ArrowwordFromCrosswordConverter.convertPuzzle(crossword, meaningLanguage);
    }

    Map<String, Object> meta = puzzle.meta != null ? new LinkedHashMap<>(puzzle.meta) : new LinkedHashMap<>();
    meta.put("source", "lexikongo");
    meta.put("generator", "KikongoArrowwordService");
    meta.put("generationMode", "crossword-derived");
    meta.put("meaningLanguage", meaningLanguage);
    meta.put("sourceCrosswordId", bestCrossword != null ? bestCrossword.id : null);
    meta.put("entryCount", countEntries(puzzle));
    puzzle.meta = meta;

    return puzzle;
  }

  private boolean isBetterArrowwordCandidate(
    ArrowwordJsonModels.PuzzleV1 candidate,
    ArrowwordJsonModels.PuzzleV1 currentBest
  ) {
    if (candidate == null) {
      return false;
    }
    if (currentBest == null) {
      return true;
    }

    int candidateEntries = countEntries(candidate);
    int currentEntries = countEntries(currentBest);
    if (candidateEntries != currentEntries) {
      return candidateEntries > currentEntries;
    }

    return countClueCells(candidate) > countClueCells(currentBest);
  }

  private int countEntries(ArrowwordJsonModels.PuzzleV1 puzzle) {
    return puzzle != null && puzzle.entries != null ? puzzle.entries.size() : 0;
  }

  private int countClueCells(ArrowwordJsonModels.PuzzleV1 puzzle) {
    if (puzzle == null || puzzle.cells == null) {
      return 0;
    }

    int count = 0;
    for (var row : puzzle.cells) {
      if (row == null) {
        continue;
      }
      for (var cell : row) {
        if (cell != null && "clue".equalsIgnoreCase(String.valueOf(cell.kind))) {
          count++;
        }
      }
    }
    return count;
  }
}
