package org.texttechnologylab.project.Nikola_Oljaca.DatabaseMigration;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;
import org.bson.Document;

import java.util.List;

/**
 * Diese Klasse speichert die Analyseergebnisse von Videos in MongoDB.
 */
public class Videoresultsaver {

    private String targetUri;
    private String targetDbName;
    private String collectionName;

    /**
     * Konstruktor: Lädt die Datenbankkonfiguration und setzt die Ziel-URI und den Datenbanknamen.
     *
     * @param collectionName Name der MongoDB-Collection, in der die Daten gespeichert werden sollen.
     * @throws Exception Falls die Datenbankkonfiguration nicht geladen werden kann.
     */
    public Videoresultsaver(String collectionName) throws Exception {
        Databaseconfigmigration.loadDatabaseConfig();
        this.targetUri = Databaseconfigmigration.getTargetDatabaseUri();
        this.targetDbName = Databaseconfigmigration.getTargetDatabaseName();
        this.collectionName = collectionName;
    }

    /**
     * Speichert das Transkript und die zugehörige Satzliste in MongoDB.
     *
     * @param documentId  Die eindeutige ID des Dokuments.
     * @param transcript  Der vollständige Transkript-Text.
     * @param sentenceList Die Liste der Sätze mit Zeitangaben.
     */
    public void saveTranscriptWithSentences(String documentId,
                                            String transcript,
                                            List<Document> sentenceList) {
        try (MongoClient client = MongoClients.create(targetUri)) {
            MongoDatabase db = client.getDatabase(targetDbName);
            MongoCollection<Document> coll = db.getCollection(collectionName);

            Document filter = new Document("_id", documentId);
            Document setData = new Document("videoAnalysis", transcript)
                    .append("sentenceList", sentenceList);

            Document update = new Document("$set", setData);
            coll.updateOne(filter, update, new UpdateOptions().upsert(true));

            System.out.println("[VideoResultSaver] Text + Sentences gespeichert für: " + documentId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Optional: Bestehende Methoden anpassen oder entfernen, falls nicht mehr benötigt.
}














