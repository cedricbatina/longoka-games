Configuration MySQL pour longoka-games (backend Java)
=====================================================

Fichiers utilises par DbConfig.java :
  - db-local.properties   si GAMES_ENV est absent ou != prod
  - db-prod.properties    si GAMES_ENV=prod

Emplacement : ce dossier (backend-java/config).

Lancement Maven / scripts :
  - Le repertoire de travail doit etre backend-java, pour que le chemin relatif "config/..." fonctionne.
  - Sinon : definir LONGOKA_CONFIG_ROOT vers ce dossier (chemin absolu), par exemple :
      set LONGOKA_CONFIG_ROOT=D:\works\lectures\longoka-games\backend-java\config

Modele sans secrets : db-local.properties.example (copier vers db-local.properties).

Les cles lex.kg.*, lex.ln.*, lex.db.*, games.db.* peuvent etre surchargees par des variables
d'environnement (voir DbConfig.java : LEX_KG_DB_HOST, etc.) pour CI ou Docker.

Variables generiques (meme idee qu'un .env pour une autre appli) :
  DB_HOST, DB_PORT, DB_USER, DB_PASSWORD (ou DB_PASS), DB_NAME
  -> utilisées comme repli pour hote, port, user, mot de passe.
  -> DB_NAME s'applique seulement a Lexikongo (lex.kg / lex.db), pas au schema Lingala.
     Pour Lingala : lex.ln.name dans le fichier ou LEX_LN_DB_NAME.

SMTP (SMTP_HOST, etc.) : non lu par ce backend Java ; reserve a vos autres services.

ISBN imprime (sortie livre) :
  - Variable d'environnement LONGOKA_BOOK_ISBN (ex. 978xxxxxxxxxx) : remplace l'ISBN-13 fictif
    dans meta.book.isbn lors de la generation (BiweeklyPuzzleBatchTool).
  - Alternative : propriete JVM -Dlongoka.book.isbn=978...
