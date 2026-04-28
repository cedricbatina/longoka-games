package com.longoka.games.meta;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Méta « sens lexique / pluriel » pour les JSON de pack livre, alignée sur Longoka
 * ({@code server/utils/learnerGamesPacks.js} et {@code scripts/backfill-games-book-meta.mjs}).
 */
public final class PackMeaningMeta {

  private static final Pattern PLURAL_HINT =
      Pattern.compile(
          "\\bpluriel|\\bpluriels\\b|\\bplurals?\\b|_plural\\b|nouns-plural|nouns-sing-or-plur|mixed-verbs-nouns-plural|nominal-class-plural|singular-to-plural|plural-to-singular",
          Pattern.CASE_INSENSITIVE);

  private static final Pattern SING_PLUR_PHRASE =
      Pattern.compile(
          "singulier.*pluriel|pluriel.*singulier|singular.*plural|plural.*singular",
          Pattern.CASE_INSENSITIVE);

  private PackMeaningMeta() {}

  /** Défaut Longoka : gloses FR/EN = entrées lexique au singulier (DB) si non précisé. */
  public static boolean resolveTranslationsSingularOnly(Boolean explicit) {
    if (explicit == null) {
      return true;
    }
    return explicit;
  }

  /** Si {@code explicit} est non nul, il prime ; sinon inférence comme le serveur. */
  public static boolean resolveIncludesPluralGameForms(
      Boolean explicit,
      String numberPolicy,
      String morphologyProfile,
      String relationType,
      String lexicalProfile,
      String profileId,
      String profileLabel,
      String title,
      String description,
      String packId) {
    if (explicit != null) {
      return explicit;
    }
    return inferIncludesPluralGameForms(
        numberPolicy,
        morphologyProfile,
        relationType,
        lexicalProfile,
        profileId,
        profileLabel,
        title,
        description,
        packId);
  }

  public static boolean inferIncludesPluralGameForms(
      String numberPolicy,
      String morphologyProfile,
      String relationType,
      String lexicalProfile,
      String profileId,
      String profileLabel,
      String title,
      String description,
      String packId) {
    String text =
        join(
            numberPolicy,
            morphologyProfile,
            relationType,
            lexicalProfile,
            profileId,
            profileLabel,
            title,
            description,
            packId);
    if (PLURAL_HINT.matcher(text).find()) {
      return true;
    }
    return SING_PLUR_PHRASE.matcher(text).find();
  }

  private static String join(String... parts) {
    StringBuilder sb = new StringBuilder();
    for (String p : parts) {
      if (p != null && !p.isEmpty()) {
        sb.append(p).append(' ');
      }
    }
    return sb.toString();
  }

  public static boolean defaultTranslationsSingularOnly() {
    return true;
  }

  /** Chaîne d’inférence (tests / debug). */
  public static String canonicalInferenceText(
      String numberPolicy,
      String morphologyProfile,
      String relationType,
      String lexicalProfile,
      String profileId,
      String profileLabel,
      String title,
      String description,
      String packId) {
    return join(
        Objects.toString(numberPolicy, ""),
        Objects.toString(morphologyProfile, ""),
        Objects.toString(relationType, ""),
        Objects.toString(lexicalProfile, ""),
        Objects.toString(profileId, ""),
        Objects.toString(profileLabel, ""),
        Objects.toString(title, ""),
        Objects.toString(description, ""),
        Objects.toString(packId, ""));
  }
}
