package org.texttechnologylab.project.Nikola_Oljaca.rest;

import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
import io.javalin.Javalin;
import io.javalin.http.Context;

import com.mongodb.client.MongoCollection;
import org.bson.Document;
import com.mongodb.client.model.Filters;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Collectors;
import java.util.*;

/**
 * Die Klasse Routes definiert und registriert die verschiedenen HTTP-Endpunkte (Routen) für die Anwendung.
 * Sie stellt sowohl JSON-Endpunkte als auch HTML-Endpunkte bereit und kümmert sich um das Rendering
 * von Freemarker-Templates für die HTML-Ausgabe.
 */
public class Routes {

    /**
     * Registriert JSON-Endpunkte für CRUD-Operationen auf Collections.
     * Folgende Endpunkte werden bereitgestellt:
     * <ul>
     *     <li><code>/collections/{name}</code> (GET): Alle Dokumente der angegebenen Collection zurückgeben.</li>
     *     <li><code>/collections/{name}</code> (POST): Ein neues Dokument in die angegebene Collection einfügen.</li>
     *     <li><code>/collections/{name}</code> (DELETE): Die angegebene Collection komplett löschen.</li>
     * </ul>
     *
     * @param app Die Javalin-Instanz, in welche die Routen registriert werden.
     */
    public static void registerJsonRoutes(Javalin app) {
        app.get("/collections/{name}", Routes::getCollectionDocuments);
        app.post("/collections/{name}", Routes::addDocumentToCollection);
        app.delete("/collections/{name}", Routes::deleteCollection);
    }

    /**
     * Registriert HTML-Endpunkte für die Darstellung von Daten mittels Freemarker-Templates.
     * Folgende Endpunkte werden bereitgestellt:
     * <ul>
     *     <li><code>/</code>: Weiterleitung auf die Einstiegsseite.</li>
     *     <li><code>/einstiegsseite</code>: Übersicht aller Abgeordneten nach Fraktion, sortiert nach Partei und Name.</li>
     *     <li><code>/abgeordneten_portfolio/{id}</code>: Detailansicht eines spezifischen Abgeordneten.</li>
     *     <li><code>/search</code>: Suchfunktion nach Abgeordneten anhand des Namens oder Vornamens.</li>
     * </ul>
     *
     * @param app Die Javalin-Instanz, in welche die Routen registriert werden.
     */
    public static void registerHtmlRoutes(Javalin app) {
        app.get("/", ctx -> ctx.redirect("/einstiegsseite"));
        app.get("/einstiegsseite", Routes::renderSpeakersPage);
        app.get("/abgeordneten_portfolio/{id}", Routes::renderSpeakerDetailPage);
        app.get("/search", Routes::searchSpeakers);

        // Entferne oder kommentiere diese Zeile, falls vorhanden:
        // app.get("/video/{videoId}", new Videostreaminghandler());
    }


    /**
     * Gibt alle Dokumente aus einer angegebenen Mongo-Collection als JSON zurück.
     *
     * @param ctx Der Javalin-Context, enthält u.a. den Collection-Namen als Pfadparameter.
     */
    public static void getCollectionDocuments(Context ctx) {
        String collectionName = ctx.pathParam("name");
        MongoCollection<Document> collection = RESTHandler.getTargetDatabase().getCollection(collectionName);

        List<Document> documents = new ArrayList<>();
        for (Document doc : collection.find()) {
            documents.add(doc);
        }
        ctx.json(documents);
    }

    /**
     * Aktualisiert ein Dokument in einer angegebenen Collection basierend auf seiner ID.
     * Das zu aktualisierende Dokument wird aus dem Request-Body (JSON) entnommen.
     *
     * @param ctx Der Javalin-Context mit Collection-Namen, Dokument-ID im Pfad und neuen Werten im Body.
     */
    public static void updateDocumentInCollection(Context ctx) {
        String collectionName = ctx.pathParam("name");
        String documentId = ctx.pathParam("id");
        Document updatedDocument = Document.parse(ctx.body());

        MongoCollection<Document> collection = RESTHandler.getTargetDatabase().getCollection(collectionName);
        Document filter = new Document("_id", documentId);
        Document update = new Document("$set", updatedDocument);

        long modifiedCount = collection.updateOne(filter, update).getModifiedCount();

        if (modifiedCount > 0) {
            ctx.result("Dokument mit ID '" + documentId + "' wurde aktualisiert.");
        } else {
            ctx.status(404).result("Kein Dokument mit ID '" + documentId + "' gefunden.");
        }
    }

