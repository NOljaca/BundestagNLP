package org.texttechnologylab.project.Nikola_Oljaca;

import org.texttechnologylab.project.Nikola_Oljaca.DatabaseMigration.Databaseconfigmigration;
import org.texttechnologylab.project.Nikola_Oljaca.nlp.Nlpmain;
import org.texttechnologylab.project.Nikola_Oljaca.DatabaseMigration.Restructure;
import org.texttechnologylab.project.Nikola_Oljaca.rest.RESTHandler;

import java.util.Scanner;

/**
 * Die Hauptklasse für die Steuerung der gesamten Anwendung.
 * Sie ermöglicht eine optionale Neuinitialisierung der Datenbank,
 * eine optionale NLP-Analyse, eine optionale Datenrestrukturierung (nach NLP),
 * und das optionale Starten der REST-API.
 */
public class Main {

    /**
     * Der Einstiegspunkt der Anwendung.
     *
     * @param args Konsolenargumente (derzeit nicht verwendet)
     */
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        boolean nlpAnalysisDone = false;  // Flag für NLP-Analyse

        //  Schritt 1: Optional die Datenbank neu initialisieren
        System.out.print("Möchten Sie die Datenbank neu initialisieren? (ja/nein): ");
        String dbResetChoice = scanner.nextLine().trim().toLowerCase();

        if (dbResetChoice.equals("ja")) {
            System.out.println("Starte Datenbank-Initialisierung...");
            Databaseconfigmigration.main(new String[]{});
            System.out.println("Datenbank wurde erfolgreich neu initialisiert!");
        } else {
            System.out.println("Überspringe Datenbank-Initialisierung...");
        }

        // Schritt 2: Optional NLP-Verarbeitung starten
        System.out.print("Möchten Sie die NLP-Analyse durchführen? (ja/nein): ");
        String nlpChoice = scanner.nextLine().trim().toLowerCase();

        if (nlpChoice.equals("ja")) {
            System.out.println("Starte NLP-Analyse...");
            Nlpmain.main(new String[]{});
            System.out.println("NLP-Analyse abgeschlossen!");
            nlpAnalysisDone = true;  // Flag setzen, da NLP durchgeführt wurde
        } else {
            System.out.println("NLP-Analyse übersprungen.");
        }

        //  Schritt 3: Restructure nur starten, wenn NLP-Analyse durchgeführt wurde
        if (nlpAnalysisDone) {
            System.out.print("Möchten Sie die Daten restrukturieren? (ja/nein): ");
            String restructureChoice = scanner.nextLine().trim().toLowerCase();

            if (restructureChoice.equals("ja")) {
                System.out.println("Starte Daten-Restrukturierung...");
                Restructure.main(new String[]{});
                System.out.println("Daten-Restrukturierung abgeschlossen!");
            } else {
                System.out.println("Daten-Restrukturierung übersprungen.");
            }
        } else {
            System.out.println("Daten-Restrukturierung kann nicht ausgeführt werden, da die NLP-Analyse nicht durchgeführt wurde.");
        }

        //  Schritt 4: Optional REST-API starten
        System.out.print("Möchten Sie die REST-API starten? (ja/nein): ");
        String restApiChoice = scanner.nextLine().trim().toLowerCase();

        if (restApiChoice.equals("ja")) {
            System.out.println("Starte REST-API...");
            RESTHandler.main(new String[]{});
        } else {
            System.out.println("REST-API-Start übersprungen.");
        }

        System.out.println("Programm beendet.");
    }
}

