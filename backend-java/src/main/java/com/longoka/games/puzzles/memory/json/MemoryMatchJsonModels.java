package com.longoka.games.puzzles.memory.json;

import java.util.List;
import java.util.Map;

/**
 * Modeles JSON pour les packs Longoka de memory-match.
 * format = "longoka.memory-match.pack.v1"
 */
public final class MemoryMatchJsonModels {

  private MemoryMatchJsonModels() {
    // utilitaire
  }

  public static final class PackV1 {
    public String format = "longoka.memory-match.pack.v1";
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
    public Integer rows;
    public Integer cols;
    public Integer pairCount;
    public List<CardV1> cards;
    public Map<String, Object> meta;
  }

  public static final class CardV1 {
    public String id;
    public String pairId;
    public String kind;
    public String value;
    public String display;
    public String normalized;
    public String label;
    public String hint;
    public EntryRefV1 entryRef;
    public Map<String, Object> meta;
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
