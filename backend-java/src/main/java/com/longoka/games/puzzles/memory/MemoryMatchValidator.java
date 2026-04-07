package com.longoka.games.puzzles.memory;

import com.longoka.games.puzzles.memory.json.MemoryMatchJsonModels;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Validation structurelle des packs memory-match.
 */
public final class MemoryMatchValidator {

  private MemoryMatchValidator() {
    // utilitaire
  }

  public static void validatePack(MemoryMatchJsonModels.PackV1 pack) {
    require(pack != null, "pack null");
    require("longoka.memory-match.pack.v1".equals(pack.format), "format memory invalide");
    require(pack.version == 1, "version memory invalide");
    require(notBlank(pack.language), "language manquante");
    require(notBlank(pack.packId), "packId manquant");
    require(notBlank(pack.title), "title manquant");
    require(pack.puzzles != null && !pack.puzzles.isEmpty(), "pack sans puzzles");

    Set<String> seenIds = new HashSet<>();
    for (MemoryMatchJsonModels.PuzzleV1 puzzle : pack.puzzles) {
      validatePuzzle(puzzle);
      require(seenIds.add(puzzle.id), "puzzle duplique: " + puzzle.id);
    }
  }

  public static void validatePuzzle(MemoryMatchJsonModels.PuzzleV1 puzzle) {
    require(puzzle != null, "puzzle null");
    require(notBlank(puzzle.id), "puzzle id manquant");
    require(notBlank(puzzle.language), "puzzle language manquant");
    require(notBlank(puzzle.layout), "layout manquant");
    require(notBlank(puzzle.relationType), "relationType manquant");
    require(puzzle.cards != null && !puzzle.cards.isEmpty(), "puzzle sans cartes");
    require((puzzle.cards.size() % 2) == 0, "memory: nombre de cartes impair");

    Map<String, Integer> pairCounts = new HashMap<>();
    Set<String> seenCardIds = new HashSet<>();

    for (MemoryMatchJsonModels.CardV1 card : puzzle.cards) {
      validateCard(card);
      require(seenCardIds.add(card.id), "carte dupliquee: " + card.id);
      pairCounts.put(card.pairId, pairCounts.getOrDefault(card.pairId, 0) + 1);
    }

    for (Map.Entry<String, Integer> entry : pairCounts.entrySet()) {
      require(entry.getValue() == 2, "pairId invalide (doit avoir 2 cartes): " + entry.getKey());
    }

    int expectedPairCount = pairCounts.size();
    require(puzzle.pairCount == null || puzzle.pairCount == expectedPairCount,
        "pairCount incoherent");

    if (puzzle.rows != null && puzzle.cols != null) {
      require(puzzle.rows > 0 && puzzle.cols > 0, "rows/cols invalides");
      require((puzzle.rows * puzzle.cols) >= puzzle.cards.size(),
          "rows*cols insuffisant pour les cartes");
    }
  }

  private static void validateCard(MemoryMatchJsonModels.CardV1 card) {
    require(card != null, "card null");
    require(notBlank(card.id), "card id manquant");
    require(notBlank(card.pairId), "pairId manquant");
    require(notBlank(card.kind), "kind manquant");
    require(notBlank(card.value), "value manquant");
    require(notBlank(card.display), "display manquant");
    require(notBlank(card.normalized), "normalized manquant");
    require(notBlank(card.label), "label manquant");
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
