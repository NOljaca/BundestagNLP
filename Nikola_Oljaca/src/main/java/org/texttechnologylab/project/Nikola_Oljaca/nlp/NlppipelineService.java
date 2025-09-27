package org.texttechnologylab.project.Nikola_Oljaca.nlp;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;   // Angepasster Import für Named Entities
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency; // Import für Dependencies
import org.hucompute.textimager.uima.type.Sentiment;
import org.apache.uima.jcas.JCas;
import org.apache.uima.fit.util.JCasUtil;
import org.texttechnologylab.DockerUnifiedUIMAInterface.DUUIComposer;
import org.texttechnologylab.DockerUnifiedUIMAInterface.driver.DUUIDockerDriver;
import org.texttechnologylab.DockerUnifiedUIMAInterface.lua.DUUILuaContext;
import org.xml.sax.SAXException;
import org.apache.uima.UIMAException;
import org.bson.Document;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Diese Klasse kapselt die NLP-Pipeline via DUUI (DockerDriver) und erweitert
 * die Extraktion um zusätzliche linguistische Merkmale:
 * - Sätze inkl. Sentiment
 * - Tokens
 * - POS-Tags
 * - Dependencies
 * - Named Entities (NER)
 *
 * Darüber hinaus wird in der Methode buildAnalysisDocument ein strukturiertes BSON‑Document erzeugt,
 * das alle extrahierten Ergebnisse enthält. Neben den Listen der einzelnen Elemente wird auch eine
 * Zählung der Named Entities (z. B. PER, LOC, ORG, MISC) als Unterdokument gespeichert.
 */
public class NlppipelineService {

    private DUUIComposer composer;
    private DUUIDockerDriver dockerDriver;
    private int workers = 1; // Anzahl paralleler Worker (Threads)

    /**
     * Konstruktor: Initialisiert den DUUIComposer, DockerDriver und fügt die spaCy- und GerVader-Komponenten hinzu.
     */
    public NlppipelineService() throws Exception {
        initComposer();
        initDockerDriver();
        addSpacyComponent();
        addGerVaderComponent();
    }

    private void initComposer() throws Exception {
        DUUILuaContext ctx = new DUUILuaContext().withJsonLibrary();
        this.composer = new DUUIComposer()
                .withSkipVerification(true)
                .withLuaContext(ctx)
                .withWorkers(workers);
    }

    private void initDockerDriver() throws IOException, UIMAException, SAXException {
        this.dockerDriver = new DUUIDockerDriver();
        this.composer.addDriver(this.dockerDriver);
    }

    /**
     * Fügt der Pipeline die spaCy-Komponente hinzu, welche für Tokenisierung, Satzsegmentierung,
     * POS-Tagging, Dependency Parsing und Named Entity Recognition zuständig ist.
     */
    private void addSpacyComponent() throws Exception {
        DUUIDockerDriver.Component spacy = new DUUIDockerDriver.Component(
                "docker.texttechnologylab.org/textimager-duui-spacy-single-de_core_news_sm:0.1.4"
        )
                .withImageFetching()
                .withScale(workers);
        this.composer.add(spacy.build());
    }

    /**
     * Fügt der Pipeline die GerVader-Komponente für die Sentiment-Analyse (Deutsch) hinzu.
     */
    private void addGerVaderComponent() throws Exception {
        DUUIDockerDriver.Component gervader = new DUUIDockerDriver.Component(
                "docker.texttechnologylab.org/gervader_duui:1.0.2"
        )
                .withParameter("selection", "text")
                .withImageFetching()
                .withScale(workers);
        this.composer.add(gervader.build());
    }

    /**
     * Führt die Pipeline auf dem übergebenen JCas aus.
     *
     * @param jcas Das CAS, das durch die NLP-Pipeline verarbeitet werden soll.
     * @throws Exception
     */
    public void processJCas(JCas jcas) throws Exception {
        this.composer.run(jcas);
    }


    /**
     * Extrahiert alle Tokens aus dem JCas.
     *
     * @param jcas Das zu analysierende CAS.
     * @return Eine Liste von Token-Texten.
     */
    public List<String> extractTokens(JCas jcas) {
        List<String> tokens = new ArrayList<>();
        for (Token token : JCasUtil.select(jcas, Token.class)) {
            tokens.add(token.getCoveredText());
        }
        return tokens;
    }

