package com.longoka.games.puzzles.crossword;

import com.longoka.games.puzzles.crossword.json.CrosswordJsonModels;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Quality checks for generated crossword grids (web + InDesign + JSON contract).
 */
public final class CrosswordGridQuality {

  private CrosswordGridQuality() {
  }

  /**
   * All non-{@code #} cells must form one orthogonal component (American-style expectation;
   * avoids “floating” word islands in the UI and in print).
   */
  public static boolean whiteCellsOrthogonallyConnected(char[][] grid) {
    if (grid == null || grid.length == 0 || grid[0] == null) {
      return true;
    }
    int rows = grid.length;
    int cols = grid[0].length;
    int startR = -1;
    int startC = -1;
    int whiteCount = 0;
    for (int r = 0; r < rows; r++) {
      for (int c = 0; c < cols; c++) {
        if (grid[r][c] != '#') {
          whiteCount++;
          if (startR < 0) {
            startR = r;
            startC = c;
          }
        }
      }
    }
    if (whiteCount <= 1) {
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
      if (grid[r][c] == '#') {
        continue;
      }
      seen++;
      for (int k = 0; k < 4; k++) {
        int nr = r + dr[k];
        int nc = c + dc[k];
        if (nr < 0 || nr >= rows || nc < 0 || nc >= cols || vis[nr][nc]) {
          continue;
        }
        if (grid[nr][nc] == '#') {
          continue;
        }
        vis[nr][nc] = true;
        q.add(new int[] { nr, nc });
      }
    }
    return seen == whiteCount;
  }

  public static boolean whiteCellsOrthogonallyConnected(List<String> gridLines, int rows, int cols) {
    if (gridLines == null || gridLines.isEmpty()) {
      return true;
    }
    char[][] g = new char[rows][cols];
    for (int r = 0; r < rows; r++) {
      String line = r < gridLines.size() ? gridLines.get(r) : "";
      for (int c = 0; c < cols; c++) {
        g[r][c] = c < line.length() ? line.charAt(c) : '#';
      }
    }
    return whiteCellsOrthogonallyConnected(g);
  }

  public static boolean whiteCellsOrthogonallyConnected(CrosswordJsonModels.PuzzleV1 puzzle) {
    if (puzzle == null || puzzle.grid == null) {
      return true;
    }
    return whiteCellsOrthogonallyConnected(puzzle.grid, puzzle.rows, puzzle.cols);
  }
}
