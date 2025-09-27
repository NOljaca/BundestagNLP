package org.texttechnologylab.project.Nikola_Oljaca.nlp;

import com.mongodb.client.MongoDatabase;
import org.apache.commons.io.FileUtils;
import org.apache.uima.cas.CASException;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.jcas.tcas.Annotation;
import org.bson.Document;
import org.texttechnologylab.DockerUnifiedUIMAInterface.DUUIComposer;
import org.texttechnologylab.DockerUnifiedUIMAInterface.driver.DUUIRemoteDriver;
import org.texttechnologylab.DockerUnifiedUIMAInterface.lua.DUUILuaContext;
import org.texttechnologylab.project.Nikola_Oljaca.DatabaseMigration.Videoresultsaver;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Beispiel-Klasse für die Videoanalyse:
 *  1) Aufruf von WhisperX (chunk_method=word, token_timestamps=true),
 *  2) Debug-Ausgabe aller Annotationen im "transcript"-View,
 *  3) Erzeugen zusätzlich naive Satzaufteilung (ohne BreakIterator) + Zeitverteilung,
 *  4) Speichern Transkript und sentenceList in MongoDB.
 */
public class VideonlpMain {

    /**
     * Erzeugt ein Grund-CAS mit leerem Dokument.
     */
    public static JCas createEmptyCas() throws ResourceInitializationException, CASException {
        JCas jcas = JCasFactory.createJCas();
        jcas.setDocumentText("");
        jcas.setDocumentLanguage("de");
        return jcas;
    }

    /**
     * Führt die WhisperX-Analyse via DUUIRemoteDriver aus.
     * chunk_method=word + token_timestamps=true
     * (Optional: .withParameter("align_model","wav2vec2"))
     */
    public static void runWhisperX(JCas cas, byte[] videoBytes, String mimeType) throws Exception {
        // 1) Base64
        String base64 = Base64.getEncoder().encodeToString(videoBytes);

        // 2) "video"-View
        JCas videoView = cas.createView("video");
        videoView.setSofaDataString(base64, mimeType);
        videoView.setDocumentLanguage("de");

        // 3) "transcript"-View
        cas.createView("transcript").setDocumentLanguage("de");

        // 4) Composer
        DUUILuaContext ctx = new DUUILuaContext().withJsonLibrary();
        DUUIComposer composer = new DUUIComposer()
                .withSkipVerification(true)
                .withLuaContext(ctx)
                .withWorkers(1);

        composer.addDriver(new DUUIRemoteDriver());

        // 5) WhisperX-Komponente
        DUUIRemoteDriver.Component whisperComponent = new DUUIRemoteDriver.Component("http://whisperx.lehre.texttechnologylab.org")
                .withScale(1)
                .withSourceView("video")
                .withTargetView("transcript")
                .withParameter("chunk_method", "word")
                .withParameter("token_timestamps", "true");
        // .withParameter("align_model","wav2vec2"); // Falls nötig

        composer.add(whisperComponent.build());
        composer.run(cas);
    }

    /**
     * Debug-Print: Gibt alle Annotationen + Features im "transcript"-View aus.
     */
    public static void debugPrintAnnotations(JCas transcriptView) {
        System.out.println("\n=== DEBUG: Annotationen im transcript-View ===");
        for (Object obj : org.apache.uima.fit.util.JCasUtil.selectAll(transcriptView)) {
            if (obj instanceof Annotation) {
                Annotation ann = (Annotation) obj;
                String typeName = ann.getType().getName();
                String coveredText = ann.getCoveredText().replace("\n","\\n");
                int begin = ann.getBegin();
                int end   = ann.getEnd();

                System.out.println(" -> " + typeName + " [" + begin + ".." + end + "] = \"" + coveredText + "\"");
                ann.getType().getFeatures().forEach(f -> {
                    String featName = f.getShortName();
                    String featVal  = ann.getFeatureValueAsString(f);
                    System.out.println("    Feature '" + featName + "' = " + featVal);
                });
            }
        }
        System.out.println("=== Ende DEBUG ===");
    }

