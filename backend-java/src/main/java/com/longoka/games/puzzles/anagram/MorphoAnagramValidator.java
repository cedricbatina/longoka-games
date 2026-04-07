package com.longoka.games.puzzles.anagram;

import com.longoka.games.puzzles.anagram.json.MorphoAnagramJsonModels;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Validation structurelle des packs d'anagrammes morphologiques.
 */
public final class MorphoAnagramValidator {

  private MorphoAnagramValidator() {
    // utilitaire
  }

  public static void validatePack(MorphoAnagramJsonModels.PackV1 pack) {
    require(pack != null, "pack anagram null");
    require("longoka.morpho-anagram.pack.v1".equals(pack.format), "format anagram invalide");
    require(pack.version == 1, "version anagram invalide");
    require(pack.puzzles != null && !pack.puzzles.isEmpty(), "pack anagram vide");
    for (MorphoAnagramJsonModels.PuzzleV1 puzzle : pack.puzzles) {
      validatePuzzle(puzzle);
    }
  }

  public static void validatePuzzle(MorphoAnagramJsonModels.PuzzleV1 puzzle) {
    require(puzzle != null, "puzzle anagram null");
    require(puzzle.challenges != null && !puzzle.challenges.isEmpty(), "puzzle anagram sans defis");
    require(intValue(puzzle.challengeCount, puzzle.challenges.size()) == puzzle.challenges.size(),
        "challengeCount anagram incoherent");

    int computedScore = 0;
    for (MorphoAnagramJsonModels.ChallengeV1 challenge : puzzle.challenges) {
      validateChallenge(challenge);
      computedScore += intValue(challenge.score, 0);
    }

    int totalScore = intValue(puzzle.totalScore, computedScore);
    require(totalScore == computedScore, "totalScore anagram incoherent");
  }

  private static void validateChallenge(MorphoAnagramJsonModels.ChallengeV1 challenge) {
    require(challenge != null, "defi anagram null");
    require(challenge.pieces != null && challenge.pieces.size() >= 2, "defi anagram sans pieces");
    require(challenge.solutionOrder != null && !challenge.solutionOrder.isEmpty(), "solution anagram vide");

    Map<String, MorphoAnagramJsonModels.PieceV1> piecesById = new LinkedHashMap<>();
    for (MorphoAnagramJsonModels.PieceV1 piece : challenge.pieces) {
      require(piece != null, "piece anagram null");
      require(notBlank(piece.id), "piece id manquant");
      require(notBlank(piece.text), "piece text manquant");
      require(notBlank(piece.normalized), "piece normalized manquant");
      require(!piecesById.containsKey(piece.id), "piece id duplique: " + piece.id);
      piecesById.put(piece.id, piece);
    }

    List<String> normalizedSegments = new ArrayList<>();
    for (String pieceId : challenge.solutionOrder) {
      require(notBlank(pieceId), "piece id vide dans la solution");
      MorphoAnagramJsonModels.PieceV1 piece = piecesById.get(pieceId);
      require(piece != null, "piece absente de la solution: " + pieceId);
      normalizedSegments.add(piece.normalized);
    }

    require(intValue(challenge.segmentCount, challenge.solutionOrder.size()) == challenge.solutionOrder.size(),
        "segmentCount anagram incoherent");

    String normalized = safe(challenge.normalized);
    require(notBlank(normalized), "normalized anagram manquant");
    require(String.join("", normalizedSegments).equals(normalized),
        "solution anagram incoherente pour " + safe(challenge.id));
  }

  private static int intValue(Integer value, int fallback) {
    return value == null ? fallback : value;
  }

  private static boolean notBlank(String value) {
    return value != null && !value.isBlank();
  }

  private static String safe(String value) {
    return value == null ? "" : value.trim();
  }

  private static void require(boolean condition, String message) {
    if (!condition) {
      throw new IllegalArgumentException(message);
    }
  }
}
