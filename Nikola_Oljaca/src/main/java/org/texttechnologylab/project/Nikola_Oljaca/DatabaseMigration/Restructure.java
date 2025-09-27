package org.texttechnologylab.project.Nikola_Oljaca.DatabaseMigration;

import com.mongodb.MongoBulkWriteException;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import org.bson.Document;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Diese Klasse führt die Datenumstrukturierung der speech-Collection durch.
 * Sie fügt eine Sentiment-Wertliste zu jedem textContent-Block basierend auf den vorhandenen Sentiment-Analysen hinzu
 * und zählt die POS-Typen aus der posTags-Analyse.
 */
public class Restructure {

    /**
     * Hauptmethode zur Ausführung der Datenumstrukturierung.
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

        // Erstelle die MongoDB-Client-Einstellungen
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new com.mongodb.ConnectionString(targetUri))
                .build();

        try (MongoClient mongoClient = MongoClients.create(settings)) {
            MongoDatabase database = mongoClient.getDatabase(targetDbName);
            MongoCollection<Document> speechCollection = database.getCollection("speech");

            System.out.println("Verbindung zur MongoDB hergestellt.");
            System.out.println("Starte Datenumstrukturierung der speech-Collection...");

            // Schritt 3: Iteriere über alle Speech-Dokumente
            FindIterable<Document> speeches = speechCollection.find();
            int processedCount = 0;
            int updatedCount = 0;
            int errorCount = 0;

            for (Document speechDoc : speeches) {
                processedCount++;
                String speechId = speechDoc.getString("_id");
                Document analysis = speechDoc.get("analysis", Document.class);
                if (analysis == null) {
                    System.err.println("Keine Analyse-Daten gefunden für Speech-ID '" + speechId + "'.");
                    errorCount++;
                    continue;
                }

                // Verarbeitung der Sentiment-Analysen
                List<String> analysisSentences = analysis.getList("sentences", String.class);
                if (analysisSentences == null || analysisSentences.isEmpty()) {
                    continue;
                }

                // Extrahiere Sentiment-Werte aus analysis.sentences
                Map<String, Double> sentenceSentimentMap = extractSentimentMap(analysisSentences);

                if (sentenceSentimentMap.isEmpty()) {
                    continue;
                }

                // Extrahiere die textContent-Blocks
                List<Document> textContents = speechDoc.getList("textContent", Document.class);
                if (textContents == null || textContents.isEmpty()) {
                    continue;
                }

                // Initialisiere eine Liste zur Speicherung der aktualisierten textContent-Blöcke
                List<Document> updatedTextContents = new ArrayList<>();

                for (Document textContent : textContents) {
                    String textBlock = textContent.getString("text");
                    if (textBlock == null || textBlock.isEmpty()) {
                        updatedTextContents.add(textContent);
                        continue;
                    }

                    List<String> sentencesInBlock = splitIntoSentences(textBlock);
                    List<Double> sentimentValuesForBlock = new ArrayList<>();

                    for (String sentence : sentencesInBlock) {
                        Double sentiment = sentenceSentimentMap.get(sentence);
                        if (sentiment != null) {
                            sentimentValuesForBlock.add(sentiment);
                        }
                        // Falls der Satz nicht in der Sentiment-Analyse vorhanden ist (z.B. Beifall), wird kein Wert hinzugefügt
                    }

                    // Füge die sentimentValues nur hinzu, wenn mindestens ein Sentiment-Wert vorhanden ist
                    if (!sentimentValuesForBlock.isEmpty()) {
                        textContent.put("sentimentValues", sentimentValuesForBlock);
                    }

                    updatedTextContents.add(textContent);
                }

                // Verarbeitung der POS-Tags
                List<String> posTags = analysis.getList("posTags", String.class);
                Map<String, Integer> posCounts = new HashMap<>();

                if (posTags != null && !posTags.isEmpty()) {
                    posCounts = extractPOSCounts(posTags);
                } else {
                    System.err.println("Keine posTags gefunden für Speech-ID '" + speechId + "'.");
                }

                // Initialisiere das Update-Document
                Document updateDocument = new Document();

                if (!updatedTextContents.isEmpty()) {
                    updateDocument.append("textContent", updatedTextContents);
                }

                if (!posCounts.isEmpty()) {
                    // Stelle sicher, dass nlpStats existiert
                    Document nlpStats = speechDoc.get("nlpStats", Document.class);
                    if (nlpStats == null) {
                        nlpStats = new Document();
                    }
                    nlpStats.put("posCounts", posCounts);
                    updateDocument.append("nlpStats", nlpStats);
                }

                if (updateDocument.isEmpty()) {
                    System.out.println("Keine Aktualisierungen für Speech-ID '" + speechId + "'.");
                    continue;
                }

                try {
                    // Führe das Update durch
                    speechCollection.updateOne(Filters.eq("_id", speechId), new Document("$set", updateDocument));
                    updatedCount++;
                } catch (MongoBulkWriteException mbwe) {

                } catch (Exception e) {

                    ;
                }
            }

            // Schritt 4: Zusammenfassung der Datenumstrukturierung
            System.out.println("Datenumstrukturierung abgeschlossen: " + processedCount + " verarbeitet, " + updatedCount + " aktualisiert, " + errorCount + " Fehler.");

        } catch (Exception e) {
            System.err.println("Fehler beim Verbinden mit MongoDB: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Extrahiert eine Map von Sätzen zu Sentiment-Werten aus der Liste der analysis.sentences.
     *
     * @param analysisSentences Liste der Sentiment-Strings aus analysis.sentences.
     * @return Map, die jeden Satz mit seinem Sentiment-Wert verknüpft.
     */
    private static Map<String, Double> extractSentimentMap(List<String> analysisSentences) {
        Map<String, Double> sentenceSentimentMap = new HashMap<>();
        Pattern pattern = Pattern.compile("Satz: (.*?)\\s*-> Sentiment: (-?\\d+\\.\\d+|\\d+)");

        for (String sentenceEntry : analysisSentences) {
            Matcher matcher = pattern.matcher(sentenceEntry);
            if (matcher.find()) {
                String sentence = matcher.group(1).trim();
                String sentimentStr = matcher.group(2).trim();
                try {
                    double sentiment = Double.parseDouble(sentimentStr);
                    sentenceSentimentMap.put(sentence, sentiment);
                } catch (NumberFormatException e) {
                    System.err.println("Ungültiges Sentiment-Format: " + sentimentStr + " in Eintrag: " + sentenceEntry);
                }
            } else {
                System.err.println("Sentiment-Wert nicht gefunden in: " + sentenceEntry);
            }
        }

        return sentenceSentimentMap;
    }

