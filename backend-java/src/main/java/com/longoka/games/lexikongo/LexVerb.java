package com.longoka.games.lexikongo;

import java.util.ArrayList;
import java.util.List;

public class LexVerb {

  private final int verbId;
  private final String name; // infinitif (ex: "kudia")
  private final String root; // racine si différente
  private final String suffix; // suffixe éventuel
  private final String phonetic; // transcription phonétique éventuelle
  private final boolean active;
  private final boolean derived;
  private final Integer derivedFromVerbId;
  private final boolean approved;
  private final Integer userId;
  private final String slug;

  private final List<LexMeaning> meanings = new ArrayList<>();

  public LexVerb(
      int verbId,
      String name,
      String root,
      String suffix,
      String phonetic,
      boolean active,
      boolean derived,
      Integer derivedFromVerbId,
      boolean approved,
      Integer userId,
      String slug,
      List<LexMeaning> initialMeanings) {
    this.verbId = verbId;
    this.name = name;
    this.root = root;
    this.suffix = suffix;
    this.phonetic = phonetic;
    this.active = active;
    this.derived = derived;
    this.derivedFromVerbId = derivedFromVerbId;
    this.approved = approved;
    this.userId = userId;
    this.slug = slug;

    if (initialMeanings != null) {
      this.meanings.addAll(initialMeanings);
    }
  }

  public int getVerbId() {
    return verbId;
  }

  public String getName() {
    return name;
  }

  public String getRoot() {
    return root;
  }

  public String getSuffix() {
    return suffix;
  }

  public String getPhonetic() {
    return phonetic;
  }

  public boolean isActive() {
    return active;
  }

  public boolean isDerived() {
    return derived;
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

  public String getSlug() {
    return slug;
  }

  public List<LexMeaning> getMeanings() {
    return meanings;
  }

  /** Permet à un repository d'ajouter un sens. */
  public void addMeaning(LexMeaning meaning) {
    if (meaning != null) {
      this.meanings.add(meaning);
    }
  }

  /**
   * Retourne les significations pour une langue donnée (ex: "fr", "en").
   */
  public List<LexMeaning> getMeaningsForLanguage(String languageCode) {
    if (languageCode == null || languageCode.isBlank()) {
      return java.util.Collections.emptyList();
    }
    java.util.List<LexMeaning> result = new java.util.ArrayList<>();
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
    java.util.List<LexMeaning> list = getMeaningsForLanguage(languageCode);
    if (list.isEmpty()) {
      return null;
    }
    java.util.LinkedHashSet<String> unique = new java.util.LinkedHashSet<>();
    for (LexMeaning m : list) {
      if (m == null)
        continue;
      String txt = m.getMeaning(); // ou m.getText()
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

  public String getFrenchMeaningsJoined() {
    return joinMeaningsForLanguage("fr", " ; ");
  }

  public String getEnglishMeaningsJoined() {
    return joinMeaningsForLanguage("en", " ; ");
  }

  /** Forme à utiliser dans la grille (par défaut : name, sinon root). */
  public String getGridForm() {
    if (name != null && !name.isBlank()) {
      return name;
    }
    if (root != null && !root.isBlank()) {
      return root;
    }
    return null;
  }

  @Override
  public String toString() {
    return "LexVerb{" +
        "id=" + verbId +
        ", name='" + name + '\'' +
        ", root='" + root + '\'' +
        ", slug='" + slug + '\'' +
        '}';
  }
}
