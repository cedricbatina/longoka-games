package com.longoka.games.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.longoka.games.lexikongo.KikongoWordSearchService;
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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Tool CLI pour générer un pack JSON de mots mêlés
 * Kikongo (noms uniquement) à partir de la base Lexikongo.
 */
public final class KikongoNounWordSearchJsonExportTool {

  private KikongoNounWordSearchJsonExportTool() {
    // utilitaire
  }

  public static void main(String[] args) throws Exception {
    System.out.println("== Export JSON : Mots mêlés Kikongo (noms) ==");

    int puzzleCount = 48; // nombre de grilles dans le pack
    int rows = 12;
    int cols = 12;
    int maxWords = 12;
    String meaningLanguageCode = "fr";
    String outputPath = "target/kikongo-nouns-wordsearch-book.v4.json";

    KikongoWordSearchService service = new KikongoWordSearchService();
    List<WordSearchJsonModels.PuzzleV1> jsonPuzzles = new ArrayList<>();

    for (int i = 0; i < puzzleCount; i++) {
      System.out.println(" - Génération de la grille " + (i + 1) + "/" + puzzleCount + "...");
      WordSearchPuzzle puzzle = service.generateRandomKikongoWordSearch(
          rows,
          cols,
          maxWords,
          meaningLanguageCode);

      WordSearchJsonModels.PuzzleV1 jsonPuzzle = toJsonPuzzle(puzzle, "nouns", "easy", meaningLanguageCode, i + 1);
      jsonPuzzles.add(jsonPuzzle);
    }

    // Construction du pack
    WordSearchJsonModels.PackV1 pack = new WordSearchJsonModels.PackV1();
    pack.language = "kg";
    pack.packId = "kg-nouns-auto-" + LocalDate.now();
    pack.title = "Kikongo – mots mêlés (noms)";
    pack.description = "Pack généré automatiquement : "
        + puzzleCount + " grilles " + rows + "x" + cols + " (noms seulement).";
    pack.meaningLanguage = meaningLanguageCode;
    pack.puzzles = jsonPuzzles;

    // Sérialisation JSON avec Jackson
    ObjectMapper mapper = new ObjectMapper();
    mapper.enable(SerializationFeature.INDENT_OUTPUT); // pretty-print

    Path out = Path.of(outputPath);
    if (out.getParent() != null) {
      Files.createDirectories(out.getParent());
    }

    mapper.writeValue(out.toFile(), pack);

    System.out.println("✅ Export JSON (noms) terminé : " + out.toAbsolutePath());
  }

  private static WordSearchJsonModels.PuzzleV1 toJsonPuzzle(
      WordSearchPuzzle puzzle,
      String mode,
      String difficulty,
      String meaningLanguageCode,
      int index) {

    WordSearchJsonModels.PuzzleV1 json = new WordSearchJsonModels.PuzzleV1();

    // Id
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

    // Grille
    char[][] grid = puzzle.getGrid();
    List<String> gridLines = new ArrayList<>(grid.length);
    for (char[] row : grid) {
      gridLines.add(new String(row));
    }
    json.grid = gridLines;

    // === ENTRIES : dédup par base + fusion FR/EN + phonétique + tags ===
    List<WordToFind> words = puzzle.getWords();
    Map<String, WordSearchJsonModels.EntryV1> entryByBase = new LinkedHashMap<>();

    if (words != null) {
      for (WordToFind w : words) {
        String base = w.getBaseForm();
        if (base == null || base.isBlank()) {
          base = w.getDisplayForm();
        }
        if (base == null || base.isBlank()) {
          continue;
        }

        WordSearchJsonModels.EntryV1 existing = entryByBase.get(base);
        if (existing == null) {
          WordSearchJsonModels.EntryV1 e = new WordSearchJsonModels.EntryV1();
          e.base = base;
          e.display = w.getDisplayForm();
          e.translation = w.getTranslation(); // FR
          e.slug = w.getSlug();
          e.partOfSpeech = w.getPartOfSpeech();
          e.extraInfo = w.getExtraInfo();
          e.phonetic = w.getPhonetic(); // PHONÉTIQUE
          e.translationEn = w.getTranslationEn(); // EN
          e.semanticTags = SemanticTagger.guessTags(e.translation);
          entryByBase.put(base, e);
        } else {
          // Fusion FR
          String t = w.getTranslation();
          if (t != null && !t.isBlank()) {
            if (existing.translation == null || existing.translation.isBlank()) {
              existing.translation = t;
            } else if (!existing.translation.equals(t)) {
              existing.translation = existing.translation + " ; " + t;
            }
          }

          // Fusion EN
          String tEn = w.getTranslationEn();
          if (tEn != null && !tEn.isBlank()) {
            if (existing.translationEn == null || existing.translationEn.isBlank()) {
              existing.translationEn = tEn;
            } else if (!existing.translationEn.equals(tEn)) {
              existing.translationEn = existing.translationEn + " ; " + tEn;
            }
          }

          // Phonétique : on garde la première non-nulle
          String ph = w.getPhonetic();
          if (existing.phonetic == null && ph != null && !ph.isBlank()) {
            existing.phonetic = ph;
          }

          // slug : premier, sinon on remplit si vide
          if (existing.slug == null && w.getSlug() != null) {
            existing.slug = w.getSlug();
          }

          // extraInfo : idem
          if (existing.extraInfo == null && w.getExtraInfo() != null) {
            existing.extraInfo = w.getExtraInfo();
          }

          // partOfSpeech : idem
          if (existing.partOfSpeech == null && w.getPartOfSpeech() != null) {
            existing.partOfSpeech = w.getPartOfSpeech();
          }

          // display : idem
          if (existing.display == null && w.getDisplayForm() != null) {
            existing.display = w.getDisplayForm();
          }

          // Tags sémantiques : si encore vides, on recalcule à partir de la traduction
          // fusionnée
          if (existing.semanticTags == null || existing.semanticTags.isEmpty()) {
            existing.semanticTags = SemanticTagger.guessTags(existing.translation);
          }
        }
      }
    }

    json.entries = new ArrayList<>(entryByBase.values());

    // === PLACEMENTS ===
    List<WordSearchJsonModels.PlacementV1> placements = new ArrayList<>();
    if (puzzle.getPlacements() != null) {
      Set<String> seenWords = new LinkedHashSet<>();
      for (WordPlacement p : puzzle.getPlacements()) {
        String word = p.getWord();
        if (word == null || word.isBlank()) {
          continue;
        }
        String key = word.toUpperCase();

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

    // === META ===
    Map<String, Object> meta = new HashMap<>();
    meta.put("source", "lexikongo");
    meta.put("meaningLanguage", meaningLanguageCode);

    // classes nominales
    Set<String> nominalClasses = new LinkedHashSet<>();
    for (WordSearchJsonModels.EntryV1 e : json.entries) {
      if (e.extraInfo != null && !e.extraInfo.isBlank()) {
        nominalClasses.add(e.extraInfo);
      }
    }
    if (!nominalClasses.isEmpty()) {
      meta.put("nominalClasses", new ArrayList<>(nominalClasses));
    }

    // domaines sémantiques
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
