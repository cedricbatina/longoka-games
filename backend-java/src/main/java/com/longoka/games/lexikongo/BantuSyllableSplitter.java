package com.longoka.games.lexikongo;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Découpe en syllabes ouvertes Bantu (CV, CCV, CVV, CCVV).
 * Y et W sont traités comme consonnes.
 */
public final class BantuSyllableSplitter {

  private BantuSyllableSplitter() {
  }

  public static List<String> splitSurfaceForm(String surfaceForm) {
    if (surfaceForm == null || surfaceForm.isBlank()) {
      return List.of();
    }
    List<String> tokens = splitTokens(surfaceForm);
    List<String> syllables = new ArrayList<>();
    for (String token : tokens) {
      String normalized = normalizeLetters(token);
      if (normalized.isBlank()) {
        continue;
      }
      syllables.addAll(splitNormalizedWord(normalized));
    }
    return syllables;
  }

  public static List<String> splitNormalizedWord(String normalized) {
    String word = normalizeLetters(normalized);
    if (word.length() < 2) {
      return word.isBlank() ? List.of() : List.of(word);
    }

    List<String> syllables = new ArrayList<>();
    int index = 0;
    while (index < word.length()) {
      StringBuilder chunk = new StringBuilder();
      int consonants = 0;
      while (index < word.length() && isConsonant(word.charAt(index)) && consonants < 2) {
        chunk.append(word.charAt(index));
        index += 1;
        consonants += 1;
      }

      if (index >= word.length()) {
        if (chunk.length() > 0) {
          appendTrailingConsonants(syllables, chunk.toString());
        }
        break;
      }

      if (!isVowel(word.charAt(index))) {
        chunk.append(word.charAt(index));
        index += 1;
        if (chunk.length() > 0) {
          syllables.add(chunk.toString());
        }
        continue;
      }

      chunk.append(word.charAt(index));
      index += 1;

      if (index < word.length() && isVowel(word.charAt(index))) {
        char nextAfterVowel = index + 1 < word.length() ? word.charAt(index + 1) : 0;
        if (nextAfterVowel != 0 && isConsonant(nextAfterVowel)) {
          chunk.append(word.charAt(index));
          index += 1;
        }
      }

      syllables.add(chunk.toString());
    }

    return mergeOrphanVowelSyllables(syllables);
  }

  public static boolean isValidOpenSyllable(String syllable) {
    if (syllable == null || syllable.isBlank()) {
      return false;
    }
    int index = 0;
    int consonants = 0;
    while (index < syllable.length() && isConsonant(syllable.charAt(index)) && consonants < 2) {
      index += 1;
      consonants += 1;
    }
    if (index >= syllable.length()) {
      return false;
    }
    int vowels = 0;
    while (index < syllable.length() && isVowel(syllable.charAt(index)) && vowels < 2) {
      index += 1;
      vowels += 1;
    }
    return index == syllable.length() && vowels >= 1;
  }

  public static boolean syllablesMatchWord(List<String> syllables, String normalizedWord) {
    if (syllables == null || syllables.isEmpty() || normalizedWord == null) {
      return false;
    }
    String joined = String.join("", syllables);
    return normalizeLetters(normalizedWord).equals(joined);
  }

  public static List<String> splitTokens(String surfaceForm) {
    String[] raw = surfaceForm.trim().split("[\\s·/|]+");
    List<String> tokens = new ArrayList<>();
    for (String part : raw) {
      if (part == null) {
        continue;
      }
      String trimmed = part.trim().replaceAll("^[\\p{Punct}&&[^-]]+|[\\p{Punct}&&[^-]]+$", "");
      if (!trimmed.isBlank()) {
        tokens.add(trimmed);
      }
    }
    return tokens.isEmpty() ? List.of(surfaceForm.trim()) : tokens;
  }

  static String normalizeLetters(String value) {
    if (value == null) {
      return "";
    }
    String folded = Normalizer.normalize(value, Normalizer.Form.NFD)
        .replaceAll("\\p{M}+", "")
        .toUpperCase(Locale.ROOT);
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < folded.length(); i++) {
      char ch = folded.charAt(i);
      if (ch >= 'A' && ch <= 'Z') {
        sb.append(ch);
      }
    }
    return sb.toString();
  }

  private static void appendTrailingConsonants(List<String> syllables, String trailing) {
    if (trailing.isBlank()) {
      return;
    }
    if (syllables.isEmpty()) {
      syllables.add(trailing);
      return;
    }
    int last = syllables.size() - 1;
    syllables.set(last, syllables.get(last) + trailing);
  }

  private static List<String> mergeOrphanVowelSyllables(List<String> syllables) {
    if (syllables.size() < 2) {
      return syllables;
    }
    List<String> merged = new ArrayList<>();
    for (String syllable : syllables) {
      if (syllable == null || syllable.isBlank()) {
        continue;
      }
      if (!merged.isEmpty() && isVowelOnly(syllable)) {
        int last = merged.size() - 1;
        merged.set(last, merged.get(last) + syllable);
      } else {
        merged.add(syllable);
      }
    }
    return merged;
  }

  private static boolean isVowelOnly(String syllable) {
    if (syllable == null || syllable.isBlank()) {
      return false;
    }
    for (int i = 0; i < syllable.length(); i++) {
      if (!isVowel(syllable.charAt(i))) {
        return false;
      }
    }
    return true;
  }

  static boolean isVowel(char value) {
    char letter = Character.toUpperCase(value);
    return letter == 'A'
        || letter == 'E'
        || letter == 'I'
        || letter == 'O'
        || letter == 'U';
  }

  static boolean isConsonant(char value) {
    return Character.isLetter(value) && !isVowel(value);
  }
}
