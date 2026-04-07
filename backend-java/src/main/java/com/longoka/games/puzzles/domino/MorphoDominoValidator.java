package com.longoka.games.puzzles.domino;

import com.longoka.games.puzzles.domino.json.MorphoDominoJsonModels;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Validation structurelle des dominos morphologiques.
 */
public final class MorphoDominoValidator {

  private MorphoDominoValidator() {
    // utilitaire
  }

  public static void validatePack(MorphoDominoJsonModels.PackV1 pack) {
    require(pack != null, "pack null");
    require("longoka.morpho-domino.pack.v1".equals(pack.format), "format domino invalide");
    require(pack.version == 1, "version domino invalide");
    require(notBlank(pack.language), "language manquante");
    require(notBlank(pack.packId), "packId manquant");
    require(notBlank(pack.title), "title manquant");
    require(pack.puzzles != null && !pack.puzzles.isEmpty(), "pack sans puzzles");

    Set<String> seenIds = new HashSet<>();
    for (MorphoDominoJsonModels.PuzzleV1 puzzle : pack.puzzles) {
      validatePuzzle(puzzle);
      require(seenIds.add(puzzle.id), "puzzle duplique: " + puzzle.id);
    }
  }

  public static void validatePuzzle(MorphoDominoJsonModels.PuzzleV1 puzzle) {
    require(puzzle != null, "puzzle null");
    require(notBlank(puzzle.id), "puzzle id manquant");
    require(notBlank(puzzle.language), "puzzle language manquant");
    require(notBlank(puzzle.relationType), "relationType manquant");
    require(notBlank(puzzle.layout), "layout manquant");
    require(puzzle.tiles != null && !puzzle.tiles.isEmpty(), "puzzle sans tuiles");
    require(puzzle.solutionOrder != null && !puzzle.solutionOrder.isEmpty(), "solutionOrder manquant");

    Map<String, MorphoDominoJsonModels.TileV1> tilesById = new HashMap<>();
    for (MorphoDominoJsonModels.TileV1 tile : puzzle.tiles) {
      validateTile(tile);
      require(tilesById.putIfAbsent(tile.id, tile) == null, "tile dupliquee: " + tile.id);
    }

    require(puzzle.solutionOrder.size() == puzzle.tiles.size(),
        "solutionOrder doit couvrir toutes les tuiles");

    Set<String> seenInSolution = new HashSet<>();
    for (String tileId : puzzle.solutionOrder) {
      require(notBlank(tileId), "tile id vide dans solutionOrder");
      MorphoDominoJsonModels.TileV1 tile = tilesById.get(tileId);
      require(tile != null, "tile absente de solutionOrder: " + tileId);
      require(seenInSolution.add(tileId), "tile repetee dans solutionOrder: " + tileId);
    }

    validateChain(puzzle.solutionOrder, tilesById);
  }

  private static void validateTile(MorphoDominoJsonModels.TileV1 tile) {
    require(tile != null, "tile null");
    require(notBlank(tile.id), "tile id manquant");
    validateSide(tile.left, "left");
    validateSide(tile.right, "right");
    require(!tile.left.normalized.equals(tile.right.normalized),
        "tile degenerée (left == right): " + tile.id);
  }

  private static void validateSide(MorphoDominoJsonModels.SideV1 side, String sideName) {
    require(side != null, sideName + " side null");
    require(notBlank(side.kind), sideName + " kind manquant");
    require(notBlank(side.value), sideName + " value manquant");
    require(notBlank(side.display), sideName + " display manquant");
    require(notBlank(side.normalized), sideName + " normalized manquant");
  }

  private static void validateChain(
      List<String> solutionOrder,
      Map<String, MorphoDominoJsonModels.TileV1> tilesById) {

    for (int i = 0; i < solutionOrder.size() - 1; i++) {
      MorphoDominoJsonModels.TileV1 current = tilesById.get(solutionOrder.get(i));
      MorphoDominoJsonModels.TileV1 next = tilesById.get(solutionOrder.get(i + 1));
      require(current != null && next != null, "chaine domino invalide");
      require(current.right.normalized.equals(next.left.normalized),
          "chaine domino rompue entre " + current.id + " et " + next.id);
    }
  }

  private static boolean notBlank(String value) {
    return value != null && !value.isBlank();
  }

  private static void require(boolean condition, String message) {
    if (!condition) {
      throw new IllegalArgumentException(message);
    }
  }
}
