package com.longoka.games.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.longoka.games.puzzles.crossword.json.CrosswordJsonModels;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tool CLI de démo pour générer un pack JSON de mots croisés Kikongo.
 * Pour l'instant : 1 seule grille codée en dur, pour valider le format.
 */
public final class KikongoCrosswordJsonExportTool {

 private KikongoCrosswordJsonExportTool() {
  // utilitaire
 }

 public static void main(String[] args) throws Exception {
  System.out.println("== Export JSON (démo) : Mots croisés Kikongo ==");

  String meaningLanguageCode = "fr";
  String languageCode = "kg";
  String outputPath = "target/kikongo-crossword-demo.v1.json";

  // 1) Construire une grille de démo simple (4x4)
  CrosswordJsonModels.PuzzleV1 puzzle = new CrosswordJsonModels.PuzzleV1();
  puzzle.id = "kg-cross-demo-1";
  puzzle.language = languageCode;
  puzzle.mode = "demo";
  puzzle.difficulty = "easy";
  puzzle.title = "Mots croisés Kikongo (démo)";
  puzzle.theme = "Démo";

  puzzle.rows = 4;
  puzzle.cols = 4;

  // Grille:
  // N Z O #
  // # # # #
  // M B O N
  // # # # #
  //
  // 1-A: NZO (demeure)
  // 2-A: MBON (argent) -> (juste un exemple bidon)
  List<String> grid = new ArrayList<>();
  grid.add("NZO#");
  grid.add("####");
  grid.add("MBON");
  grid.add("####");
  puzzle.grid = grid;

  // Entries
  List<CrosswordJsonModels.EntryV1> entries = new ArrayList<>();

  // 1-A
  {
   CrosswordJsonModels.EntryV1 e = new CrosswordJsonModels.EntryV1();
   e.id = "1-A";
   e.number = 1;
   e.direction = "ACROSS";
   e.row = 0;
   e.col = 0;
   e.answer = "NZO";
   e.display = "Nzo";
   e.clue = "Demeure"; // indice FR
   e.translation = "Demeure"; // traduction FR structurée
   e.translationEn = "House";
   e.phonetic = "nzó"; // exemple
   e.slug = "nzo";
   e.partOfSpeech = "noun";
   e.extraInfo = "n-zi";
   e.semanticTags = List.of("habitation");
   entries.add(e);
  }

  // 2-A
  {
   CrosswordJsonModels.EntryV1 e = new CrosswordJsonModels.EntryV1();
   e.id = "2-A";
   e.number = 2;
   e.direction = "ACROSS";
   e.row = 2;
   e.col = 0;
   e.answer = "MBON";
   e.display = "Mbono"; // juste pour l'exemple
   e.clue = "Argent (exemple)"; // indice FR (fake)
   e.translation = "Argent"; // traduction FR
   e.translationEn = "Money";
   e.phonetic = null;
   e.slug = null;
   e.partOfSpeech = "noun";
   e.extraInfo = null;
   e.semanticTags = List.of("travail");
   entries.add(e);
  }

  puzzle.entries = entries;

  // Meta
  Map<String, Object> meta = new HashMap<>();
  meta.put("source", "lexikongo-demo");
  meta.put("meaningLanguage", meaningLanguageCode);
  meta.put("nominalClasses", List.of("n-zi"));
  meta.put("semanticDomains", List.of("habitation", "travail"));
  puzzle.meta = meta;

  // 2) Pack
  CrosswordJsonModels.PackV1 pack = new CrosswordJsonModels.PackV1();
  pack.language = languageCode;
  pack.packId = "kg-cross-demo-" + LocalDate.now();
  pack.title = "Kikongo – mots croisés (démo)";
  pack.description = "Pack de démonstration: 1 grille 4x4.";
  pack.meaningLanguage = meaningLanguageCode;

  List<CrosswordJsonModels.PuzzleV1> puzzles = new ArrayList<>();
  puzzles.add(puzzle);
  pack.puzzles = puzzles;

  // 3) Sérialisation JSON
  ObjectMapper mapper = new ObjectMapper();
  mapper.enable(SerializationFeature.INDENT_OUTPUT);

  Path out = Path.of(outputPath);
  if (out.getParent() != null) {
   Files.createDirectories(out.getParent());
  }

  System.out.println("DEBUG: path absolu = " + out.toAbsolutePath());
  mapper.writeValue(out.toFile(), pack);

  System.out.println("✅ Export JSON (mots croisés démo) terminé : " + out.toAbsolutePath());
 }
}