    /**
     * Extrahiert eine Map von POS-Typen zu deren Häufigkeit aus der Liste der posTags.
     *
     * @param posTags Liste der POS-Strings aus posTags.
     * @return Map, die jeden POS-Typ mit seiner Häufigkeit verknüpft.
     */
    private static Map<String, Integer> extractPOSCounts(List<String> posTags) {
        Map<String, Integer> posCounts = new HashMap<>();
        Pattern posPattern = Pattern.compile("\\[(.*?)\\]");

        for (String tag : posTags) {
            Matcher matcher = posPattern.matcher(tag);
            if (matcher.find()) {
                String pos = matcher.group(1).trim();
                posCounts.put(pos, posCounts.getOrDefault(pos, 0) + 1);
            } else {
                System.err.println("POS-Tag nicht gefunden in: " + tag);
            }
        }

        return posCounts;
    }

    /**
     * Splittet einen Text in einzelne Sätze.
     *
     * @param text Der zu splittende Text.
     * @return Liste der Sätze.
     */
    private static List<String> splitIntoSentences(String text) {
        List<String> sentences = new ArrayList<>();
        // Einfache Regex zur Satztrennung basierend auf Punkt, Ausrufezeichen und Fragezeichen
        // Berücksichtigt keine Abkürzungen oder spezielle Fälle
        String[] splits = text.split("(?<=[.!?])\\s+");
        for (String sentence : splits) {
            String trimmed = sentence.trim();
            if (!trimmed.isEmpty()) {
                sentences.add(trimmed);
            }
        }
        return sentences;
    }
}






