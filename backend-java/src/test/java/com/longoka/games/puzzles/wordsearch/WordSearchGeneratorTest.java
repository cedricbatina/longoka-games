package com.longoka.games.puzzles.wordsearch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

class WordSearchGeneratorTest {

  @Test
  void returnsOnlyWordsThatAreActuallyPlaced() {
    WordSearchGenerator generator = new WordSearchGenerator(new Random(42));

    List<WordToFind> inputWords = List.of(
        new WordToFind("ABCDEFGHIJK", "ABCDEFGHIJK", "long", null, "noun", null),
        new WordToFind("MOTO", "MOTO", "engine", null, "noun", null),
        new WordToFind("NZO", "NZO", "house", null, "noun", null),
        new WordToFind("MBWA", "MBWA", "dog", null, "noun", null));

    WordSearchPuzzle puzzle = generator.generatePuzzle(
        "kg",
        "test",
        "theme",
        8,
        8,
        inputWords);

    Set<String> placed = new HashSet<>();
    for (WordPlacement placement : puzzle.getPlacements()) {
      placed.add(placement.getWord());
    }

    assertFalse(puzzle.getWords().isEmpty());
    for (WordToFind word : puzzle.getWords()) {
      assertTrue(placed.contains(normalize(word.getBaseForm())));
    }

    // Long word cannot fit in 8x8, so it must not be exposed as a target word.
    List<String> exposed = new ArrayList<>();
    for (WordToFind word : puzzle.getWords()) {
      exposed.add(normalize(word.getBaseForm()));
    }
    assertFalse(exposed.contains("ABCDEFGHIJK"));
  }

  @Test
  void handlesNullOrBlankBaseFormsWithoutCrashing() {
    WordSearchGenerator generator = new WordSearchGenerator(new Random(7));

    List<WordToFind> inputWords = List.of(
        new WordToFind("MUNTU", "MUNTU", "person", null, "noun", null),
        new WordToFind(" ", " ", "blank", null, "noun", null));

    WordSearchPuzzle puzzle = generator.generatePuzzle(
        "kg",
        "null-safe",
        "theme",
        10,
        10,
        inputWords);

    assertNotNull(puzzle.getGrid());
    assertEquals(10, puzzle.getGrid().length);
  }

  private static String normalize(String baseForm) {
    if (baseForm == null) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < baseForm.length(); i++) {
      char ch = baseForm.charAt(i);
      if (Character.isLetter(ch)) {
        sb.append(Character.toUpperCase(ch));
      }
    }
    return sb.toString();
  }
}
