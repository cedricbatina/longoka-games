package com.longoka.games.lexikongo;

import com.longoka.games.app.DbConfig;
import com.longoka.games.puzzles.crossword.json.CrosswordJsonModels;
import com.longoka.games.puzzles.wordsearch.json.SemanticTagger;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Service pour générer des MOTS CROISÉS MIXTES (noms + verbes) en Kikongo,
 * à partir de la base Lexikongo.
 *
 * On ne modifie PAS KikongoCrosswordService, on ajoute juste ce service séparé.
 */
public class KikongoMixedCrosswordService {

  /**
   * Génère une grille de mots croisés MIXTE (noms + verbes).
   *
   * @param rows                nombre de lignes de la grille
   * @param cols                nombre de colonnes de la grille
   * @param maxEntries          nombre maximal d’entrées (définitions) à placer
   * @param meaningLanguageCode langue des définitions (FR en pratique)
   * @param index               indice de la grille (pour construire l’id)
   */
  public CrosswordJsonModels.PuzzleV1 generateMixedCrossword(
      int rows,
      int cols,
      int maxEntries,
      String meaningLanguageCode,
      int index) throws SQLException {

    try (Connection conn = DbConfig.openKikongoLexConnection()) {
      LexikongoWordRepository wordRepo = new LexikongoWordRepository(conn);
      LexikongoVerbRepository verbRepo = new LexikongoVerbRepository(conn);

      // On cible moitié noms, moitié verbes (comme pour MixedWordSearch)
      int nounTarget = maxEntries / 2;
      int verbTarget = maxEntries - nounTarget;

      int nounFetch = nounTarget * 3;
      int verbFetch = verbTarget * 3;

      List<LexWord> lexWords = wordRepo.findRandomWords(nounFetch, meaningLanguageCode);
      List<LexVerb> lexVerbs = verbRepo.findRandomVerbsByLength(
          3, // minLength
          12, // maxLength
          verbFetch,
          meaningLanguageCode);

      // 1) Construire des candidats "riches"
      List<MixedEntryCandidate> nounCandidates = new ArrayList<>();
      List<MixedEntryCandidate> verbCandidates = new ArrayList<>();

      fillNounCandidates(nounCandidates, lexWords);
      fillVerbCandidates(verbCandidates, lexVerbs);

      // 2) Mixer les deux listes pour obtenir la sélection finale
      List<MixedEntryCandidate> mixedSelection = selectMixedEntries(
          nounCandidates,
          verbCandidates,
          maxEntries,
          nounTarget,
          verbTarget);

      // 3) Construire le PuzzleV1 à partir de cette sélection
      return buildPuzzleFromMixedCandidates(
          mixedSelection,
          rows,
          cols,
          meaningLanguageCode,
          index);
    }
  }

  // ---------------------------------------------------------------------------
  // Représentation interne d’un candidat (nom ou verbe)
  // ---------------------------------------------------------------------------

  /**
   * Candidat mixte (nom ou verbe) avec toutes les métadonnées utiles.
   */
  private static final class MixedEntryCandidate {
    String answer; // forme en majuscules à placer dans la grille (sanitisée)
    String display; // forme affichée
    String translation; // FR fusionné
    String translationEn; // EN fusionné
    String phonetic; // transcription phonétique
    String slug; // lien Lexikongo
    String partOfSpeech; // "noun" ou "verb"
    String extraInfo; // classe nominale pour les noms
    List<String> semanticTags; // tags sémantiques dérivés des traductions
  }

  // ---------------------------------------------------------------------------
  // Construction des candidats NOMS
  // ---------------------------------------------------------------------------

  private void fillNounCandidates(List<MixedEntryCandidate> target, List<LexWord> lexWords) {
    if (lexWords == null) {
      return;
    }

    for (LexWord w : lexWords) {
      String baseForm = chooseBaseFormFromWord(w);
      if (baseForm == null || baseForm.isBlank()) {
        continue;
      }

      String fr = w.getFrenchMeaningsJoined();
      String en = w.getEnglishMeaningsJoined();
      String phon = w.getPhonetic();
      String slug = w.getSlug();

      String extraInfo = null;
      if (w.getNominalClass() != null) {
        extraInfo = w.getNominalClass().getClassName();
      }

      List<String> tags = SemanticTagger.guessTags(fr);

      MixedEntryCandidate c = new MixedEntryCandidate();
      c.display = baseForm;
      c.answer = sanitizeAnswer(baseForm);
      if (c.answer.length() < 3) {
        continue; // trop court pour un mot croisé
      }
      c.translation = fr;
      c.translationEn = en;
      c.phonetic = phon;
      c.slug = slug;
      c.partOfSpeech = "noun";
      c.extraInfo = extraInfo;
      c.semanticTags = tags;

      target.add(c);
    }
  }

