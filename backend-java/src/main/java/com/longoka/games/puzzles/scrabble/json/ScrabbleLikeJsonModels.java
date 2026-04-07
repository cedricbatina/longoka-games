package com.longoka.games.puzzles.scrabble.json;

import java.util.List;
import java.util.Map;

/**
 * Modeles JSON pour les packs Longoka de scrabble-like.
 * format = "longoka.scrabble-like.pack.v1"
 */
public final class ScrabbleLikeJsonModels {

  private ScrabbleLikeJsonModels() {
    // utilitaire
  }

  public static final class PackV1 {
    public String format = "longoka.scrabble-like.pack.v1";
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
    public Integer slotCount;
    public Integer score;
    public List<TileV1> rack;
    public EntryRefV1 entryRef;
    public Map<String, Object> meta;
  }

  public static final class TileV1 {
    public String id;
    public String letter;
    public String normalized;
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
