package org.texttechnologylab.project.Nikola_Oljaca.DatabaseMigration;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

public class Nlpresultsaver {

    private String targetUri;
    private String targetDbName;
    private String collectionName;

    public Nlpresultsaver(String collectionName) throws Exception {
        Databaseconfigmigration.loadDatabaseConfig();
        this.targetUri = Databaseconfigmigration.getTargetDatabaseUri();
        this.targetDbName = Databaseconfigmigration.getTargetDatabaseName();
        this.collectionName = collectionName;
    }

    /**
     * Speichert die Analysis-Ergebnisse (als strukturiertes Document) in das Feld "analysis" des Rede-Dokuments.
     *
     * @param documentId    Die ID des Rede-Dokuments.
     * @param analysisDoc   Das strukturierte Analysis-Dokument.
     */
    public void saveResults(String documentId, Document analysisDoc) {
        try (MongoClient client = MongoClients.create(targetUri)) {
            MongoDatabase database = client.getDatabase(targetDbName);
            MongoCollection<Document> collection = database.getCollection(collectionName);
            Document update = new Document("$set", new Document("analysis", analysisDoc));
            collection.updateOne(new Document("_id", documentId), update);
            System.out.println("Dokument mit ID " + documentId + " wurde aktualisiert.");
        } catch (Exception e) {
            System.err.println("Fehler beim Speichern der NLP-Ergebnisse: " + e.getMessage());
            e.printStackTrace();
        }
    }
}





