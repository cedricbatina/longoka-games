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

    // Dossier contenant db-local.properties / db-prod.properties :
    // - par defaut : ./config depuis le repertoire de travail (lancer Maven depuis backend-java)
    // - optionnel : variable LONGOKA_CONFIG_ROOT = chemin absolu vers le dossier config
    String configRoot = System.getenv("LONGOKA_CONFIG_ROOT");
    Path configPath = (configRoot != null && !configRoot.isBlank())
        ? Path.of(configRoot.trim(), filename)
        : Path.of("config", filename);

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

  private static String nonBlank(String value) {
    return (value != null && !value.isBlank()) ? value : null;
  }

  private static String prop(String key) {
    return nonBlank(PROPS.getProperty(key));
  }

  private static String env(String key) {
    return nonBlank(System.getenv(key));
  }

  /**
   * Ordre : cle dans .properties, variable Longoka LEX_*, puis variables generiques DB_* (comme un .env PHP/Node).
   */
  private static String resolveHost(String propKey, String lexEnvKey, String defaultValue) {
    String v = prop(propKey);
    if (v != null) {
      return v;
    }
    v = env(lexEnvKey);
    if (v != null) {
      return v;
    }
    v = env("DB_HOST");
    return v != null ? v : defaultValue;
  }

  private static String resolvePort(String propKey, String lexEnvKey, String defaultValue) {
    String v = prop(propKey);
    if (v != null) {
      return v;
    }
    v = env(lexEnvKey);
    if (v != null) {
      return v;
    }
    v = env("DB_PORT");
    return v != null ? v : defaultValue;
  }

  private static String resolveUser(String propKey, String lexEnvKey, String defaultValue) {
    String v = prop(propKey);
    if (v != null) {
      return v;
    }
    v = env(lexEnvKey);
    if (v != null) {
      return v;
    }
    v = env("DB_USER");
    return v != null ? v : defaultValue;
  }

  private static String resolvePass(String propKey, String lexEnvKey, String defaultValue) {
    String v = prop(propKey);
    if (v != null) {
      return v;
    }
    v = env(lexEnvKey);
    if (v != null) {
      return v;
    }
    v = env("DB_PASSWORD");
    if (v != null) {
      return v;
    }
    v = env("DB_PASS");
    return v != null ? v : defaultValue;
  }

  /**
   * Base Lexikongo : DB_NAME ne s'applique qu'aux schemas kg / legacy lex.db (pas a Lingala).
   */
  private static String resolveLexikongoDbName(String propKey, String lexEnvKey, String defaultValue) {
    String v = prop(propKey);
    if (v != null) {
      return v;
    }
    v = env(lexEnvKey);
    if (v != null) {
      return v;
    }
    v = env("DB_NAME");
    return v != null ? v : defaultValue;
  }

  private static String resolveLingalaDbName(String propKey, String lexEnvKey, String defaultValue) {
    String v = prop(propKey);
    if (v != null) {
      return v;
    }
    v = env(lexEnvKey);
    return v != null ? v : defaultValue;
  }

  private static String resolveGamesDbName(String propKey, String envKey, String defaultValue) {
    String v = prop(propKey);
    if (v != null) {
      return v;
    }
    v = env(envKey);
    return v != null ? v : defaultValue;
  }

  // =========================
  // 1) Alias legacy DB principale Lexikongo
  // =========================
  public static Connection openLexikongoConnection() throws SQLException {
    String host = resolveHost("lex.db.host", "LEX_DB_HOST", "localhost");
    String port = resolvePort("lex.db.port", "LEX_DB_PORT", "3306");
    String db = resolveLexikongoDbName("lex.db.name", "LEX_DB_NAME", "6i695q_lexikongo");
    String user = resolveUser("lex.db.user", "LEX_DB_USER", "root");
    String pass = resolvePass("lex.db.pass", "LEX_DB_PASS", "");

    String url = "jdbc:mysql://" + host + ":" + port + "/" + db + JDBC_OPTIONS;
    return DriverManager.getConnection(url, user, pass);
  }

  // =========================
  // 2) DB Kikongo
  // =========================
  public static Connection openKikongoLexConnection() throws SQLException {
    String host = resolveHost("lex.kg.host", "LEX_KG_DB_HOST", "localhost");
    String port = resolvePort("lex.kg.port", "LEX_KG_DB_PORT", "3306");
    String db = resolveLexikongoDbName("lex.kg.name", "LEX_KG_DB_NAME", "6i695q_lexikongo");
    String user = resolveUser("lex.kg.user", "LEX_KG_DB_USER", "root");
    String pass = resolvePass("lex.kg.pass", "LEX_KG_DB_PASS", "");

    String url = "jdbc:mysql://" + host + ":" + port + "/" + db + JDBC_OPTIONS;
    return DriverManager.getConnection(url, user, pass);
  }

  // =========================
  // 3) DB Lingala
  // =========================
  public static Connection openLingalaLexConnection() throws SQLException {
    String host = resolveHost("lex.ln.host", "LEX_LN_DB_HOST", "localhost");
    String port = resolvePort("lex.ln.port", "LEX_LN_DB_PORT", "3306");
    String db = resolveLingalaDbName("lex.ln.name", "LEX_LN_DB_NAME", "6i695q_lingala");
    String user = resolveUser("lex.ln.user", "LEX_LN_DB_USER", "root");
    String pass = resolvePass("lex.ln.pass", "LEX_LN_DB_PASS", "");

    String url = "jdbc:mysql://" + host + ":" + port + "/" + db + JDBC_OPTIONS;
    return DriverManager.getConnection(url, user, pass);
  }

  // =========================
  // 4) DB stock de jeux
  // =========================
  public static Connection openGamesStockConnection() throws SQLException {
    String host = resolveHost("games.db.host", "GAMES_DB_HOST", "localhost");
    String port = resolvePort("games.db.port", "GAMES_DB_PORT", "3306");
    String db = resolveGamesDbName("games.db.name", "GAMES_DB_NAME", "6i695q_games_stock");
    String user = resolveUser("games.db.user", "GAMES_DB_USER", "root");
    String pass = resolvePass("games.db.pass", "GAMES_DB_PASS", "");

    String url = "jdbc:mysql://" + host + ":" + port + "/" + db + JDBC_OPTIONS;
    return DriverManager.getConnection(url, user, pass);
  }
}