    /**
     * 1) Schlägt naive Sätze per Split am Punkt ('.') vor. (Ohne BreakIterator, nur minimal.)
     * 2) Verteilt die Zeitspanne "timeStart..timeEnd" proportional nach Zeichenzahl.
     */
    public static List<Document> naiveSentencesWithTime(String text, double timeStart, double timeEnd) {
        List<Document> sents = new ArrayList<>();

        // 1) Sätze trennen - naive Split am Punkt + Leerzeichen
        String[] rawSents = text.split("\\.\\s+");
        // Filter leere
        List<String> cleaned = new ArrayList<>();
        for (String s : rawSents) {
            String trimmed = s.trim();
            if (!trimmed.isEmpty()) {
                cleaned.add(trimmed);
            }
        }

        int totalChars = cleaned.stream().mapToInt(String::length).sum();
        if (totalChars == 0 || cleaned.isEmpty()) {
            // Falls wir gar keine Sätze finden => Rückgabe Leer
            return sents;
        }

        double totalTime = timeEnd - timeStart;
        double currentStart = timeStart;

        for (int i = 0; i < cleaned.size(); i++) {
            String sentenceText = cleaned.get(i);
            int len = sentenceText.length();
            double ratio = (double) len / totalChars;
            double chunk = ratio * totalTime;

            double segStart = currentStart;
            double segEnd   = currentStart + chunk;

            // Falls wir den Punkt "." abgeschnitten haben, ggf. wieder anfügen
            // hier optional
            if (!sentenceText.endsWith(".")) {
                sentenceText += ".";
            }

            Document d = new Document("text", sentenceText)
                    .append("startTime", segStart)
                    .append("endTime",   segEnd);

            sents.add(d);

            currentStart = segEnd;
        }
        return sents;
    }

    /**
     * processedSingleVideo:
     *  - lädt Video,
     *  - runWhisperX -> transcript,
     *  - debugPrint,
     *  - naive "sentenceList" generieren (zeitlich proportional),
     *  - Speichern in DB => (videoAnalysis, sentenceList).
     */
    public static void processSingleVideo(String videoResourcePath, MongoDatabase db) throws Exception {
        // 1) Video-Datei lesen
        ClassLoader cl = VideonlpMain.class.getClassLoader();
        URL videoURL = cl.getResource(videoResourcePath);
        if (videoURL == null) {
            throw new IOException("Video nicht gefunden: " + videoResourcePath);
        }
        File file = new File(videoURL.toURI());
        byte[] videoBytes = FileUtils.readFileToByteArray(file);

        // MIME
        String mimeType = Files.probeContentType(Path.of(file.getAbsolutePath()));
        if (mimeType == null) {
            mimeType = "video/mp4";
        }

        // 2) CAS
        JCas cas = createEmptyCas();
        runWhisperX(cas, videoBytes, mimeType);

        // 3) Debug
        JCas transcriptView = cas.getView("transcript");
        debugPrintAnnotations(transcriptView);

        // 4) Transkript-Text
        String transcript = transcriptView.getDocumentText();
        System.out.println("[VideoNlpMain] Transkript für " + file.getName() + ":\n" + transcript);

        // 5) (Entfernt) Token extrahieren, da tokenList leer ist
        // List<Document> tokenList = extractTokens(transcriptView); // Entfernt

        // 6) Zeitspanne festlegen (Placeholder)
        double audioStart = 0.0;
        double audioEnd   = 30.0; // Placeholder, anpassen je nach tatsächlicher Dauer

        // 7) Naive Satzliste: verteile text [audioStart..audioEnd]
        List<Document> sentenceList = naiveSentencesWithTime(transcript, audioStart, audioEnd);

        // 8) In MongoDB speichern
        // -> Felder: videoAnalysis (string), sentenceList ([])
        Videoresultsaver saver = new Videoresultsaver("video");
        saver.saveTranscriptWithSentences(
                file.getName(),
                transcript,
                sentenceList
        );
    }

    /**
     * ProzessAllVideos: Scannt alle .mp4 im Ordner -> processSingleVideo()
     */
    public static void processAllVideos(String resourceFolderPath, MongoDatabase db) throws Exception {
        if (resourceFolderPath == null || resourceFolderPath.isEmpty()) {
            System.out.println("[VideoNlpMain] Kein Videoordner, Abbruch.");
            return;
        }
        ClassLoader cl = VideonlpMain.class.getClassLoader();
        URL folderURL = cl.getResource(resourceFolderPath);
        if (folderURL == null) {
            System.out.println("[VideoNlpMain] Ordner nicht gefunden: " + resourceFolderPath);
            return;
        }
        File folder = new File(folderURL.toURI());
        File[] mp4s = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".mp4"));
        if (mp4s == null || mp4s.length == 0) {
            System.out.println("[VideoNlpMain] Keine .mp4-Dateien in " + resourceFolderPath);
            return;
        }

        for (File f : mp4s) {
            try {
                System.out.println("\n=== Starte Analyse für: " + f.getName() + " ===");
                processSingleVideo(resourceFolderPath + "/" + f.getName(), db);
            } catch (Exception e) {
                System.err.println("[VideoNlpMain] Fehler bei " + f.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Main zum direkten Testen (unabhängig von NlpMain).
     */
    public static void main(String[] args) {
        try {
            // DB null => wir benutzen den default 'VideoResultSaver("video")'
            processAllVideos("1", null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

























