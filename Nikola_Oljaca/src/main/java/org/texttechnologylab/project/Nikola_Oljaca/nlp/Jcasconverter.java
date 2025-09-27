package org.texttechnologylab.project.Nikola_Oljaca.nlp;

import org.apache.uima.cas.CASException;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.texttechnologylab.project.Nikola_Oljaca.model.Speech;
import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;

/**
 * Kapselt die Logik, aus einer Speech ein UIMA-JCas zu erzeugen.
 */
public class Jcasconverter {

    /**
     * Konvertiert ein Speech-Objekt in ein neues JCas (DocumentText, MetaData etc.).
     */
    public static JCas convert(Speech speech) throws ResourceInitializationException, CASException {
        // 1) Leeres JCas erzeugen
        JCas jcas = JCasFactory.createJCas();

        // 2) Text und Sprache setzen (Fallback auf leeren Text)
        String text = speech.getText();
        if (text == null) {
            text = "";
        }
        jcas.setDocumentText(text);
        jcas.setDocumentLanguage("de"); // Sprache setzen

        // 3) DocumentMetaData initialisieren
        DocumentMetaData dmd = new DocumentMetaData(jcas);
        dmd.setDocumentId(speech.getId());
        dmd.setDocumentTitle("Rede von " + speech.getSpeaker());
        dmd.addToIndexes();

        return jcas;
    }
}



