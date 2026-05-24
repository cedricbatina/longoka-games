package com.longoka.games.lexikongo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class BantuSyllableSplitterTest {

  @Test
  void splitsNtemoAsNteMo() {
    assertEquals(List.of("NTE", "MO"), BantuSyllableSplitter.splitNormalizedWord("NTEMO"));
    assertTrue(BantuSyllableSplitter.isValidOpenSyllable("NTE"));
    assertTrue(BantuSyllableSplitter.isValidOpenSyllable("MO"));
    assertFalse(BantuSyllableSplitter.isValidOpenSyllable("EMO"));
    assertFalse(BantuSyllableSplitter.isValidOpenSyllable("NT"));
  }

  @Test
  void splitsWithYAsConsonant() {
    assertEquals(List.of("MU", "YO", "KO", "LO"), BantuSyllableSplitter.splitNormalizedWord("MUYOKOLO"));
  }

  @Test
  void splitsNayaSequenceAsNaYa() {
    assertEquals(List.of("KU", "NA", "YA", "MI"), BantuSyllableSplitter.splitNormalizedWord("KUNAYAMI"));
  }

  @Test
  void splitsCompoundSurfaceFormOnSpaces() {
    assertEquals(
        List.of("KU", "NA", "YA", "MI"),
        BantuSyllableSplitter.splitSurfaceForm("Ku nayami"));
  }

  @Test
  void allowsCvvBeforeFollowingConsonant() {
    assertEquals(List.of("KIA", "ZI"), BantuSyllableSplitter.splitNormalizedWord("KIAZI"));
  }

  @Test
  void splitsKaniyamimundaLikeForms() {
    assertEquals(List.of("KA", "NI", "MU", "NDA"), BantuSyllableSplitter.splitNormalizedWord("KANIMUNDA"));
    assertEquals(List.of("KA", "NI", "MU", "NDA", "LA"), BantuSyllableSplitter.splitNormalizedWord("KANIMUNDALA"));
  }
}
