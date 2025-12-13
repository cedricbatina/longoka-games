package com.longoka.games.puzzles.crossword.json;

import java.util.List;
import java.util.Map;

/**
 * Modèles JSON pour les packs de mots croisés Longoka.
 * format = "longoka.crossword.pack.v1"
 */
public final class CrosswordJsonModels {

 private CrosswordJsonModels() {
  // utilitaire
 }

 /**
  * Racine d'un pack de grilles de mots croisés.
  */
 public static final class PackV1 {
  /** Identifiant de format. */
  public String format = "longoka.crossword.pack.v1";

  /** Version du format. */
  public int version = 1;

  /** Langue principale des réponses (ex: "kg"). */
  public String language;

  /** Identifiant du pack (slug/date, etc.). */
  public String packId;

  /** Titre du pack. */
  public String title;

  /** Description courte. */
  public String description;

  /** Langue des définitions (indices), ex: "fr". */
  public String meaningLanguage;

  /** Liste des grilles. */
  public List<PuzzleV1> puzzles;
 }

 /**
  * Une grille de mots croisés.
  */
 public static final class PuzzleV1 {
  /** Id unique de la grille. */
  public String id;

  /** Langue principale (ex: "kg"). */
  public String language;

  /** Mode: "nouns", "verbs", "mixed", etc. */
  public String mode;

  /** Difficulté: "easy", "medium", etc. */
  public String difficulty;

  /** Titre pour la page. */
  public String title;

  /** Thème éventuel. */
  public String theme;

  /** Nombre de lignes. */
  public int rows;

  /** Nombre de colonnes. */
  public int cols;

  /**
   * Grille sous forme de lignes de texte :
   * - lettre A–Z pour une case remplie,
   * - '#' pour une case noire / bloc.
   */
  public List<String> grid;

  /** Définitions / réponses. */
  public List<EntryV1> entries;

  /** Meta : source, domains, etc. */
  public Map<String, Object> meta;
 }

 /**
  * Une définition + réponse dans la grille.
  */
 public static final class EntryV1 {
  /** Id interne (ex: "1-A", "2-D"). */
  public String id;

  /** Numéro affiché (1, 2, 3...). */
  public int number;

  /** Direction: "ACROSS" (horizontal) ou "DOWN" (vertical). */
  public String direction;

  /** Ligne de départ (0-based). */
  public int row;

  /** Colonne de départ (0-based). */
  public int col;

  /** Réponse en lettres (telle qu'elle apparaît dans la grille). */
  public String answer;

  /** Forme affichée du mot (Kikongo). */
  public String display;

  /** Indice en clair (FR) pour le joueur. */
  public String clue;

  /** Traduction (FR) structurée, souvent == clue. */
  public String translation;

  /** Traduction anglaise, si disponible. */
  public String translationEn;

  /** Transcription phonétique, si disponible. */
  public String phonetic;

  /** Slug Lexikongo, si disponible. */
  public String slug;

  /** Partie du discours: "noun", "verb", etc. */
  public String partOfSpeech;

  /** Infos extra (classe nominale, etc.). */
  public String extraInfo;

  /** Tags sémantiques, ex: "animal", "habitation". */
  public java.util.List<String> semanticTags;
 }
}
