package com.longoka.games.puzzles.wordsearch;

import java.util.Objects;

/**
 * A word that must be found in the puzzle, with optional metadata.
 * This is independent from DB entities (LexWord, LexVerb).
 */
public final class WordToFind {

  /** Base form used to generate the grid (e.g. "muntu"). */
  private final String baseForm;

  /** Form displayed to the user in the word list (can include accents, etc.). */
  private final String displayForm;

  /** Translation or short meaning, optionally null. */
  private final String translation;
  /** Optional phonetic transcription, if available. */
  private final String phonetic;

  /** Optional English translation, if available. */
  private final String translationEn;

  /** Optional slug (can be null) to link back to Lexikongo / Lexilingala. */
  private final String slug;

  /** Optional part of speech, e.g. "noun", "verb". */
  private final String partOfSpeech;

  /** Optional extra info, for example nominal class name. */
  private final String extraInfo;

  // Ancien constructeur (compat), on le garde
  public WordToFind(
      String baseForm,
      String displayForm,
      String translation,
      String slug,
      String partOfSpeech,
      String extraInfo) {
    // on délègue au constructeur complet avec phonetic + translationEn = null
    this(baseForm, displayForm, translation, slug, partOfSpeech, extraInfo, null, null);
  }

  // Nouveau constructeur complet
  public WordToFind(
      String baseForm,
      String displayForm,
      String translation,
      String slug,
      String partOfSpeech,
      String extraInfo,
      String phonetic,
      String translationEn) {

    this.baseForm = Objects.requireNonNull(baseForm, "baseForm must not be null");
    this.displayForm = displayForm != null ? displayForm : baseForm;
    this.translation = translation;
    this.slug = slug;
    this.partOfSpeech = partOfSpeech;
    this.extraInfo = extraInfo;
    this.phonetic = phonetic;
    this.translationEn = translationEn;
  }

  public String getBaseForm() {
    return baseForm;
  }

  public String getDisplayForm() {
    return displayForm;
  }

  public String getTranslation() {
    return translation;
  }

  public String getSlug() {
    return slug;
  }

  public String getPartOfSpeech() {
    return partOfSpeech;
  }

  public String getExtraInfo() {
    return extraInfo;
  }

  public String getPhonetic() {
    return phonetic;
  }

  public String getTranslationEn() {
    return translationEn;
  }

  @Override
  public String toString() {
    return "WordToFind{" +
        "baseForm='" + baseForm + '\'' +
        ", displayForm='" + displayForm + '\'' +
        ", translation='" + translation + '\'' +
        ", translationEn='" + translationEn + '\'' +
        ", slug='" + slug + '\'' +
        ", partOfSpeech='" + partOfSpeech + '\'' +
        ", extraInfo='" + extraInfo + '\'' +
        ", phonetic='" + phonetic + '\'' +
        '}';
  }

}
