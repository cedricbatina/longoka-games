package com.longoka.games.app;

/**
 * Tool CLI pour generer un premier pack JSON memory-match Natikongo.
 */
public final class NatikongoBasicsMemoryMatchJsonExportTool {

  private NatikongoBasicsMemoryMatchJsonExportTool() {
    // utilitaire
  }

  public static void main(String[] args) throws Exception {
    System.out.println("== Export JSON : Memory match Natikongo (bases) ==");

    String outputPath = "target/packs/natikongo/natikongo-memory-basics-pack.v1.json";
    NatikongoMemoryMatchPackExporter.exportBasicsPack(outputPath);
  }
}