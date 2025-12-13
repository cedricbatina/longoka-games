package com.longoka.games.lexikongo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LexWord {

  private final int wordId;
  private final String singular;
  private final String plural;
  private final String root;
  private final NominalClass nominalClass;
  private final String phonetic;
  private final boolean derivedWord;
  private final Integer derivedFromWordId;
  private final Integer derivedFromVerbId;
  private final boolean approved;
  private final Integer userId;
  private final String numberVariability; // valeur brute de la colonne DB
  private final String slug;
  private final List<LexMeaning> meanings;

  public LexWord(
      int wordId,
      String singular,
      String plural,
      String root,
      NominalClass nominalClass,
      String phonetic,
      boolean derivedWord,
      Integer derivedFromWordId,
      Integer derivedFromVerbId,
      boolean approved,
      Integer userId,
      String numberVariability,
      String slug,
      List<LexMeaning> meanings) {
    this.wordId = wordId;
    this.singular = singular;
    this.plural = plural;
    this.root = root;
    this.nominalClass = nominalClass;
    this.phonetic = phonetic;
    this.derivedWord = derivedWord;
    this.derivedFromWordId = derivedFromWordId;
    this.derivedFromVerbId = derivedFromVerbId;
    this.approved = approved;
    this.userId = userId;
    this.numberVariability = numberVariability;
    this.slug = slug;
    // on garde la référence de la liste pour que le repository puisse la remplir
    this.meanings = (meanings != null) ? meanings : new ArrayList<>();
  }

  public int getWordId() {
    return wordId;
  }

  public String getSingular() {
    return singular;
  }

  public String getPlural() {
    return plural;
  }

  public String getRoot() {
    return root;
  }

  public NominalClass getNominalClass() {
    return nominalClass;
  }

  public String getPhonetic() {
    return phonetic;
  }

  public boolean isDerivedWord() {
    return derivedWord;
  }

  public Integer getDerivedFromWordId() {
    return derivedFromWordId;
  }

  public Integer getDerivedFromVerbId() {
    return derivedFromVerbId;
  }

  public boolean isApproved() {
    return approved;
  }

  public Integer getUserId() {
    return userId;
  }

  public String getNumberVariability() {
    return numberVariability;
  }

  public String getSlug() {
    return slug;
  }

  /**
   * Liste des significations associées au mot (dans la langue demandée).
   */
  public List<LexMeaning> getMeanings() {
    return Collections.unmodifiableList(meanings);
  }

  /**
   * Retourne les significations pour une langue donnée (ex: "fr", "en").
   */
  public List<LexMeaning> getMeaningsForLanguage(String languageCode) {
    if (languageCode == null || languageCode.isBlank()) {
      return Collections.emptyList();
    }
    List<LexMeaning> result = new ArrayList<>();
    for (LexMeaning m : meanings) {
      if (m == null) {
        continue;
      }
      if (languageCode.equalsIgnoreCase(m.getLanguageCode())) {
        result.add(m);
      }
    }
    return result;
  }

  /**
   * Concatène toutes les significations pour une langue donnée
   * en évitant les doublons exacts. Séparateur par défaut : " ; ".
   */
  public String joinMeaningsForLanguage(String languageCode, String separator) {
    List<LexMeaning> list = getMeaningsForLanguage(languageCode);
    if (list.isEmpty()) {
      return null;
    }
    java.util.LinkedHashSet<String> unique = new java.util.LinkedHashSet<>();
    for (LexMeaning m : list) {
      if (m == null)
        continue;
      String txt = m.getMeaning(); // ou m.getText() si besoin
      if (txt != null) {
        txt = txt.trim();
      }
      if (txt != null && !txt.isEmpty()) {
        unique.add(txt);
      }
    }
    if (unique.isEmpty()) {
      return null;
    }
    String sep = (separator != null && !separator.isEmpty()) ? separator : " ; ";
    return String.join(sep, unique);
  }

  /** Convenience : toutes les traductions FR fusionnées. */
  public String getFrenchMeaningsJoined() {
    return joinMeaningsForLanguage("fr", " ; ");
  }

  /** Convenience : toutes les traductions EN fusionnées. */
  public String getEnglishMeaningsJoined() {
    return joinMeaningsForLanguage("en", " ; ");
  }

  /**
   * Méthode utilisée par le repository pour remplir les meanings après coup.
   */
  void addMeaning(LexMeaning meaning) {
    this.meanings.add(meaning);
  }

  @Override
  public String toString() {
    String base = (singular != null && !singular.isEmpty())
        ? singular
        : (plural != null ? plural : ("#" + wordId));

    String cls = (nominalClass != null) ? nominalClass.getClassName() : "?";

    return "LexWord{" +
        "id=" + wordId +
        ", form='" + base + '\'' +
        ", class=" + cls +
        ", slug='" + slug + '\'' +
        '}';
  }
}
