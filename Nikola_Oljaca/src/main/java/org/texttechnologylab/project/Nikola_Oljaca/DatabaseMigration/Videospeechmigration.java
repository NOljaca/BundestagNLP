package org.texttechnologylab.project.Nikola_Oljaca.DatabaseMigration;

import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;

/**
 * Diese Klasse führt die Migration durch, indem sie die video-Collection mit der speech-Collection verknüpft
 * und die Daten der video-Collection in die entsprechenden speech-Dokumente einfügt.
 * Die Verknüpfung erfolgt über die gemeinsame Basis-ID.
 */
public class Videospeechmigration {

    /**
     * Hauptmethode zur Ausführung der Migration.
     *
     * @param args Kommandozeilenargumente (nicht verwendet).
     */
    public static void main(String[] args) {
        // Schritt 1: Lade die Datenbankkonfiguration
        try {
            Databaseconfigmigration.loadDatabaseConfig();
        } catch (Exception e) {
            System.err.println("Fehler beim Laden der Datenbankkonfiguration: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        // Schritt 2: Verbinde zur Ziel-Datenbank
        String targetUri = Databaseconfigmigration.getTargetDatabaseUri();
        String targetDbName = Databaseconfigmigration.getTargetDatabaseName();

        try (MongoClient mongoClient = MongoClients.create(targetUri)) {
            MongoDatabase database = mongoClient.getDatabase(targetDbName);
            MongoCollection<Document> speechCollection = database.getCollection("speech");
            MongoCollection<Document> videoCollection = database.getCollection("video");

            System.out.println("Verbindung zur MongoDB hergestellt.");
            System.out.println("Starte Migration der video-Collection mit der speech-Collection...");

            // Schritt 3: Iteriere über alle Video-Dokumente
            FindIterable<Document> videos = videoCollection.find();
            int migratedCount = 0;
            int notFoundCount = 0;

            for (Document videoDoc : videos) {
                String videoId = videoDoc.getString("_id");
                String baseId = extractBaseId(videoId);

                if (baseId == null || baseId.isEmpty()) {
                    System.err.println("Basis-ID konnte aus Video-ID '" + videoId + "' nicht extrahiert werden.");
                    notFoundCount++;
                    continue;
                }

                // Suche das entsprechende Speech-Dokument
                Document speechDoc = speechCollection.find(Filters.eq("_id", baseId)).first();

                if (speechDoc != null) {
                    // Füge die Video-Daten zum Speech-Dokument hinzu
                    // Hier wird ein neues Feld "videoData" hinzugefügt, das die Video-Daten enthält
                    Document videoData = new Document("videoId", videoId)
                            .append("videoDetails", videoDoc);

                    // Aktualisiere das Speech-Dokument mit den Video-Daten
                    speechCollection.updateOne(Filters.eq("_id", baseId),
                            Updates.set("videoData", videoData));

                    migratedCount++;
                    System.out.println("Speech-ID '" + baseId + "' erfolgreich mit Video-ID '" + videoId + "' verknüpft.");
                } else {
                    System.err.println("Kein Speech-Dokument gefunden für Basis-ID '" + baseId + "', Video-ID '" + videoId + "'.");
                    notFoundCount++;
                }
            }

            // Schritt 4: Zusammenfassung der Migration
            System.out.println("Migration abgeschlossen: " + migratedCount + " migriert, " + notFoundCount + " nicht gefunden.");

        } catch (Exception e) {
            System.err.println("Fehler während der Migration: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Extrahiert die Basis-ID aus der Video-_id, indem der String vor den doppelten Unterstrichen '__' genommen wird.
     *
     * @param videoId Die vollständige Video-_id.
     * @return Die extrahierte Basis-ID oder null, falls keine Unterstriche gefunden wurden.
     */
    private static String extractBaseId(String videoId) {
        if (videoId == null) {
            return null;
        }
        String delimiter = "__";
        int index = videoId.indexOf(delimiter);
        if (index != -1) {
            return videoId.substring(0, index);
        }
        // Falls keine doppelten Unterstriche gefunden werden, versuche einfache Unterstriche
        delimiter = "_";
        index = videoId.indexOf(delimiter);
        if (index != -1) {
            return videoId.substring(0, index);
        }
        // Falls keine Unterstriche gefunden werden, gebe die gesamte ID zurück
        return videoId;
    }
}





