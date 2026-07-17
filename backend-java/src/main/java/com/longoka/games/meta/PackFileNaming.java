package com.longoka.games.meta;

/**
 * Convention nommage fichiers packs exportés (Java → sync Longoka).
 *
 * <p>Le champ JSON {@code format}/{@code schema} interne reste {@code *.pack.v1}
 * (version de schéma). Seul le <strong>nom de fichier</strong> utilise le semver
 * logiciel {@value #FILE_VERSION} — décision Cédric 2026-07-17.
 */
public final class PackFileNaming {

  /** Version semver des fichiers exportés (pas le suffixe {@code .v1}). */
  public static final String FILE_VERSION = "1.0.0";

  private PackFileNaming() {}

  /**
   * Ex. {@code kg-mixed-verbs-nouns-singular-wordsearch-pack-1.0.0.json}
   *
   * @param languageCode ex. {@code kg}, {@code ln}
   * @param profileToken ex. {@code mixed-verbs-nouns-singular--20260716-semaine-app-fr}
   * @param puzzleKind ex. {@code wordsearch}, {@code arrowword}
   */
  public static String packFilename(String languageCode, String profileToken, String puzzleKind) {
    return languageCode
        + "-"
        + profileToken
        + "-"
        + puzzleKind
        + "-pack-"
        + FILE_VERSION
        + ".json";
  }
}