  private String chooseBaseFormFromWord(LexWord w) {
    if (w.getSingular() != null && !w.getSingular().isBlank()) {
      return w.getSingular();
    }
    if (w.getPlural() != null && !w.getPlural().isBlank()) {
      return w.getPlural();
    }
    if (w.getRoot() != null && !w.getRoot().isBlank()) {
      return w.getRoot();
    }
    return null;
  }

  // ---------------------------------------------------------------------------
  // Construction des candidats VERBES
  // ---------------------------------------------------------------------------

  private void fillVerbCandidates(List<MixedEntryCandidate> target, List<LexVerb> lexVerbs) {
    if (lexVerbs == null) {
      return;
    }

    for (LexVerb v : lexVerbs) {
      String baseForm = v.getGridForm();
      if (baseForm == null || baseForm.isBlank()) {
        continue;
      }

      String fr = v.getFrenchMeaningsJoined();
      String en = v.getEnglishMeaningsJoined();
      String phon = v.getPhonetic();
      String slug = v.getSlug();

      List<String> tags = SemanticTagger.guessTags(fr);

      MixedEntryCandidate c = new MixedEntryCandidate();
      c.display = baseForm;
      c.answer = sanitizeAnswer(baseForm);
      if (c.answer.length() < 3) {
        continue; // trop court
      }
      c.translation = fr;
      c.translationEn = en;
      c.phonetic = phon;
      c.slug = slug;
      c.partOfSpeech = "verb";
      c.extraInfo = null;
      c.semanticTags = tags;

      target.add(c);
    }
  }

  // ---------------------------------------------------------------------------
  // Sélection MIXTE noms + verbes (comme pour le MixedWordSearchService)
  // ---------------------------------------------------------------------------

  private List<MixedEntryCandidate> selectMixedEntries(
      List<MixedEntryCandidate> nounCandidates,
      List<MixedEntryCandidate> verbCandidates,
      int maxEntries,
      int nounTarget,
      int verbTarget) {

    List<MixedEntryCandidate> result = new ArrayList<>();

    int ni = 0;
    int vi = 0;

    // 1) D’abord essayer de respecter nounTarget / verbTarget
    while (result.size() < maxEntries &&
        (ni < nounCandidates.size() || vi < verbCandidates.size())) {

      if (ni < nounCandidates.size() && countOfPos(result, "noun") < nounTarget) {
        result.add(nounCandidates.get(ni++));
        if (result.size() >= maxEntries)
          break;
      }

      if (vi < verbCandidates.size() && countOfPos(result, "verb") < verbTarget) {
        result.add(verbCandidates.get(vi++));
      }

      if (countOfPos(result, "noun") >= nounTarget &&
          countOfPos(result, "verb") >= verbTarget) {
        break;
      }
    }

    // 2) Si on n’a pas assez de mots, on complète avec ce qu’il reste
    while (result.size() < maxEntries && ni < nounCandidates.size()) {
      result.add(nounCandidates.get(ni++));
    }
    while (result.size() < maxEntries && vi < verbCandidates.size()) {
      result.add(verbCandidates.get(vi++));
    }

    return result;
  }

  private int countOfPos(List<MixedEntryCandidate> list, String pos) {
    int c = 0;
    if (list == null || pos == null)
      return 0;
    for (MixedEntryCandidate e : list) {
      if (pos.equals(e.partOfSpeech))
        c++;
    }
    return c;
  }

  // ---------------------------------------------------------------------------
  // Construction du CrosswordJsonModels.PuzzleV1
  // ---------------------------------------------------------------------------

