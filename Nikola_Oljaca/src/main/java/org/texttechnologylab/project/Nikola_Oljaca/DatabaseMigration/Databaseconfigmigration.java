package org.texttechnologylab.project.Nikola_Oljaca.DatabaseMigration;

/**
 * Diese Klasse führt eine Migration der Datenbank durch.
 * Sie exportiert alle Daten der Quell-Datenbank als JSON-Dokumente
 * und importiert diese in die Ziel-Datenbank, nachdem alle Collections der Ziel-Datenbank gelöscht wurden.
 */
import java.io.*;
import java.nio.file.*;
import java.util.Properties;

import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

public class Databaseconfigmigration {

    private static final String SOURCE_CONFIG_FILE = "musterloesung_uebung2_db_ro"; // Datei mit Quell-Datenbankdetails
    private static final String TARGET_CONFIG_FILE = "dbconfig.properties"; // Datei mit Ziel-Datenbankdetails

    private static String sourceDatabaseUri;
    private static String sourceDatabaseName;
    private static String targetDatabaseUri;
    private static String targetDatabaseName;

    /**
     * Hauptmethode, die die Migrationslogik ausführt.
     *
     * @param args Programmargumente (nicht verwendet)
     */
    public static void main(String[] args) {
        try {
            // 1. Quell- und Ziel-Datenbankdetails laden
            loadDatabaseConfig();

            // 2. Daten aus der Quell-Datenbank exportieren
            Path exportPath = Paths.get("data/exported_collections");
            exportSourceDatabase(exportPath);

            // 3. Ziel-Datenbank leeren
            try (MongoClient targetClient = MongoClients.create(new ConnectionString(targetDatabaseUri))) {
                MongoDatabase targetDatabase = targetClient.getDatabase(targetDatabaseName);
                clearAllCollections(targetDatabase);

                // 4. Exportierte Daten in die Ziel-Datenbank importieren
                importDataToTargetDatabase(exportPath, targetDatabase);
            }

            System.out.println("Migration erfolgreich abgeschlossen.");
        } catch (Exception e) {
            System.err.println("Fehler bei der Datenbankmigration: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Lädt die Konfigurationsdaten für Quell- und Ziel-Datenbanken aus Properties-Dateien.
     *
     * @throws IOException Falls die Datei nicht gefunden wird oder fehlerhaft ist
     */
    public static void loadDatabaseConfig() throws IOException {
        sourceDatabaseUri = loadDatabaseUriFromConfig(SOURCE_CONFIG_FILE);
        targetDatabaseUri = loadDatabaseUriFromConfig(TARGET_CONFIG_FILE);
    }

    /**
     * Lädt die URI einer Datenbank aus einer Properties-Datei.
     *
     * @param configFile Der Name der Konfigurationsdatei
     * @return Die URI der Datenbank
     * @throws IOException Falls die Datei nicht gefunden wird oder fehlerhaft ist
     */
    private static String loadDatabaseUriFromConfig(String configFile) throws IOException {
        Properties properties = new Properties();
        try (InputStream inputStream = Databaseconfigmigration.class.getClassLoader().getResourceAsStream(configFile)) {
            if (inputStream == null) {
                throw new FileNotFoundException("Die Datei " + configFile + " wurde nicht im Klassenpfad gefunden.");
            }
            properties.load(inputStream);
        }

        // Datenbankdetails aus der Properties-Datei laden
        String host = properties.getProperty("remote_host");
        String port = properties.getProperty("remote_port");
        String user = properties.getProperty("remote_user");
        String password = properties.getProperty("remote_password");
        String databaseName = properties.getProperty("remote_database");

        if (configFile.equals(SOURCE_CONFIG_FILE)) {
            sourceDatabaseName = databaseName;
        } else {
            targetDatabaseName = databaseName;
        }

        return String.format("mongodb://%s:%s@%s:%s/%s", user, password, host, port, databaseName);
    }

    /**
     * Exportiert alle Collections aus der Quell-Datenbank in JSON-Dateien.
     *
     * @param exportPath Der Zielpfad für die exportierten JSON-Dateien
     */
    private static void exportSourceDatabase(Path exportPath) {
        try (MongoClient sourceClient = MongoClients.create(new ConnectionString(sourceDatabaseUri))) {
            MongoDatabase sourceDatabase = sourceClient.getDatabase(sourceDatabaseName);

            if (!Files.exists(exportPath)) {
                Files.createDirectories(exportPath);
            }

            for (String collectionName : sourceDatabase.listCollectionNames()) {
                MongoCollection<Document> collection = sourceDatabase.getCollection(collectionName);
                Path collectionFile = exportPath.resolve(collectionName + ".json");

                try (BufferedWriter writer = Files.newBufferedWriter(collectionFile)) {
                    for (Document doc : collection.find()) {
                        writer.write(doc.toJson());
                        writer.newLine();
                    }
                }

                System.out.println("Collection '" + collectionName + "' wurde exportiert nach " + collectionFile);
            }
        } catch (Exception e) {
            System.err.println("Fehler beim Export der Quell-Datenbank: " + e.getMessage());
        }
    }

    /**
     * Löscht alle Collections in der Ziel-Datenbank.
     *
     * @param targetDatabase Die Ziel-Datenbank
     */
    private static void clearAllCollections(MongoDatabase targetDatabase) {
        try {
            for (String collectionName : targetDatabase.listCollectionNames()) {
                try {
                    targetDatabase.getCollection(collectionName).drop();
                    System.out.println("Collection '" + collectionName + "' wurde gelöscht.");
                } catch (Exception e) {
                    System.err.println("Fehler beim Löschen der Collection '" + collectionName + "': " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Fehler beim Abrufen der Collections: " + e.getMessage());
        }
    }

    /**
     * Importiert die JSON-Dateien in die Ziel-Datenbank.
     *
     * @param importPath Der Pfad der zu importierenden JSON-Dateien
     * @param targetDatabase Die Ziel-Datenbank
     */

    private static void importDataToTargetDatabase(Path importPath, MongoDatabase targetDatabase) {
        try {
            for (Path filePath : Files.newDirectoryStream(importPath, "*.json")) {
                String collectionName = filePath.getFileName().toString().replaceFirst("\\.json$", "");
                MongoCollection<Document> collection = targetDatabase.getCollection(collectionName);

                try (BufferedReader reader = Files.newBufferedReader(filePath)) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        collection.insertOne(Document.parse(line));
                    }
                }

                System.out.println("Daten von " + filePath + " wurden in Collection '" + collectionName + "' importiert.");
            }
        } catch (Exception e) {
            System.err.println("Fehler beim Import der Daten in die Ziel-Datenbank: " + e.getMessage());
        }
    }
    public static String getTargetDatabaseUri() {
        return targetDatabaseUri;
    }

    public static String getTargetDatabaseName() {
        return targetDatabaseName;
    }

}