    /**
     * Fügt ein neues Dokument in die angegebene Collection ein. Das zu speichernde Dokument wird aus dem
     * Request-Body (JSON) entnommen.
     *
     * @param ctx Der Javalin-Context mit dem Collection-Namen im Pfad und dem neuen Dokument im Body.
     */
    public static void addDocumentToCollection(Context ctx) {
        String collectionName = ctx.pathParam("name");
        Document newDocument = Document.parse(ctx.body());
        MongoCollection<Document> collection = RESTHandler.getTargetDatabase().getCollection(collectionName);
        collection.insertOne(newDocument);
        ctx.status(201).result("Dokument hinzugefügt.");
    }

    /**
     * Löscht die angegebene MongoDB-Collection vollständig.
     *
     * @param ctx Der Javalin-Context mit dem Collection-Namen im Pfad.
     */
    public static void deleteCollection(Context ctx) {
        String collectionName = ctx.pathParam("name");
        RESTHandler.getTargetDatabase().getCollection(collectionName).drop();
        ctx.result("Collection '" + collectionName + "' wurde gelöscht.");
    }

    /**
     * Rendert die Einstiegsseite mit allen erfassten Abgeordneten, gruppiert nach Parteien.
     * Zusätzlich wird die Anzahl aller erfassten Abgeordneten ausgegeben.
     *
     * @param ctx Der Javalin-Context für die HTTP-Anfrage.
     */
    public static void renderSpeakersPage(Context ctx) {
        MongoCollection<Document> speakerCollection = RESTHandler.getTargetDatabase().getCollection("speaker");
        MongoCollection<Document> speechCollection = RESTHandler.getTargetDatabase().getCollection("speech");

        List<Document> speakers = speakerCollection.find().into(new ArrayList<>());
        int totalSpeakers = speakers.size();

        // Redenanzahl pro Speaker
        Map<String, Integer> speechCountMap = new HashMap<>();
        for (Document sp : speakers) {
            String speakerId = sp.getString("_id");
            long count = speechCollection.countDocuments(Filters.eq("speaker", speakerId));
            speechCountMap.put(speakerId, (int) count);
        }

        // Liste aller Speaker mit Partei und Redenanzahl
        List<Map<String, Object>> speakerList = new ArrayList<>();
        for (Document doc : speakers) {
            Map<String, Object> s = new HashMap<>();
            s.put("id", doc.getString("_id"));
            s.put("name", doc.getString("name"));
            s.put("firstName", doc.getString("firstName"));

            String partyValue = doc.getString("party");
            if (partyValue == null || partyValue.isBlank()) {
                partyValue = "sonstige";
            }
            s.put("party", partyValue);

            int speechCount = speechCountMap.getOrDefault(doc.getString("_id"), 0);
            s.put("speechCount", speechCount);

            speakerList.add(s);
        }

        // Sortierung nach Partei und dann nach Vorname
        speakerList.sort(
                Comparator.comparing(
                        (Map<String, Object> sp) -> (String) sp.get("party"),
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
                ).thenComparing(
                        sp -> (String) sp.get("firstName"),
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
                )
        );

        // Gruppierung nach Partei
        List<Map<String, Object>> parties = new ArrayList<>();
        String currentParty = null;
        List<Map<String, Object>> currentPartySpeakers = null;

        for (Map<String, Object> sp : speakerList) {
            String p = (String) sp.get("party");
            if (!p.equals(currentParty)) {
                currentParty = p;
                currentPartySpeakers = new ArrayList<>();
                Map<String, Object> partyMap = new HashMap<>();
                partyMap.put("partyName", currentParty);
                partyMap.put("speakers", currentPartySpeakers);
                parties.add(partyMap);
            }
            currentPartySpeakers.add(sp);
        }

        Map<String, Object> model = new HashMap<>();
        model.put("parties", parties);
        model.put("totalSpeakers", totalSpeakers);

        ctx.render("einstiegsseite.ftl", model);
    }

