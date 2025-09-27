package org.texttechnologylab.project.Nikola_Oljaca.nlp;



import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.apache.uima.jcas.JCas;
import org.bson.Document;
import org.texttechnologylab.project.Nikola_Oljaca.DatabaseMigration.Databaseconfigmigration;
import org.texttechnologylab.project.Nikola_Oljaca.DatabaseMigration.Nlpresultsaver;
import org.texttechnologylab.project.Nikola_Oljaca.model.Speech;

import java.io.File;
import java.util.Scanner;
import java.io.File;
import java.util.Scanner;

public class Nlpmain {

    // Ordner, in dem die serialisierten CAS‑Dateien abgelegt werden.
    private static final String SERIALIZED_DIR = "serialized";

    public static void main(String[] args) {
        // Standardmäßig im "speech"-Modus; kann per Command‑Line-Parameter oder interaktiv überschrieben werden.
        String mode = null;
        if (args != null && args.length > 0) {
            mode = args[0].toLowerCase();
        } else {
            Scanner scanner = new Scanner(System.in);
            System.out.println("Bitte geben Sie den Modus ein: 'speech', 'video' oder 'both':");
            mode = scanner.nextLine().toLowerCase();
        }

        // Falls Video-Analyse benötigt wird, fragen wir den relativen Ordnerpfad ab:
        String videoFolderPath = null;
        if (mode.equals("video") || mode.equals("both")) {
            Scanner scanner = new Scanner(System.in);
            System.out.println("Bitte geben Sie den relativen Pfad zum Ordner mit den Videodateien ein (z. B. \"static/video\"):");
            videoFolderPath = scanner.nextLine();
        }

        try {
            // 1) DB-Konfiguration laden
            Databaseconfigmigration.loadDatabaseConfig();
            String targetUri = Databaseconfigmigration.getTargetDatabaseUri();
            String targetDbName = Databaseconfigmigration.getTargetDatabaseName();

            // 2) NLP-Pipeline instanziieren (Für Rede‑Analyse)
            NlppipelineService pipelineService = new NlppipelineService();

            // 3) Verbindung zur MongoDB herstellen
            try (MongoClient client = MongoClients.create(targetUri)) {
                MongoDatabase targetDb = client.getDatabase(targetDbName);
                // Reden liegen in der Collection "speech"
                MongoCollection<Document> speechColl = targetDb.getCollection("speech");

                // Erzeuge den Ordner für serialisierte CAS‑Dateien, falls nicht vorhanden
                File serDir = new File(SERIALIZED_DIR);
                if (!serDir.exists()) {
                    serDir.mkdir();
                }

                // Instanz des NlpResultSaver zum Speichern der Analyse-Ergebnisse in der "speech"-Collection
                Nlpresultsaver resultSaver = new Nlpresultsaver("speech");

                // Wenn im Modus "speech" oder "both" – Verarbeite die Reden
                if (mode.equals("speech") || mode.equals("both")) {
                    for (Document doc : speechColl.find()) {
                        Speech speech = new Speech();
                        speech.setId(doc.getString("_id"));
                        speech.setSpeaker(doc.getString("speaker"));
                        String text = doc.getString("text");
                        if (text == null) {
                            text = "";
                        }
                        speech.setText(text);

                        String fileName = SERIALIZED_DIR + File.separator + speech.getId() + ".xmi";
                        File xmiFile = new File(fileName);
                        JCas jcas;
                        if (xmiFile.exists()) {
                            jcas = Caseserialization.loadCas(xmiFile);
                            System.out.println("CAS für Dokument " + speech.getId() + " wurde geladen.");
                        } else {
                            jcas = Jcasconverter.convert(speech);
                            pipelineService.processJCas(jcas);
                            Caseserialization.saveCas(jcas, xmiFile);
                        }

                        Document analysisDoc = pipelineService.buildAnalysisDocument(jcas);
                        resultSaver.saveResults(doc.getString("_id"), analysisDoc);
                    }
                }

                // Wenn im Modus "video" oder "both" – Videoanalyse durchführen
                if (mode.equals("video") || mode.equals("both")) {
                    // Rufen Sie die Methode processAllVideos mit dem Ordnerpfad und der MongoDatabase auf.
                    VideonlpMain.processAllVideos(videoFolderPath, targetDb);
                }
            }

            System.out.println("NLP-Verarbeitung abgeschlossen.");
        } catch (Exception e) {
            System.err.println("Fehler bei der NLP-Verarbeitung: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
