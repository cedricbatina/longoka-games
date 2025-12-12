package com.longoka.games.lexikongo;

public class LexMeaning {

  private final int meaningId;
  private final String languageCode;
  private final String meaning;

  public LexMeaning(int meaningId, String languageCode, String meaning) {
    this.meaningId = meaningId;
    this.languageCode = languageCode;
    this.meaning = meaning;
  }

  /**
   * Constructeur de compatibilite pour l'ancien code
   * qui faisait new LexMeaning(lang, meaning).
   * On met meaningId = 0 comme valeur neutre.
   */
  public LexMeaning(String languageCode, String meaning) {
    this(0, languageCode, meaning);
  }

  public int getMeaningId() {
    return meaningId;
  }

  public String getLanguageCode() {
    return languageCode;
  }

  /**
   * Alias explicite pour le champ "meaning".
   * C'est cette methode que tes services appellent.
   */
  public String getMeaning() {
    return meaning;
  }

  /**
   * Compat possible si ailleurs on utilisait getText().
   */
  public String getText() {
    return meaning;
  }

  @Override
  public String toString() {
    return meaning;
  }
}
