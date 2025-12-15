package com.longoka.games.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.longoka.games.lexikongo.KikongoMixedWordSearchService;
import com.longoka.games.puzzles.wordsearch.WordPlacement;
import com.longoka.games.puzzles.wordsearch.WordSearchPuzzle;
import com.longoka.games.puzzles.wordsearch.WordToFind;
import com.longoka.games.puzzles.wordsearch.json.WordSearchJsonModels;

import com.longoka.games.puzzles.wordsearch.json.SemanticTagger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// AJOUTER :
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Tool CLI pour générer un pack JSON de mots mêlés
 * Kikongo (noms + verbes) à partir de la base Lexikongo.
 */
public final class KikongoMixedWordSearchJsonExportTool {

  private KikongoMixedWordSearchJsonExportTool() {
    // utilitaire
  }

  public static void main(String[] args) throws Exception {
    System.out.println("== Export JSON : Mots mêlés Kikongo (noms + verbes) ==");

    // Paramètres (tu peux les ajuster à la main ou plus tard les passer en args)
    int puzzleCount = 48; // nombre de grilles dans le pack
    int rows = 12;
    int cols = 12;
    int maxWords = 12;
    String meaningLanguageCode = "fr";
    // String outputPath = "target/kikongo-mixed-wordsearch-pack.v1.json";
    String outputPath = "target/kikongo-mixed-wordsearch-book.v2.json";

    KikongoMixedWordSearchService service = new KikongoMixedWordSearchService();
    List<WordSearchJsonModels.PuzzleV1> jsonPuzzles = new ArrayList<>();

    for (int i = 0; i < puzzleCount; i++) {
      System.out.println(" - Génération de la grille " + (i + 1) + "/" + puzzleCount + "...");
      WordSearchPuzzle puzzle = service.generateMixedKikongoWordSearch(
          rows,
          cols,
          maxWords,
          meaningLanguageCode);

      WordSearchJsonModels.PuzzleV1 jsonPuzzle = toJsonPuzzle(puzzle, "mixed", "easy", meaningLanguageCode, i + 1);
      jsonPuzzles.add(jsonPuzzle);
    }

    // Construction du pack
    WordSearchJsonModels.PackV1 pack = new WordSearchJsonModels.PackV1();
    pack.language = "kg";
    pack.packId = "kg-mixed-auto-" + LocalDate.now();
    pack.title = "Kikongo – mots mêlés (noms + verbes)";
    pack.description = "Pack généré automatiquement : "
        + puzzleCount + " grilles " + rows + "x" + cols + ".";
    pack.meaningLanguage = meaningLanguageCode;
    pack.puzzles = jsonPuzzles;

    // Sérialisation JSON avec Jackson
    ObjectMapper mapper = new ObjectMapper();
    mapper.enable(SerializationFeature.INDENT_OUTPUT); // pretty-print

    Path out = Path.of(outputPath);
    System.out.println("DEBUG: path absolu = " + out.toAbsolutePath());

    if (out.getParent() != null) {
      Files.createDirectories(out.getParent());
    }

    // debug existence AVANT écriture
    System.out.println("DEBUG: existe avant write ? " + out.toFile().exists());

    // écriture effective
    mapper.writeValue(out.toFile(), pack);

    // debug existence APRÈS écriture
    System.out.println("DEBUG: existe après write ? " + out.toFile().exists());

    System.out.println("✅ Export JSON terminé : " + out.toAbsolutePath());

    if (out.getParent() != null) {
      Files.createDirectories(out.getParent());
    }
    mapper.writeValue(out.toFile(), pack);

    System.out.println("✅ Export JSON terminé : " + out.toAbsolutePath());
  }