    /**
     * Rendert die Detailseite eines bestimmten Abgeordneten, inklusive Biografie, Mitgliedschaften,
     * Reden (mit Kommentaren) sowie einer Änderungshistorie für Kommentare und Bild-URL.
     *
     * @param ctx Der Javalin-Context mit der Abgeordneten-ID als Pfadparameter.
     */
    public static void renderSpeakerDetailPage(Context ctx) {
        String speakerId = ctx.pathParam("id");
        System.out.println("DEBUG: Speaker-ID aus PathParam: " + speakerId);

        MongoCollection<Document> speakerCollection = RESTHandler.getTargetDatabase().getCollection("speaker");
        MongoCollection<Document> speechCollection = RESTHandler.getTargetDatabase().getCollection("speech");
        MongoCollection<Document> commentHistoryCollection = RESTHandler.getTargetDatabase().getCollection("comment_history");
        MongoCollection<Document> imageHistoryCollection = RESTHandler.getTargetDatabase().getCollection("image_history");

        Document speaker = speakerCollection.find(Filters.eq("_id", speakerId)).first();
        if (speaker == null) {
            ctx.status(404).result("Abgeordneter nicht gefunden.");
            return;
        }

        List<Document> allSpeakers = speakerCollection.find().into(new ArrayList<>());
        List<String> speakerNames = allSpeakers.stream()
                .map(sp -> sp.getString("firstName") + " " + sp.getString("name"))
                .collect(Collectors.toList());

        List<String> factions = Arrays.asList("CDU/CSU", "SPD", "FDP", "BÜNDNIS 90/DIE GRÜNEN", "AfD", "DIE LINKE", "sonstige");

        // **Reden des Abgeordneten laden**
        List<Document> speeches = speechCollection.find(Filters.eq("speaker", speakerId)).into(new ArrayList<>());
        System.out.println("DEBUG: Gefundene Reden für Sprecher " + speakerId + ": " + speeches.size());

        List<Map<String, Object>> speechList = new ArrayList<>();
        for (Document speech : speeches) {
            Map<String, Object> speechData = new HashMap<>();
            String speechId = speech.getString("_id");

            if (speechId == null) {
                System.out.println("WARNUNG: `speech._id` ist null! Rede wird übersprungen.");
                continue;
            }

            speechData.put("id", speechId);
            System.out.println("DEBUG: Verarbeite Rede mit ID: " + speechId);

            Document protocol = speech.get("protocol", Document.class);
            speechData.put("title", protocol != null ? protocol.getString("title") : "Ohne Titel");
            speechData.put("date", protocol != null ? protocol.getLong("date") : 0);

            Document agenda = speech.get("agenda", Document.class);
            speechData.put("agenda", agenda != null ? agenda.getString("id") : "Unbekannt");

            //  **NLP-Analysen (POS-Typen & Named Entities)**
            Document analysis = speech.get("analysis", Document.class);
            Map<String, Object> nlpStats = new HashMap<>();
            if (analysis != null) {
                List<String> posTags = analysis.getList("posTags", String.class);
                Map<String, Integer> posCounts = new HashMap<>();
                if (posTags != null) {
                    for (String pos : posTags) {
                        String posType = pos.replaceAll(".*\\[([A-Z$]+)\\]$", "$1");
                        posCounts.put(posType, posCounts.getOrDefault(posType, 0) + 1);
                    }
                }
                nlpStats.put("posCounts", posCounts);

                Document namedEntitiesCounts = analysis.get("namedEntitiesCounts", Document.class);
                if (namedEntitiesCounts != null) {
                    Map<String, Integer> neCounts = new HashMap<>();
                    for (Map.Entry<String, Object> entry : namedEntitiesCounts.entrySet()) {
                        neCounts.put(entry.getKey(), ((Number) entry.getValue()).intValue());
                    }
                    nlpStats.put("namedEntitiesCounts", neCounts);
                } else {
                    nlpStats.put("namedEntitiesCounts", Collections.emptyMap());
                }
            } else {
                nlpStats.put("posCounts", Collections.emptyMap());
                nlpStats.put("namedEntitiesCounts", Collections.emptyMap());
            }

            speechData.put("nlpStats", nlpStats);

            //  **Textinhalte der Rede**
            List<Document> textContent = speech.getList("textContent", Document.class);
            List<Map<String, Object>> contentList = new ArrayList<>();

            for (Document content : textContent) {
                Map<String, Object> contentMap = new HashMap<>();
                contentMap.put("id", content.getString("id"));
                contentMap.put("type", content.getString("type"));
                contentMap.put("text", content.getString("text"));
                contentMap.put("assignedTo", content.getString("assignedTo") != null ? content.getString("assignedTo") : "Unbekannt");

                if ("text".equalsIgnoreCase(content.getString("type"))) {
                    List<Double> sentimentValues = content.getList("sentimentValues", Double.class);
                    double averageSentiment = 0.0;
                    if (sentimentValues != null && !sentimentValues.isEmpty()) {
                        averageSentiment = sentimentValues.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                    }
                    String sentimentColor = getColorForSentiment(averageSentiment);
                    contentMap.put("sentimentColor", sentimentColor);
                }

                contentList.add(contentMap);
            }

            speechData.put("contentList", contentList);

            // **Video-URL basierend auf der Speech-ID**
            String videoUrl = "/video/" + speechId + ".mp4";
            speechData.put("videoUrl", videoUrl);

            speechList.add(speechData);
        }

        //  **Kommentar- und Bild-Historie laden**
        List<Document> commentHistoryEntries = commentHistoryCollection.find().into(new ArrayList<>());
        List<Document> imageHistoryEntries = imageHistoryCollection.find().into(new ArrayList<>());

        List<Map<String, Object>> unifiedHistory = new ArrayList<>();
        for (Document history : commentHistoryEntries) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("type", "comment");
            entry.put("timestamp", history.getLong("timestamp"));
            entry.put("contentId", history.getString("contentId"));
            entry.put("oldValue", history.getString("oldAssignedTo"));
            entry.put("newValue", history.getString("newAssignedTo"));
            unifiedHistory.add(entry);
        }

