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