    /**
     * Extrahiert POS-Tags zu jedem Token.
     * (Annahme: Token besitzt ein POS-Attribut, das über getPos().getPosValue() abrufbar ist.)
     *
     * @param jcas Das zu analysierende CAS.
     * @return Eine Liste, in der jeder Eintrag das Token und seinen zugehörigen POS-Tag enthält.
     */
    public List<String> extractPOSTags(JCas jcas) {
        List<String> posList = new ArrayList<>();
        for (Token token : JCasUtil.select(jcas, Token.class)) {
            String tokenText = token.getCoveredText();
            String posTag = (token.getPos() != null) ? token.getPos().getPosValue() : "unbekannt";
            posList.add(tokenText + " [" + posTag + "]");
        }
        return posList;
    }

    /**
     * Extrahiert Dependencies aus dem JCas.
     * Wir verwenden hierfür den Dependency-Typ aus dem Package
     * de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.
     *
     * @param jcas Das zu analysierende CAS.
     * @return Eine Liste von Strings, die die Abhängigkeiten beschreiben.
     */
    public List<String> extractDependencies(JCas jcas) {
        List<String> dependencies = new ArrayList<>();
        for (Dependency dep : JCasUtil.select(jcas, Dependency.class)) {
            String governor = (dep.getGovernor() != null) ? dep.getGovernor().getCoveredText() : "null";
            String dependent = (dep.getDependent() != null) ? dep.getDependent().getCoveredText() : "null";
            String relation = (dep.getDependencyType() != null) ? dep.getDependencyType() : "unbekannt";
            dependencies.add("Relation: " + relation + " - " + governor + " -> " + dependent);
        }
        return dependencies;
    }

    /**
     * Extrahiert Named Entities (NER) aus dem JCas.
     * Hier wird davon ausgegangen, dass der Typ NamedEntity im Package
     * de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity definiert ist.
     *
     * @param jcas Das zu analysierende CAS.
     * @return Eine Liste, in der jede Named Entity inklusive ihres Typs aufgeführt ist.
     */
    public List<String> extractNamedEntities(JCas jcas) {
        List<String> entities = new ArrayList<>();
        for (NamedEntity ne : JCasUtil.select(jcas, NamedEntity.class)) {
            String neText = ne.getCoveredText();
            String neType = (ne.getValue() != null) ? ne.getValue() : "unbekannt";
            entities.add(neText + " (" + neType + ")");
        }
        return entities;
    }

    /**
     * Baut ein strukturiertes Analysis-Dokument, das alle extrahierten Ergebnisse enthält.
     * Dabei werden:
     * - Die Sätze (inklusive Sentiment-Informationen),
     * - Tokens,
     * - POS-Tags,
     * - Dependencies und
     * - Named Entities (sowohl als Liste als auch als Zählung je NER-Typ)
     * als Unterdokument abgelegt.
     *
     * @param jcas Das analysierte CAS.
     * @return Ein Document, das die gesamte Analyse strukturiert enthält.
     */
    public Document buildAnalysisDocument(JCas jcas) {
        // Listen der extrahierten Ergebnisse
        List<String> sentenceResults = extractAnalysisResults(jcas);
        List<String> tokenResults = extractTokens(jcas);
        List<String> posResults = extractPOSTags(jcas);
        List<String> dependencyResults = extractDependencies(jcas);
        List<String> neResults = extractNamedEntities(jcas);

        // Zähle die Named Entities (z. B. "PER", "LOC", "ORG", "MISC") – basierend auf dem Format "Text (TYPE)"
        Map<String, Integer> neCounts = new HashMap<>();
        for (String ne : neResults) {
            int openParen = ne.lastIndexOf('(');
            int closeParen = ne.lastIndexOf(')');
            if (openParen >= 0 && closeParen > openParen) {
                String type = ne.substring(openParen + 1, closeParen).trim();
                neCounts.put(type, neCounts.getOrDefault(type, 0) + 1);
            }
        }

        // Überführe das Map<String,Integer> in ein Map<String, Object>
        Map<String, Object> neCountsObj = neCounts.entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> (Object) e.getValue()
                ));

        // Erstelle das strukturierte Analysis-Dokument
        Document analysisDoc = new Document();
        analysisDoc.append("sentences", sentenceResults)
                .append("tokens", tokenResults)
                .append("posTags", posResults)
                .append("dependencies", dependencyResults)
                .append("namedEntitiesList", neResults)
                .append("namedEntitiesCounts", new Document(neCountsObj));
        return analysisDoc;
    }


}