        for (Document history : imageHistoryEntries) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("type", "image");
            entry.put("timestamp", history.getLong("timestamp"));
            entry.put("speakerId", history.getString("speakerId"));
            entry.put("oldValue", history.getString("oldImageUrl"));
            entry.put("newValue", history.getString("newImageUrl"));
            unifiedHistory.add(entry);
        }

        unifiedHistory.sort((o1, o2) -> Long.compare((Long) o2.get("timestamp"), (Long) o1.get("timestamp")));

        //  **Zusätzliche Infos über den Abgeordneten**
        Map<String, Object> model = new HashMap<>();
        model.put("party", speaker.getString("party") != null ? speaker.getString("party") : "Parteilos");
        model.put("id", speaker.getString("_id"));
        model.put("name", speaker.getString("name"));
        model.put("firstName", speaker.getString("firstName"));
        model.put("title", speaker.getString("title") != null ? speaker.getString("title") : "");
        model.put("birthDate", speaker.getDate("geburtsdatum"));
        model.put("birthPlace", speaker.getString("geburtsort") != null ? speaker.getString("geburtsort") : "Unbekannt");
        model.put("deathDate", speaker.getDate("sterbedatum"));
        model.put("gender", speaker.getString("geschlecht") != null ? speaker.getString("geschlecht") : "Unbekannt");
        model.put("occupation", speaker.getString("beruf") != null ? speaker.getString("beruf") : "Unbekannt");
        model.put("familyStatus", speaker.getString("familienstand"));
        model.put("religion", speaker.getString("religion"));
        model.put("academicTitle", speaker.getString("akademischertitel"));
        model.put("vita", speaker.getString("vita"));
        model.put("speeches", speechList);
        model.put("history", unifiedHistory);
        model.put("allSpeakers", speakerNames);
        model.put("factions", factions);
        model.put("imageUrl", speaker.getString("imageUrl"));

        ctx.render("abgeordneten_portfolio.ftl", model);
    }


    /**
     * Führt eine Suche nach Abgeordneten durch, deren Vor- oder Nachname den Suchbegriff enthält.
     * Wenn kein Suchbegriff eingegeben ist, wird die Einstiegsseite mit allen Abgeordneten angezeigt.
     * Andernfalls werden nur die passenden Suchergebnisse gelistet.
     *
     * @param ctx Der Javalin-Context, der einen optionalen Query-Parameter "q" (Suchstring) enthält.
     */
    public static void searchSpeakers(Context ctx) {
        String query = ctx.queryParam("q");
        MongoCollection<Document> speakerCollection = RESTHandler.getTargetDatabase().getCollection("speaker");
        MongoCollection<Document> speechCollection = RESTHandler.getTargetDatabase().getCollection("speech");

        Map<String, Object> model = new HashMap<>();

        if (query == null || query.isBlank()) {
            // Keine Sucheingabe => Ausgabe wie Startseite

            List<Document> speakers = speakerCollection.find().into(new ArrayList<>());
            int totalSpeakers = speakers.size();

            Map<String, Integer> speechCountMap = new HashMap<>();
            for (Document sp : speakers) {
                String speakerId = sp.getString("_id");
                long count = speechCollection.countDocuments(Filters.eq("speaker", speakerId));
                speechCountMap.put(speakerId, (int) count);
            }

            List<Map<String, Object>> speakerList = new ArrayList<>();
            for (Document doc : speakers) {
                Map<String, Object> s = new HashMap<>();
                s.put("id", doc.getString("_id"));
                s.put("name", doc.getString("name"));
                s.put("firstName", doc.getString("firstName"));

                String partyValue = doc.getString("party");
                if (partyValue == null || partyValue.isBlank()) {
                    partyValue = "sonstige";
                }
                s.put("party", partyValue);

                int speechCount = speechCountMap.getOrDefault(doc.getString("_id"), 0);
                s.put("speechCount", speechCount);

                speakerList.add(s);
            }

            speakerList.sort(
                    Comparator.comparing(
                            (Map<String, Object> sp) -> (String) sp.get("party"),
                            Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
                    ).thenComparing(
                            sp -> (String) sp.get("firstName"),
                            Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
                    )
            );

            List<Map<String, Object>> parties = new ArrayList<>();
            String currentParty = null;
            List<Map<String, Object>> currentPartySpeakers = null;

            for (Map<String, Object> sp : speakerList) {
                String p = (String) sp.get("party");
                if (!p.equals(currentParty)) {
                    currentParty = p;
                    currentPartySpeakers = new ArrayList<>();
                    Map<String, Object> partyMap = new HashMap<>();
                    partyMap.put("partyName", currentParty);
                    partyMap.put("speakers", currentPartySpeakers);
                    parties.add(partyMap);
                }
                currentPartySpeakers.add(sp);
            }

            model.put("parties", parties);
            model.put("totalSpeakers", totalSpeakers);

        } else {
            // Suche nach bestimmten Abgeordneten
            List<Document> results = speakerCollection.find(Filters.or(
                    Filters.regex("name", query, "i"),
                    Filters.regex("firstName", query, "i"))
            ).into(new ArrayList<>());

            int totalSpeakers = results.size();

            List<Map<String, Object>> speakerList = new ArrayList<>();
            for (Document doc : results) {
                Map<String, Object> s = new HashMap<>();
                s.put("id", doc.getString("_id"));
                s.put("name", doc.getString("name"));
                s.put("firstName", doc.getString("firstName"));

                String partyValue = doc.getString("party");
                if (partyValue == null || partyValue.isBlank()) {
                    partyValue = "sonstige";
                }
                s.put("party", partyValue);

                long speechCount = speechCollection.countDocuments(Filters.eq("speaker", doc.getString("_id")));
                s.put("speechCount", speechCount);

                speakerList.add(s);
            }

            speakerList.sort(
                    Comparator.comparing(
                            (Map<String, Object> sp) -> (String) sp.get("party"),
                            Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
                    ).thenComparing(
                            sp -> (String) sp.get("name"),
                            Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
                    )
            );

            model.put("query", query);
            model.put("searchResults", speakerList);
            model.put("totalSpeakers", totalSpeakers);
        }

        ctx.render("einstiegsseite.ftl", model);
    }

    /**
     * Aktualisiert die Zuordnung eines Kommentars zu einem Abgeordneten oder einer Fraktion.
     * Gleichzeitig wird die Änderung in der Kommentar-Historie festgehalten.
     *
     * @param ctx Der Javalin-Context mit den Formularparametern "contentId" und "assignedTo".
     */
    public static void updateCommentAssignment(Context ctx) {
        String contentId = ctx.formParam("contentId");
        String assignedTo = ctx.formParam("assignedTo");

        if (contentId == null || assignedTo == null) {
            ctx.status(400).result("Fehlende Parameter: contentId oder assignedTo.");
            return;
        }

        MongoCollection<Document> speechCollection = RESTHandler.getTargetDatabase().getCollection("speech");
        MongoCollection<Document> commentHistoryCollection = RESTHandler.getTargetDatabase().getCollection("comment_history");

        Document speech = speechCollection.find(Filters.elemMatch("textContent", Filters.eq("id", contentId))).first();
        if (speech == null) {
            ctx.status(404).result("Kommentar nicht gefunden.");
            return;
        }

        List<Document> textContent = speech.getList("textContent", Document.class);
        for (Document content : textContent) {
            if (contentId.equals(content.getString("id"))) {
                String oldAssignedTo = content.getString("assignedTo");

                // Historieneintrag speichern
                Document historyEntry = new Document("contentId", contentId)
                        .append("oldAssignedTo", oldAssignedTo != null ? oldAssignedTo : "Unbekannt")
                        .append("newAssignedTo", assignedTo)
                        .append("timestamp", System.currentTimeMillis());
                commentHistoryCollection.insertOne(historyEntry);

                content.put("assignedTo", assignedTo);
                break;
            }
        }

        // Update in der Datenbank
        speechCollection.updateOne(Filters.eq("_id", speech.getString("_id")),
                Updates.set("textContent", textContent));

        ctx.redirect(ctx.header("Referer"));
    }

    /**
     * Aktualisiert das Bild-URL eines Abgeordneten und protokolliert die Änderung in einer Bild-Historie.
     *
     * @param ctx Der Javalin-Context mit den Parametern "speakerId" und "imageUrl".
     */
    public static void updateImageAssignment(Context ctx) {
        String speakerId = ctx.formParam("speakerId");
        String newImageUrl = ctx.formParam("imageUrl");

        if (speakerId == null || newImageUrl == null) {
            ctx.status(400).result("Fehlende Parameter: speakerId oder imageUrl.");
            return;
        }

        MongoCollection<Document> speakerCollection = RESTHandler.getTargetDatabase().getCollection("speaker");
        MongoCollection<Document> imageHistoryCollection = RESTHandler.getTargetDatabase().getCollection("image_history");

        Document speaker = speakerCollection.find(Filters.eq("_id", speakerId)).first();
        if (speaker == null) {
            ctx.status(404).result("Abgeordneter nicht gefunden.");
            return;
        }

        String oldImageUrl = speaker.getString("imageUrl");

        // Historieneintrag speichern
        Document historyEntry = new Document("speakerId", speakerId)
                .append("oldImageUrl", oldImageUrl != null ? oldImageUrl : "Unbekannt")
                .append("newImageUrl", newImageUrl)
                .append("timestamp", System.currentTimeMillis());
        imageHistoryCollection.insertOne(historyEntry);

        // Bild-URL aktualisieren
        speakerCollection.updateOne(Filters.eq("_id", speakerId),
                Updates.set("imageUrl", newImageUrl));

        ctx.redirect(ctx.header("Referer"));
    }

    /**
     * Registriert die Routen für Kommentar- und Bild-Updates.
     * <ul>
     *     <li><code>/update-comment</code>: Zum Aktualisieren der Kommentarzuordnung</li>
     *     <li><code>/update-image</code>: Zum Aktualisieren der Bild-URL eines Abgeordneten</li>
     * </ul>
     *
     * @param app Die Javalin-Instanz, in die die Routen registriert werden.
     */
    public static void registerCommentRoutes(Javalin app) {
        app.post("/update-comment", Routes::updateCommentAssignment);
        app.post("/update-image", Routes::updateImageAssignment);
    }
    /**
     * Wandelt einen Sentiment-Wert in eine entsprechende Hintergrundfarbe um.
     * -1 = Rot (#FF0000), 0 = Weiß (#FFFFFF), 1 = Grün (#00FF00).
     * Die Farbe wird kontinuierlich basierend auf dem Wert skaliert.
     *
     * @param sentiment Der Sentiment-Wert (z.B. -1.0 bis 1.0).
     * @return Der HEX-Code der entsprechenden Farbe.
     */
    public static String getColorForSentiment(double sentiment) {
        // Clip den Wert auf den Bereich [-1, 1]
        sentiment = Math.max(-1.0, Math.min(1.0, sentiment));

        int red, green, blue;

        if (sentiment < 0) {
            // Von Rot (#FF0000) nach Weiß (#FFFFFF)
            double ratio = (sentiment + 1.0) / 1.0; // ratio von 0 (sentiment=-1) bis 1 (sentiment=0)
            red = 255;
            green = (int) (255 * ratio);
            blue = (int) (255 * ratio);
        } else {
            // Von Weiß (#FFFFFF) nach Grün (#00FF00)
            double ratio = sentiment / 1.0; // ratio von 0 (sentiment=0) bis 1 (sentiment=1)
            red = (int) (255 * (1 - ratio));
            green = 255;
            blue = (int) (255 * (1 - ratio));
        }

        return String.format("#%02X%02X%02X", red, green, blue);
    }



}





