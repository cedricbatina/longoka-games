package com.longoka.games.puzzles.anagram.json;

import java.util.List;
import java.util.Map;

/**
 * Modeles JSON pour les packs Longoka d'anagrammes morphologiques.
 * format = "longoka.morpho-anagram.pack.v1"
 */
public final class MorphoAnagramJsonModels {

  private MorphoAnagramJsonModels() {
    // utilitaire
  }

  public static final class PackV1 {
    public String format = "longoka.morpho-anagram.pack.v1";
    public int version = 1;
    public String language;
    public String packId;
    public String title;
    public String description;
    public String meaningLanguage;
    public Map<String, Object> meta;
    public List<PuzzleV1> puzzles;
  }

  public static final class PuzzleV1 {
    public String id;
    public String language;
    public String mode;
    public String difficulty;
    public String title;
    public String theme;
    public String relationType;
    public String layout;
    public Integer challengeCount;
    public Integer totalScore;
    public List<ChallengeV1> challenges;
    public Map<String, Object> meta;
  }

  public static final class ChallengeV1 {
    public String id;
    public String kind;
    public String answer;
    public String display;
    public String normalized;
    public String translation;
    public String translationEn;
    public String phonetic;
    public String label;
    public Integer segmentCount;
    public Integer score;
    public List<PieceV1> pieces;
    public List<String> solutionOrder;
    public EntryRefV1 entryRef;
    public Map<String, Object> meta;
  }

  public static final class PieceV1 {
    public String id;
    public String text;
    public String normalized;
    public String role;
    public Integer points;
  }

  public static final class EntryRefV1 {
    public String slug;
    public String partOfSpeech;
    public String phonetic;
    public String extraInfo;
    public String translation;
    public String translationEn;
    public String root;
    public String singular;
    public String plural;
  }
}
