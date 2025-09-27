package org.texttechnologylab.project.Nikola_Oljaca.rest;

import org.texttechnologylab.project.Nikola_Oljaca.DatabaseMigration.Databaseconfigmigration;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoClient;

/**
 * Die Klasse RESTHandler dient als Einstiegspunkt für die Anwendung.
 */
public class RESTHandler {

    private static MongoDatabase targetDatabase;

    public static void main(String[] args) {
        try {
            System.out.println("Lade Datenbankkonfiguration...");

            Databaseconfigmigration.loadDatabaseConfig();
            String uri = Databaseconfigmigration.getTargetDatabaseUri();
            String dbName = Databaseconfigmigration.getTargetDatabaseName();
            System.out.println("Verbindungs-URI: " + uri);
            System.out.println("Datenbankname: " + dbName);

            MongoClient mongoClient = MongoClients.create(uri);
            targetDatabase = mongoClient.getDatabase(dbName);

            System.out.println("Verbindung zur Datenbank erfolgreich!");

            int port = 7080;
            var app = JavalinConfig.createApp(port);

            app.before(ctx -> {
                ctx.header("Access-Control-Allow-Origin", "*");
                ctx.header("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
                ctx.header("Access-Control-Allow-Headers", "Content-Type,Accept");
            });

            Routes.registerJsonRoutes(app);
            Routes.registerHtmlRoutes(app);
            Routes.registerCommentRoutes(app);

            System.out.println("REST-API und HTML-Seiten laufen auf http://localhost:" + port);
        } catch (Exception e) {
            System.err.println("Fehler beim Starten der REST-API: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static MongoDatabase getTargetDatabase() {
        return targetDatabase;
    }
}





















