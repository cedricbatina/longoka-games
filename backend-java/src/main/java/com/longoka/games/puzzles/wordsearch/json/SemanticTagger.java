package com.longoka.games.puzzles.wordsearch.json;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Déduit des tags sémantiques simples à partir des traductions FR (et plus tard
 * EN/PT).
 * Pour l’instant, on reste modeste et extensible.
 */
public final class SemanticTagger {

  private SemanticTagger() {
  }

  public static List<String> guessTags(String frTranslation) {
    List<String> tags = new ArrayList<>();
    if (frTranslation == null || frTranslation.isBlank()) {
      return tags;
    }

    String text = frTranslation.toLowerCase(Locale.ROOT);

    // Exemple de règles très simples. On enrichira plus tard.
    if (containsAny(text, "chien", "chat", "animal", "bête", "oiseau", "poisson", "léopard", "fourmi", "hyppopotame",
        "serpent", "singe", "abeille", "mouche", "moustique", "guêpe", "mille-pattes", "criquet", "sauterelle", "lion",
        "éléphant", "buffle", "biche", "antilope", "rat", "souris", "poule", "coq", "vache", "boeuf", "porc", "truie",
        "papillon", "larve", "araignée")) {
      tags.add("animal");
    }
    if (containsAny(text, "maison", "demeure", "habitation", "village", "case", "douche", "cuisine", "chaise", "table",
        "véranda", "balcon", "parcelle", "jardin", "toit", "mur", "terre", "air", "eau")) {
      tags.add("habitation");
    }
    if (containsAny(text, "tête", "cerveau", "main", "ventre", "estomac", "vésicule", "rein", "foie", "poumons",
        "doigt",
        "dent", "langue", "koto", "cou", "muscle", "veine", "cheveux", "lèvre", "pénis", "sexe féminin", "odorat",
        "pied", "corps", "coeur", "cœur")) {
      tags.add("corps humain");
    }
    if (containsAny(text, "manger", "boire", "nourriture", "repas", "aliment", "oeuf", "eau", "feu", "casserole",
        "marmite", "fourchette", "cuillère", "pain de manioc", "gombo", "oignon", "épinard", "légumes", "sel", "piment",
        "igname", "manioc", "safou")) {
      tags.add("alimentation");
    }
    if (containsAny(text, "parler", "dire", "répondre", "crier", "chuchoter")) {
      tags.add("parole");
    }
    if (containsAny(text, "penser", "réfléchir", "imaginer")) {
      tags.add("pensée");
    }
    if (containsAny(text, "travailler", "travail", "métier", "profession")) {
      tags.add("travail");
    }
    if (containsAny(text, "route", "chemin", "cheminement", "rue", "pays", "ville", "univers", "cosmos", "planète",
        "galaxie", "soleil", "mois", "année", "semaine", "jour", "hier", "avant-hier", "demain")) {
      tags.add("localisation");
    }
    // etc. tu pourras enrichir la liste quand tu voudras

    return tags;
  }

  private static boolean containsAny(String text, String... needles) {
    for (String n : needles) {
      if (text.contains(n)) {
        return true;
      }
    }
    return false;
  }
}
