package com.longoka.games.puzzles.arrowword.json;

import java.util.List;
import java.util.Map;

/**
 * Modèles JSON pour les packs Longoka de mots fléchés.
 * format = "longoka.arrowword.pack.v1"
 */
public final class ArrowwordJsonModels {

  private ArrowwordJsonModels() {
    // utilitaire
  }

  /**
   * Racine d'un pack de grilles de mots fléchés.
   */
  public static final class PackV1 {
    /** Identifiant du format. */
    public String format = "longoka.arrowword.pack.v1";

    /** Version du format. */
    public int version = 1;

    /** Langue principale des réponses, ex: "kg". */
    public String language;

    /** Identifiant fonctionnel du pack. */
    public String packId;

    /** Titre affiché. */
    public String title;

    /** Description courte. */
    public String description;

    /** Langue des indices / significations, ex: "fr". */
    public String meaningLanguage;

    /** Métadonnées libres au niveau pack. */
    public Map<String, Object> meta;

    /** Liste des grilles du pack. */
    public List<PuzzleV1> puzzles;
  }

  /**
   * Une grille de mots fléchés.
   */
  public static final class PuzzleV1 {
    public String id;
    public String language;
    public String mode;
    public String difficulty;
    public String title;
    public String theme;
    public int rows;
    public int cols;

    /**
     * Matrice orientée cellule.
     * kind = "block" | "letter" | "clue"
     */
    public List<List<CellV1>> cells;

    /** Entrées pilotées par les cases-indices. */
    public List<EntryV1> entries;

    /** Métadonnées libres. */
    public Map<String, Object> meta;
  }

  /**
   * Une cellule de la grille.
   */
  public static final class CellV1 {
    /** "block", "letter" ou "clue". */
    public String kind;

    /** Lettre solution si kind == "letter". */
    public String solution;

    /** Indice court simple si kind == "clue". */
    public String clue;

    /**
     * Variante structurée si une case porte plusieurs indices,
     * ex: { "RIGHT": "Demeure", "DOWN": "Route" }.
     */
    public Map<String, String> clues;

    /** Directions associées à la case-indice. */
    public List<String> arrows;

    /** Entrées pilotées par cette case-indice. */
    public List<String> entryIds;

    /** Métadonnées libres par cellule. */
    public Map<String, Object> meta;
  }

  /**
   * Une entrée répondant à une case-indice.
   */
  public static final class EntryV1 {
    /** Identifiant interne, ex: "1-R". */
    public String id;

    /** Direction principale: "RIGHT" ou "DOWN". */
    public String direction;

    /** Coordonnées de la case-indice (0-based). */
    public int clueRow;
    public int clueCol;

    /** Coordonnées de la première lettre (0-based). */
    public int startRow;
    public int startCol;

    /** Réponse normalisée telle qu'elle remplit la grille. */
    public String answer;

    /** Forme d'affichage / surface form. */
    public String display;

    /** Traduction / indice principal. */
    public String translation;

    /** Traduction anglaise éventuelle. */
    public String translationEn;

    /** Phonétique éventuelle. */
    public String phonetic;

    /** Slug lexical éventuel. */
    public String slug;

    /** Partie du discours. */
    public String partOfSpeech;

    /** Info morphologique complémentaire. */
    public String extraInfo;

    /** Tags sémantiques. */
    public List<String> semanticTags;
  }
}
