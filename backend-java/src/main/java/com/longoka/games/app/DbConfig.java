package com.longoka.games.app;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public final class DbConfig {

  private static final Properties PROPS = new Properties();
  private static final String ACTIVE_PROFILE;

  // Options communes pour toutes les connexions JDBC
  private static final String JDBC_OPTIONS = "?useUnicode=true&characterEncoding=UTF-8&useSSL=false";

  static {
    // 1) Déterminer l'environnement : local ou prod
    String env = System.getenv("GAMES_ENV");
    if (env == null || env.isBlank()) {
      ACTIVE_PROFILE = "local";
    } else {
      ACTIVE_PROFILE = env.trim().toLowerCase(); // ex: "prod"
    }

    // 2) Choisir le fichier de config
    String filename = ACTIVE_PROFILE.equals("prod")
        ? "db-prod.properties"
        : "db-local.properties";

    Path configPath = Path.of("config", filename);

    try (FileInputStream fis = new FileInputStream(configPath.toFile())) {
      PROPS.load(fis);
      System.out.println(
          "Config DB (" + ACTIVE_PROFILE + ") chargée depuis "
              + configPath.toAbsolutePath());
    } catch (IOException e) {
      System.err.println(
          "Impossible de charger " + configPath.toAbsolutePath()
              + " : " + e.getMessage());
      System.err.println(
          "On utilisera uniquement les valeurs par défaut / variables d'environnement.");
    }
  }

  private DbConfig() {
    // util class
  }

  private static String propOrEnv(String propKey, String envKey, String defaultValue) {
    String val = PROPS.getProperty(propKey);
    if (val != null && !val.isEmpty()) {
      return val;
    }
    String env = System.getenv(envKey);
    if (env != null && !env.isEmpty()) {
      return env;
    }
    return defaultValue;
  }

  // =========================
  // 1) DB principale Lexikongo
  // =========================
  public static Connection openLexikongoConnection() throws SQLException {
    String host = propOrEnv("lex.db.host", "LEX_DB_HOST", "localhost");
    String port = propOrEnv("lex.db.port", "LEX_DB_PORT", "3306");
    String db = propOrEnv("lex.db.name", "LEX_DB_NAME", "6i695q_lexikongo");
    String user = propOrEnv("lex.db.user", "LEX_DB_USER", "root");
    String pass = propOrEnv("lex.db.pass", "LEX_DB_PASS", "");

    String url = "jdbc:mysql://" + host + ":" + port + "/" + db + JDBC_OPTIONS;
    return DriverManager.getConnection(url, user, pass);
  }

  // =========================
  // 2) DB Kikongo (lexikongo)
  // =========================
  public static Connection openKikongoLexConnection() throws SQLException {
    String host = propOrEnv("lex.kg.host", "LEX_KG_DB_HOST", "localhost");
    String port = propOrEnv("lex.kg.port", "LEX_KG_DB_PORT", "3306");
    String db = propOrEnv("lex.kg.name", "LEX_KG_DB_NAME", "6i695q_lexikongo");
    String user = propOrEnv("lex.kg.user", "LEX_KG_DB_USER", "root");
    String pass = propOrEnv("lex.kg.pass", "LEX_KG_DB_PASS", "");

    String url = "jdbc:mysql://" + host + ":" + port + "/" + db + JDBC_OPTIONS;
    return DriverManager.getConnection(url, user, pass);
  }

  // =========================
  // 3) DB Lingala (future)
  // =========================
  public static Connection openLingalaLexConnection() throws SQLException {
    String host = propOrEnv("lex.ln.host", "LEX_LN_DB_HOST", "localhost");
    String port = propOrEnv("lex.ln.port", "LEX_LN_DB_PORT", "3306");
    String db = propOrEnv("lex.ln.name", "LEX_LN_DB_NAME", "6i695q_lexilingala");
    String user = propOrEnv("lex.ln.user", "LEX_LN_DB_USER", "root");
    String pass = propOrEnv("lex.ln.pass", "LEX_LN_DB_PASS", "");

    String url = "jdbc:mysql://" + host + ":" + port + "/" + db + JDBC_OPTIONS;
    return DriverManager.getConnection(url, user, pass);
  }

  // =========================
  // 4) DB stock de jeux
  // =========================
  public static Connection openGamesStockConnection() throws SQLException {
    String host = propOrEnv("games.db.host", "GAMES_DB_HOST", "localhost");
    String port = propOrEnv("games.db.port", "GAMES_DB_PORT", "3306");
    String db = propOrEnv("games.db.name", "GAMES_DB_NAME", "6i695q_games_stock");
    String user = propOrEnv("games.db.user", "GAMES_DB_USER", "root");
    String pass = propOrEnv("games.db.pass", "GAMES_DB_PASS", "");

    String url = "jdbc:mysql://" + host + ":" + port + "/" + db + JDBC_OPTIONS;
    return DriverManager.getConnection(url, user, pass);
  }
}
