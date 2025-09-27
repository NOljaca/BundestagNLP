package org.texttechnologylab.project.Nikola_Oljaca.nlp;

import org.apache.uima.cas.CASException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.util.XmlCasSerializer;
import org.apache.uima.resource.ResourceInitializationException;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;

public class Caseserialization {

    /**
     * Speichert den übergebenen JCas als XMI in die angegebene Datei.
     * Hier wird der FileOutputStream direkt an den XmlCasSerializer übergeben,
     * da dieser einen OutputStream erwartet.
     */
    public static void saveCas(JCas jcas, File file) throws IOException, CASException, SAXException {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            XmlCasSerializer.serialize(jcas.getCas(), fos);
            System.out.println("CAS in Datei " + file.getAbsolutePath() + " gespeichert.");
        }
    }

    /**
     * Lädt ein JCas aus der angegebenen XMI-Datei.
     *
     * @param file Die XMI-Datei, die gelesen werden soll
     * @return Der geladene JCas
     * @throws IOException
     * @throws ResourceInitializationException
     * @throws CASException
     */
    public static JCas loadCas(File file) throws IOException, ResourceInitializationException, CASException {
        JCas jcas = org.apache.uima.fit.factory.JCasFactory.createJCas();
        try (FileInputStream fis = new FileInputStream(file)) {
            org.apache.uima.util.CasIOUtils.load(fis, jcas.getCas());
            System.out.println("CAS aus Datei " + file.getAbsolutePath() + " geladen.");
        }
        return jcas;
    }
}




