package com.longoka.games.puzzles.scrabble;

import com.longoka.games.puzzles.scrabble.json.ScrabbleLikeJsonModels;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Validation structurelle des packs scrabble-like.
 */
public final class ScrabbleLikeValidator {

  private ScrabbleLikeValidator() {
  }

  public static void validatePack(ScrabbleLikeJsonModels.PackV1 pack) {
    require(pack != null, "pack scrabble null");
    require("longoka.scrabble-like.pack.v1".equals(pack.format), "format scrabble invalide");
    require(pack.version == 1, "version scrabble invalide");
    require(pack.puzzles != null && !pack.puzzles.isEmpty(), "pack scrabble sans puzzles");
    for (ScrabbleLikeJsonModels.PuzzleV1 puzzle : pack.puzzles) {
      validatePuzzle(puzzle);
    }
  }

  public static void validatePuzzle(ScrabbleLikeJsonModels.PuzzleV1 puzzle) {
    require(puzzle != null, "puzzle scrabble null");
    require(puzzle.challenges != null && !puzzle.challenges.isEmpty(), "puzzle scrabble sans challenges");
    require(puzzle.layout != null && !puzzle.layout.isBlank(), "layout scrabble manquant");
    require(puzzle.challengeCount == null || puzzle.challengeCount == puzzle.challenges.size(),
        "challengeCount scrabble incoherent");

    int computedScore = 0;
    for (ScrabbleLikeJsonModels.ChallengeV1 challenge : puzzle.challenges) {
      validateChallenge(challenge);
      computedScore += challenge.score != null ? challenge.score : 0;
    }
    require(puzzle.totalScore == null || puzzle.totalScore == computedScore,
        "totalScore scrabble incoherent");
  }

  private static void validateChallenge(ScrabbleLikeJsonModels.ChallengeV1 challenge) {
    require(challenge != null, "challenge scrabble null");
    require(notBlank(challenge.id), "challenge scrabble sans id");
    require(notBlank(challenge.answer), "challenge scrabble sans answer");
    require(notBlank(challenge.normalized), "challenge scrabble sans normalized");
    require(challenge.slotCount != null && challenge.slotCount > 0, "slotCount scrabble invalide");
    require(challenge.slotCount == challenge.normalized.length(), "slotCount scrabble incoherent");
    require(challenge.score != null && challenge.score > 0, "score scrabble invalide");
    require(challenge.rack != null && !challenge.rack.isEmpty(), "challenge scrabble sans rack");

    List<String> rackLetters = new ArrayList<>();
    for (ScrabbleLikeJsonModels.TileV1 tile : challenge.rack) {
      require(tile != null, "tuile scrabble nulle");
      require(notBlank(tile.id), "tuile scrabble sans id");
      require(notBlank(tile.letter), "tuile scrabble sans lettre");
      require(tile.points != null && tile.points > 0, "points de tuile scrabble invalides");
      String normalized = normalizeLetters(tile.normalized != null ? tile.normalized : tile.letter);
      require(normalized.length() == 1, "tuile scrabble invalide");
      rackLetters.add(normalized);
    }

    List<String> answerLetters = splitLetters(challenge.normalized);
    Collections.sort(rackLetters);
    Collections.sort(answerLetters);
    require(rackLetters.equals(answerLetters), "rack scrabble incoherent avec la reponse");
  }

  private static List<String> splitLetters(String value) {
    String normalized = normalizeLetters(value);
    List<String> letters = new ArrayList<>();
    for (int index = 0; index < normalized.length(); index += 1) {
      letters.add(String.valueOf(normalized.charAt(index)));
    }
    return letters;
  }

  private static String normalizeLetters(String value) {
    if (value == null) {
      return "";
    }
    String folded = Normalizer.normalize(value, Normalizer.Form.NFD)
        .replaceAll("\\p{M}+", "")
        .toUpperCase();
    StringBuilder out = new StringBuilder();
    for (int index = 0; index < folded.length(); index += 1) {
      char ch = folded.charAt(index);
      if (ch >= 'A' && ch <= 'Z') {
        out.append(ch);
      }
    }
    return out.toString();
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
