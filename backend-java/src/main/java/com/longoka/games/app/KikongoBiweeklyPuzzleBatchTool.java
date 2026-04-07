package com.longoka.games.app;

import java.util.ArrayList;
import java.util.List;

/**
 * Point d'entree dedie pour la generation bihebdo Kikongo.
 */
public final class KikongoBiweeklyPuzzleBatchTool {

  private static final String LANGUAGE_CODE = "kg";

  private KikongoBiweeklyPuzzleBatchTool() {
  }

  public static void main(String[] args) throws Exception {
    BiweeklyPuzzleBatchTool.main(forceLanguage(args, LANGUAGE_CODE));
  }

  private static String[] forceLanguage(String[] args, String languageCode) {
    List<String> sanitized = new ArrayList<>();
    if (args != null) {
      for (int index = 0; index < args.length; index += 1) {
        String current = args[index];
        if ("--language".equalsIgnoreCase(current) || "--languages".equalsIgnoreCase(current)) {
          index += 1;
          continue;
        }
        sanitized.add(current);
      }
    }
    sanitized.add("--language");
    sanitized.add(languageCode);
    return sanitized.toArray(String[]::new);
  }
}