  /**
   * Construit la structure JSON d’une grille de mots croisés
   * à partir de la sélection mixte.
   *
   * On reprend la logique de buildCrosswordFromWords de KikongoCrosswordService.
   */
  private CrosswordJsonModels.PuzzleV1 buildPuzzleFromMixedCandidates(
      List<MixedEntryCandidate> mixedSelection,
      int rows,
      int cols,
      String meaningLanguageCode,
      int index) {

    // 1) Initialiser la grille remplie de '#'
    char[][] grid = new char[rows][cols];
    for (int r = 0; r < rows; r++) {
      for (int c = 0; c < cols; c++) {
        grid[r][c] = '#';
      }
    }

    List<PlacedWord> placed = new ArrayList<>();

    // 2) Placer le premier mot horizontalement en (0,0) si possible
    for (MixedEntryCandidate cand : mixedSelection) {
      String answer = cand.answer;
      if (answer == null || answer.isBlank()) {
        continue;
      }
      if (answer.length() <= cols) {
        placeAcross(grid, answer, 0, 0);
        PlacedWord pw = new PlacedWord();
        pw.candidate = cand;
        pw.row = 0;
        pw.col = 0;
        pw.across = true;
        placed.add(pw);
        break;
      }
    }

    // 3) Placer les autres mots avec croisements + fallback simple
    int maxEntries = mixedSelection.size(); // on limitera plus tard via number
    for (MixedEntryCandidate cand : mixedSelection) {
      if (placed.size() >= maxEntries) {
        break;
      }
      if (isAlreadyPlaced(placed, cand)) {
        continue;
      }

      String answer = cand.answer;
      if (answer == null) {
        continue;
      }
      answer = sanitizeAnswer(answer);
      if (answer.length() < 3) {
        continue;
      }

      PlacedWord found = tryPlaceWithCrossing(grid, answer);
      if (found != null) {
        found.candidate = cand;
        placed.add(found);
      } else {
        PlacedWord fallback = tryPlaceSimple(grid, answer);
        if (fallback != null) {
          fallback.candidate = cand;
          placed.add(fallback);
        }
      }
    }

    // 4) Construire le CrosswordJsonModels.PuzzleV1
    CrosswordJsonModels.PuzzleV1 puzzle = new CrosswordJsonModels.PuzzleV1();
    String mode = "mixed";

    puzzle.id = "kg-cross-mixed-" + index;
    puzzle.language = "kg";
    puzzle.mode = mode;
    puzzle.difficulty = "easy";
    puzzle.title = "Mots croisés Kikongo (mixed)";
    puzzle.theme = "noms + verbes";

    puzzle.rows = rows;
    puzzle.cols = cols;

    // Grille -> List<String>
    List<String> gridLines = new ArrayList<>(rows);
    for (int r = 0; r < rows; r++) {
      gridLines.add(new String(grid[r]));
    }
    puzzle.grid = gridLines;

    // Entries + meta
    List<CrosswordJsonModels.EntryV1> entries = new ArrayList<>();
    Set<String> nominalClasses = new LinkedHashSet<>();
    Set<String> semanticDomains = new LinkedHashSet<>();

    int number = 1;
    for (PlacedWord pw : placed) {
      MixedEntryCandidate c = pw.candidate;
      if (c == null) {
        continue;
      }

      String answer = c.answer;
      if (answer == null) {
        continue;
      }

      CrosswordJsonModels.EntryV1 e = new CrosswordJsonModels.EntryV1();
      e.id = number + (pw.across ? "-A" : "-D");
      e.number = number;
      e.direction = pw.across ? "ACROSS" : "DOWN";
      e.row = pw.row;
      e.col = pw.col;
      e.answer = answer;
      e.display = c.display;
      e.clue = c.translation; // FR comme indice
      e.translation = c.translation; // FR
      e.translationEn = c.translationEn;
      e.phonetic = c.phonetic;
      e.slug = c.slug;
      e.partOfSpeech = c.partOfSpeech;
      e.extraInfo = c.extraInfo;

      // Semantic tags : on réutilise ceux pré-calculés si dispo
      List<String> tags = (c.semanticTags != null && !c.semanticTags.isEmpty())
          ? c.semanticTags
          : SemanticTagger.guessTags(e.translation);
      e.semanticTags = tags;
      if (tags != null) {
        semanticDomains.addAll(tags);
      }

      if (e.extraInfo != null && !e.extraInfo.isBlank()) {
        nominalClasses.add(e.extraInfo);
      }

      entries.add(e);
      number++;
    }

    puzzle.entries = entries;

    Map<String, Object> meta = new HashMap<>();
    meta.put("source", "lexikongo");
    meta.put("meaningLanguage", meaningLanguageCode);

    if (!nominalClasses.isEmpty()) {
      meta.put("nominalClasses", new ArrayList<>(nominalClasses));
    }
    if (!semanticDomains.isEmpty()) {
      meta.put("semanticDomains", new ArrayList<>(semanticDomains));
    }

    puzzle.meta = meta;

    return puzzle;
  }

  // ---------------------------------------------------------------------------
  // Helpers placement (copiés/adaptés depuis KikongoCrosswordService)
  // ---------------------------------------------------------------------------

  /**
   * Nettoie une base pour obtenir la forme "réponse" en lettres A-Z (majuscules).
   */
  private String sanitizeAnswer(String baseForm) {
    if (baseForm == null) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    String s = baseForm.toUpperCase(Locale.ROOT);
    for (int i = 0; i < s.length(); i++) {
      char ch = s.charAt(i);
      if (ch >= 'A' && ch <= 'Z') {
        sb.append(ch);
      }
    }
    return sb.toString();
  }