  private static WordSearchJsonModels.PuzzleV1 toJsonPuzzle(
      WordSearchPuzzle puzzle,
      String mode,
      String difficulty,
      String meaningLanguageCode,
      int index) {

    WordSearchJsonModels.PuzzleV1 json = new WordSearchJsonModels.PuzzleV1();

    // Id : on réutilise l'id interne si présent, sinon on fabrique un id simple.
    if (puzzle.getId() != null && !puzzle.getId().isBlank()) {
      json.id = puzzle.getId();
    } else {
      json.id = puzzle.getLanguageCode() + "-" + mode + "-" + index;
    }

    json.language = puzzle.getLanguageCode();
    json.mode = mode;
    json.difficulty = difficulty;
    json.title = puzzle.getTitle();
    json.theme = puzzle.getTheme();
    json.rows = puzzle.getRows();
    json.cols = puzzle.getCols();

    // Grille -> List<String>
    char[][] grid = puzzle.getGrid();
    List<String> gridLines = new ArrayList<>(grid.length);
    for (char[] row : grid) {
      gridLines.add(new String(row));
    }
    json.grid = gridLines;

    // === 1) ENTRIES : déduplication par base + fusion des traductions ===
    List<WordToFind> words = puzzle.getWords();
    Map<String, WordSearchJsonModels.EntryV1> entryByBase = new LinkedHashMap<>();

    if (words != null) {
      for (WordToFind w : words) {
        String base = w.getBaseForm();
        if (base == null || base.isBlank()) {
          base = w.getDisplayForm();
        }
        if (base == null || base.isBlank()) {
          // on ne sait pas identifier proprement ce mot -> on le skippe
          continue;
        }

        WordSearchJsonModels.EntryV1 existing = entryByBase.get(base);
        if (existing == null) {
          // première occurrence de ce base
          WordSearchJsonModels.EntryV1 e = new WordSearchJsonModels.EntryV1();
          e.base = base;
          e.display = w.getDisplayForm();
          e.translation = w.getTranslation();
          e.slug = w.getSlug();
          e.partOfSpeech = w.getPartOfSpeech();
          e.extraInfo = w.getExtraInfo();

          // 🔥 AJOUT : phonétique + EN
          e.phonetic = w.getPhonetic();
          e.translationEn = w.getTranslationEn();

          // tags sémantiques (on recalculera après fusion aussi)
          e.semanticTags = SemanticTagger.guessTags(e.translation);

          entryByBase.put(base, e);
        } else {
          // fusion ...

          // fusion : on garde une seule entrée pour ce base

          // 1) Traduction : on fusionne si différent
          String t = w.getTranslation();
          if (t != null && !t.isBlank()) {
            if (existing.translation == null || existing.translation.isBlank()) {
              existing.translation = t;
            } else if (!existing.translation.equals(t)) {
              existing.translation = existing.translation + " ; " + t;
            }
          }

          // 🔥 1bis) Traduction EN : même logique
          String en = w.getTranslationEn();
          if (en != null && !en.isBlank()) {
            if (existing.translationEn == null || existing.translationEn.isBlank()) {
              existing.translationEn = en;
            } else if (!existing.translationEn.equals(en)) {
              existing.translationEn = existing.translationEn + " ; " + en;
            }
          }

          // 2) slug : on garde le premier, mais si null on prend le suivant non-null
          if (existing.slug == null && w.getSlug() != null) {
            existing.slug = w.getSlug();
          }

          // 3) extraInfo (classe nominale, etc.) : premier non-null
          if (existing.extraInfo == null && w.getExtraInfo() != null) {
            existing.extraInfo = w.getExtraInfo();
          }

          // 4) partOfSpeech : idem, on remplit si vide
          if (existing.partOfSpeech == null && w.getPartOfSpeech() != null) {
            existing.partOfSpeech = w.getPartOfSpeech();
          }

          // 5) display : on garde la première forme, sauf si vraiment null
          if (existing.display == null && w.getDisplayForm() != null) {
            existing.display = w.getDisplayForm();
          }

          // 🔥 6) phonétique : on garde la première non nulle
          if (existing.phonetic == null && w.getPhonetic() != null && !w.getPhonetic().isBlank()) {
            existing.phonetic = w.getPhonetic();
          }

        }
      }
    }

    json.entries = new ArrayList<>(entryByBase.values());
    // Recalcule les semanticTags après fusion des traductions
    for (WordSearchJsonModels.EntryV1 e : json.entries) {
      e.semanticTags = SemanticTagger.guessTags(e.translation);
    }

    // === 2) PLACEMENTS : une seule occurrence par mot dans la grille ===
    List<WordSearchJsonModels.PlacementV1> placements = new ArrayList<>();
    if (puzzle.getPlacements() != null) {
      Set<String> seenWords = new LinkedHashSet<>();
      for (WordPlacement p : puzzle.getPlacements()) {
        String word = p.getWord();
        if (word == null || word.isBlank()) {
          continue;
        }
        String key = word.toUpperCase(); // normalisation simple

        // si on a déjà un placement pour ce mot, on ignore les suivants
        if (seenWords.contains(key)) {
          continue;
        }
        seenWords.add(key);

        WordSearchJsonModels.PlacementV1 jp = new WordSearchJsonModels.PlacementV1();
        jp.word = word;
        jp.row = p.getRow();
        jp.col = p.getCol();
        jp.direction = p.getDirection().name();
        jp.length = word.length();
        placements.add(jp);
      }
    }
    json.placements = placements;

    // === 3) META : source + langue + liste des classes nominales ===
    Map<String, Object> meta = new HashMap<>();
    meta.put("source", "lexikongo");
    meta.put("meaningLanguage", meaningLanguageCode);

    // on dérive les classes nominales à partir de extraInfo
    Set<String> nominalClasses = new LinkedHashSet<>();
    for (WordSearchJsonModels.EntryV1 e : json.entries) {
      if (e.extraInfo != null && !e.extraInfo.isBlank()) {
        nominalClasses.add(e.extraInfo);
      }
    }
    if (!nominalClasses.isEmpty()) {
      meta.put("nominalClasses", new ArrayList<>(nominalClasses));
    }
    // agrégat des domaines sémantiques à partir des semanticTags
    Set<String> semanticDomains = new LinkedHashSet<>();
    for (WordSearchJsonModels.EntryV1 e : json.entries) {
      if (e.semanticTags != null && !e.semanticTags.isEmpty()) {
        semanticDomains.addAll(e.semanticTags);
      }
    }
    if (!semanticDomains.isEmpty()) {
      meta.put("semanticDomains", new ArrayList<>(semanticDomains));
    }
    json.meta = meta;

    return json;
  }
}