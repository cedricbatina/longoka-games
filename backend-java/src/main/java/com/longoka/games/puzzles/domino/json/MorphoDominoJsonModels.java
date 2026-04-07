package com.longoka.games.puzzles.domino.json;

import java.util.List;
import java.util.Map;

/**
 * Modeles JSON pour les dominos morphologiques Longoka.
 * format = "longoka.morpho-domino.pack.v1"
 */
public final class MorphoDominoJsonModels {

  private MorphoDominoJsonModels() {
    // utilitaire
  }

  public static final class PackV1 {
    public String format = "longoka.morpho-domino.pack.v1";
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
    public List<TileV1> tiles;
    public List<String> solutionOrder;
    public Map<String, Object> meta;
  }

  public static final class TileV1 {
    public String id;
    public SideV1 left;
    public SideV1 right;
    public EntryRefV1 entryRef;
    public Map<String, Object> meta;
  }

  public static final class SideV1 {
    public String kind;
    public String value;
    public String display;
    public String normalized;
    public String label;
  }

  public static final class EntryRefV1 {
    public Integer wordId;
    public String slug;
    public String partOfSpeech;
    public String phonetic;
    public String extraInfo;
    public String singular;
    public String plural;
    public String root;
    public String translation;
    public String translationEn;
  }
}