  private boolean isAlreadyPlaced(List<PlacedWord> placed, MixedEntryCandidate cand) {
    if (cand == null || cand.display == null) {
      return false;
    }
    String key = cand.display.toLowerCase(Locale.ROOT);
    for (PlacedWord pw : placed) {
      if (pw.candidate != null && pw.candidate.display != null) {
        if (key.equals(pw.candidate.display.toLowerCase(Locale.ROOT))) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Essaie de placer un mot en croisant des lettres déjà présentes.
   */
  private PlacedWord tryPlaceWithCrossing(char[][] grid, String answer) {
    int rows = grid.length;
    int cols = grid[0].length;
    char[] letters = answer.toCharArray();

    for (int r = 0; r < rows; r++) {
      for (int c = 0; c < cols; c++) {
        char existing = grid[r][c];
        if (existing == '#' || existing == 0) {
          continue;
        }
        for (int i = 0; i < letters.length; i++) {
          if (letters[i] != existing) {
            continue;
          }

          // Tentative horizontale (ACROSS)
          int startCol = c - i;
          if (startCol >= 0 && startCol + letters.length <= cols) {
            if (canPlaceAcross(grid, letters, r, startCol)) {
              placeAcross(grid, answer, r, startCol);
              PlacedWord pw = new PlacedWord();
              pw.row = r;
              pw.col = startCol;
              pw.across = true;
              return pw;
            }
          }

          // Tentative verticale (DOWN)
          int startRow = r - i;
          if (startRow >= 0 && startRow + letters.length <= rows) {
            if (canPlaceDown(grid, letters, startRow, c)) {
              placeDown(grid, answer, startRow, c);
              PlacedWord pw = new PlacedWord();
              pw.row = startRow;
              pw.col = c;
              pw.across = false;
              return pw;
            }
          }
        }
      }
    }
    return null;
  }

  /**
   * Fallback : on tente une place horizontale simple puis verticale.
   */
  private PlacedWord tryPlaceSimple(char[][] grid, String answer) {
    int rows = grid.length;
    int cols = grid[0].length;
    char[] letters = answer.toCharArray();

    // Horizontal
    for (int r = 0; r < rows; r++) {
      for (int startCol = 0; startCol + letters.length <= cols; startCol++) {
        if (canPlaceAcross(grid, letters, r, startCol)) {
          placeAcross(grid, answer, r, startCol);
          PlacedWord pw = new PlacedWord();
          pw.row = r;
          pw.col = startCol;
          pw.across = true;
          return pw;
        }
      }
    }

    // Vertical
    for (int c = 0; c < cols; c++) {
      for (int startRow = 0; startRow + letters.length <= rows; startRow++) {
        if (canPlaceDown(grid, letters, startRow, c)) {
          placeDown(grid, answer, startRow, c);
          PlacedWord pw = new PlacedWord();
          pw.row = startRow;
          pw.col = c;
          pw.across = false;
          return pw;
        }
      }
    }

    return null;
  }

  private boolean canPlaceAcross(char[][] grid, char[] letters, int row, int startCol) {
    int cols = grid[0].length;
    if (startCol < 0 || startCol + letters.length > cols) {
      return false;
    }
    for (int i = 0; i < letters.length; i++) {
      char existing = grid[row][startCol + i];
      if (existing != '#' && existing != letters[i]) {
        return false;
      }
    }
    return true;
  }

  private boolean canPlaceDown(char[][] grid, char[] letters, int startRow, int col) {
    int rows = grid.length;
    if (startRow < 0 || startRow + letters.length > rows) {
      return false;
    }
    for (int i = 0; i < letters.length; i++) {
      char existing = grid[startRow + i][col];
      if (existing != '#' && existing != letters[i]) {
        return false;
      }
    }
    return true;
  }

  private void placeAcross(char[][] grid, String answer, int row, int startCol) {
    char[] letters = answer.toCharArray();
    for (int i = 0; i < letters.length; i++) {
      grid[row][startCol + i] = letters[i];
    }
  }

  private void placeDown(char[][] grid, String answer, int startRow, int col) {
    char[] letters = answer.toCharArray();
    for (int i = 0; i < letters.length; i++) {
      grid[startRow + i][col] = letters[i];
    }
  }

  /**
   * Petite structure interne pour mémoriser un mot placé.
   */
  private static final class PlacedWord {
    MixedEntryCandidate candidate;
    int row;
    int col;
    boolean across;
  }
}
