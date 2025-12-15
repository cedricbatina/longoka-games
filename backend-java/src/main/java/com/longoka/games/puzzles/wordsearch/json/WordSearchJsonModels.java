package com.longoka.games.puzzles.wordsearch.json;

import java.util.List;
import java.util.Map;

/**
 * DTO simples pour l'export JSON des mots mêlés.
 * Conçus pour être sérialisés avec Jackson.
 */
public final class WordSearchJsonModels {

  private WordSearchJsonModels() {
    // utilitaire, pas d'instances
  }

  /** Pack contenant plusieurs grilles. */
  public static final class PackV1 {
    /** Ex: "longoka.wordsearch.pack.v1" */
    public String schema = "longoka.wordsearch.pack.v1";

    /** Version du format (1 pour cette première version). */
    public int version = 1;

    /** Code langue des mots dans la grille, ex "kg", "ln". */
    public String language;

    /** Identifiant du pack, ex: "kg-mixed-auto-2025-12-12". */
    public String packId;

    /** Titre du pack. */
    public String title;

    /** Description du pack. */
    public String description;

    /** Langue des définitions / traductions, ex "fr". */
    public String meaningLanguage;

    /** Liste des grilles. */
    public List<PuzzleV1> puzzles;
  }

  /** Une grille individuelle de mots mêlés. */
  public static final class PuzzleV1 {
    /** Identifiant de la grille à l’intérieur du pack. */
    public String id;

    /** Code langue des mots dans la grille, ex "kg". */
    public String language;

    /** Mode : "nouns", "verbs" ou "mixed". */
    public String mode;

    /** Difficulté indicative : "easy", "medium", "hard", etc. */
    public String difficulty;

    /** Titre de la grille. */
    public String title;

    /** Thème éventuel (ex: "Noms + verbes"). */
    public String theme;

    /** Nombre de lignes. */
    public int rows;

    /** Nombre de colonnes. */
    public int cols;

    /**
     * Grille représentée comme une liste de lignes,
     * Chaque string est de longueur = cols, en majuscules.
     */
    public List<String> grid;

    /** Entrées (mots) à trouver, avec leurs métadonnées. */
    public List<EntryV1> entries;

    /** Placements (solution) sous forme de coordonnées. */
    public List<PlacementV1> placements;

    /** Métadonnées libres. */
    public Map<String, Object> meta;
  }

  /** Une entrée de la liste des mots à trouver. */
  /** Une entrée de la liste des mots à trouver. */
  public static final class EntryV1 {
    /** Forme de base, ex "tunga". */
    public String base;

    /** Forme affichée dans la liste (peut inclure accents). */
    public String display;

    /** Traduction / définition courte (FR pour l’instant). */
    public String translation;

    /** Slug pour lien vers Lexikongo / Lexilingala. */
    public String slug;

    /** Partie du discours, ex "noun", "verb". */
    public String partOfSpeech;

    /** Infos extra (classe nominale, etc.). */
    public String extraInfo;

    /** Transcription phonétique si disponible. */
    public String phonetic;

    /** Traduction anglaise si disponible. */
    public String translationEn;

    /**
     * Tags sémantiques dérivés des traductions (ex: "animal", "corps", "maison").
     */
    public java.util.List<String> semanticTags;
  }

  /** Position d’un mot dans la grille. */
  public static final class PlacementV1 {
    /** Mot tel qu’écrit dans la grille (upper-case, sans espaces). */
    public String word;

    /** Ligne de départ (0-based). */
    public int row;

    /** Colonne de départ (0-based). */
    public int col;

    /** Direction : RIGHT, DOWN, DIAGONAL_DOWN_RIGHT, DIAGONAL_UP_RIGHT. */
    public String direction;

    /** Longueur du mot dans la grille. */
    public int length;
  }
}
