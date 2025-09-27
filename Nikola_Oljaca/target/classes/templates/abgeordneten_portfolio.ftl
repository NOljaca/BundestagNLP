<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Abgeordneten-Portfolio</title>

    <!-- d3.js Bibliothek einbinden -->
    <script src="https://d3js.org/d3.v7.min.js"></script>
    <style>
        /*
         * Grundlegendes Layout:
         * - Der Body ist in zwei Spalten aufgeteilt:
         *   links der Hauptinhalt (main-content),
         *   rechts die History-Box.
         */
        body {
            display: flex;
            flex-direction: row;
            font-family: Arial, sans-serif;
        }
        .main-content {
            width: 70%;
            padding: 20px;
        }
        .history-box {
            width: 25%;
            margin-left: 5%;
            padding: 10px;
            border: 1px solid #ccc;
            background-color: #f9f9f9;
            max-height: 90vh;
            overflow-y: auto;
        }
        .comment {
            color: red;
            font-style: italic;
            margin-bottom: 10px;
        }
        .speech-title {
            /*
             * Stile für die Überschriften der Reden:
             * - Zeilenumbrüche und Wortumbrüche bei sehr langen Titeln ermöglichen,
             * - Verhindert Überlappungen mit der History-Box.
             */
            white-space: normal;
            overflow-wrap: break-word;
            word-wrap: break-word;
            max-width: calc(100% - 30%);
            display: inline-block;
        }
        /* Zusätzliche Stile für die Diagramme */
        .chart {
            margin: 20px 0;
        }
        .bar-chart, .pie-chart {
            width: 100%;
            height: 400px;
        }
        .bubble-chart {
            width: 100%;
            height: 400px;
        }
        /* Tooltip-Stil */
        .tooltip {
            position: absolute;
            text-align: center;
            padding: 6px;
            font: 12px sans-serif;
            background: lightsteelblue;
            border: 0px;
            border-radius: 8px;
            pointer-events: none;
        }
    </style>
</head>
<body>
<!--
    Hauptinhalt der Seite: Informationen zum Abgeordneten,
    Bild, Biografie, persönliche Daten, Mitgliedschaften und Reden.
-->
<div class="main-content">
    <!-- Link zurück zur Einstiegsseite -->
    <p><a href="/einstiegsseite">Zurück zur Startseite</a></p>

    <!-- Titel und Name des Abgeordneten -->
    <h1>${title} ${firstName} ${name} (${party! "Parteilos"})</h1>

    <!-- Anzeige des Bildes, falls vorhanden -->
    <#-- imageUrl wird geprüft. Falls nicht vorhanden, wird ein Platzhalter angezeigt. -->
    <#if imageUrl?? && imageUrl != "">
        <img src="${imageUrl}" alt="${firstName} ${name}" style="max-width:300px; border-radius: 5px;">
    <#else>
        <p>Kein Bild vorhanden</p>
    </#if>

    <!-- Formular zum Aktualisieren des Bild-URLs -->
    <form action="/update-image" method="post" style="margin-top: 10px;">
        <input type="hidden" name="speakerId" value="${id}">
        <label>Neues Bild-URL: </label>
        <input type="text" name="imageUrl" placeholder="Bild-URL eingeben" required style="padding: 5px; margin-top: 5px;">
        <button type="submit" style="padding: 5px; margin-top: 5px;">Speichern</button>
    </form>

    <!-- Biografie des Abgeordneten -->
    <h2>Biografie</h2>
    <#if vita??>
        <p>${vita}</p>
    <#else>
        <p>Keine Biografie vorhanden</p>
    </#if>

    <!-- Persönliche Informationen mit Default-Werten bei fehlenden Daten -->
    <h2>Persönliche Informationen</h2>
    <ul>
        <li><strong>Geburtsdatum:</strong> <#if birthDate??>${birthDate?date?string("dd.MM.yyyy")}<#else>N/A</#if></li>
        <li><strong>Geburtsort:</strong> ${birthPlace!"Unbekannt"}</li>
        <li><strong>Sterbedatum:</strong> <#if deathDate??>${deathDate?date?string("dd.MM.yyyy")}<#else>N/A</#if></li>
        <li><strong>Geschlecht:</strong> ${gender!"Unbekannt"}</li>
        <li><strong>Beruf:</strong> ${occupation!"Unbekannt"}</li>
        <li><strong>Familienstand:</strong> ${familyStatus!"Unbekannt"}</li>
        <li><strong>Religion:</strong> ${religion!"Unbekannt"}</li>
        <li><strong>Akademischer Titel:</strong> ${academicTitle!"Unbekannt"}</li>
    </ul>

    <!-- Mitgliedschaften des Abgeordneten, falls vorhanden -->
    <#if memberships??>
        <h2>Mitgliedschaften</h2>
        <ul>
            <#list memberships as m>
                <li>
                    ${m.role!''} - ${m.label!''}
                </li>
            </#list>
        </ul>
    </#if>

    <!--
        Liste aller Reden des Abgeordneten:
        Jede Rede wird mit Titel, Agenda und Datum dargestellt.
        Unterhalb der Rede-Titel werden die einzelnen Inhalte aufgeführt:
        - Text: Normale Auflistung als <li>
        - Kommentar: Mit Zuordnungs-Formular, um den Kommentar einer Fraktion oder Person zuzuweisen.
    -->
    <!-- Liste aller Reden des Abgeordneten -->
    <#list speeches as speech>
        <div class="speech">
            <h3 class="speech-title">${speech.title?replace("_", " ")} - ${speech.agenda?replace("_", " ")}</h3>
            <p><strong>Datum:</strong> ${speech.date?number_to_date?string("dd.MM.yyyy")}</p>

            <!-- Video einfügen -->
            <#if speech.id == "ID2019200100">
            <video controls style="width: 100%; max-width: 1080px; height: 720px; border: 2px solid black;">
                <source src="http://localhost:7080/video/ID2019200100__2024_10_11_TOP%2026_7616626_h264_1920_1080_5000kb_baseline_de_5000.mp4" type="video/mp4">
                Ihr Browser unterstützt das Video-Tag nicht.
            </video>

            <#elseif speech.id == "ID2019200200">
            <video controls style="width: 100%; max-width: 1080px; height: 720px; border: 2px solid black;">
                <source src="http://localhost:7080/video/ID2019200200__2024_10_11_TOP%2026_7616627_h264_1920_1080_5000kb_baseline_de_5000.mp4" type="video/mp4">
                Ihr Browser unterstützt das Video-Tag nicht.
            </video>

            <#else>
            <p style="color: red;">Keine passende Rede-ID gefunden.</p>
            </#if>

            <!-- NLP-Statistiken anzeigen -->
            <div class="nlp-stats">
                <!-- POS-Typen als Balkendiagramm -->
                <div id="pos-chart-${speech.id}" class="bar-chart chart"></div>

                <!-- Named Entities als Bubble-Chart -->
                <div id="ne-bubble-chart-${speech.id}" class="bubble-chart chart"></div>
            </div>

            <ul>
                <#list speech.contentList as content>
                    <#if content.type == "text">
                        <!-- Hintergrundfarbe basierend auf dem Sentiment-Wert -->
                        <li style="background-color: ${content.sentimentColor! '#FFFFFF'}; padding: 5px; border-radius: 3px; margin-bottom: 5px;">
                            ${content.text}
                        </li>
                    <#elseif content.type == "comment">
                        <!-- Kommentare werden rot und kursiv dargestellt. Enthält ein Formular zum Aktualisieren der Zuordnung (assignedTo). -->
                        <li class="comment">
                            <form action="/update-comment" method="post">
                                <p>${content.text}</p>
                                <label>Zuordnung:</label>
                                <select name="assignedTo" required>
                                    <option value="Unbekannt" <#if content.assignedTo == "Unbekannt">selected</#if>>Unbekannt</option>
                                    <!-- Fraktionen -->
                                    <optgroup label="Fraktionen">
                                        <#list factions as faction>
                                            <option value="${faction}" <#if content.assignedTo == faction>selected</#if>>${faction}</option>
                                        </#list>
                                    </optgroup>
                                    <!-- Abgeordnete -->
                                    <optgroup label="Abgeordnete">
                                        <#list allSpeakers as speakerName>
                                            <option value="${speakerName}" <#if content.assignedTo == speakerName>selected</#if>>${speakerName}</option>
                                        </#list>
                                    </optgroup>
                                </select>

                                <!-- Hidden-Feld zur Identifizierung des Kommentars -->
                                <input type="hidden" name="contentId" value="${content.id!''}">
                                <button type="submit" style="padding: 5px; margin-top: 5px;">Speichern</button>
                            </form>
                        </li>
                    </#if>
                </#list>
            </ul>
        </div>

        <!-- Skript zur Visualisierung der NLP-Statistiken -->
        <script>
            document.addEventListener("DOMContentLoaded", function() {
                // POS-Typen als Balkendiagramm
                (function() {
                    const data = [
                        <#list speech.nlpStats.posCounts?keys as posType>
                        { "type": "${posType}", "count": ${speech.nlpStats.posCounts[posType]!0} }<#if posType_has_next>,</#if>
                        </#list>
                    ];
                    // Berechnung der maximalen Breite basierend auf der Anzahl der Kategorien
                    const barWidth = 50;
                    const totalBars = data.length;
                    const width = Math.max(600, totalBars * barWidth);
                    const height = 400;
                    const margin = { top: 40, right: 20, bottom: 70, left: 60 };

                    const svg = d3.select("#pos-chart-${speech.id}")
                        .append("svg")
                        .attr("width", width)
                        .attr("height", height)
                        .append("g")
                        .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

                    const x = d3.scaleBand()
                        .range([0, width - margin.left - margin.right])
                        .domain(data.map(function(d) { return d.type; }))
                        .padding(0.2);

                    svg.append("g")
                        .attr("transform", "translate(0," + (height - margin.top - margin.bottom) + ")")
                        .call(d3.axisBottom(x))
                        .selectAll("text")
                        .attr("transform", "translate(-10,0)rotate(-45)")
                        .style("text-anchor", "end");

                    const y = d3.scaleLinear()
                        .domain([0, d3.max(data, function(d) { return d.count; }) * 1.1])
                        .range([height - margin.top - margin.bottom, 0]);

                    svg.append("g")
                        .call(d3.axisLeft(y));

                    svg.selectAll(".mybar")
                        .data(data)
                        .enter()
                        .append("rect")
                        .attr("x", function(d) { return x(d.type); })
                        .attr("y", function(d) { return y(d.count); })
                        .attr("width", x.bandwidth())
                        .attr("height", function(d) { return height - margin.top - margin.bottom - y(d.count); })
                        .attr("fill", "#69b3a2");

                    // Titel hinzufügen
                    svg.append("text")
                        .attr("x", (width - margin.left - margin.right) / 2)
                        .attr("y", -20)
                        .attr("text-anchor", "middle")
                        .style("font-size", "16px")
                        .style("text-decoration", "underline")
                        .text("POS-Typen Häufigkeit");
                })();

                // Named Entities als Bubble-Chart mit prozentualem Anteil
                (function() {
                    const data = [
                        <#list speech.nlpStats.namedEntitiesCounts?keys as neType>
                        { type: "${neType}", count: ${speech.nlpStats.namedEntitiesCounts[neType]!0} }<#if neType_has_next>,</#if>
                        </#list>
                    ];

                    // Berechnung der Gesamtzahl der Named Entities für Prozentuale Anteile
                    const total = d3.sum(data, function(d) { return d.count; });

                    // Hinzufügen eines Prozentanteils zu jedem Datenelement
                    data.forEach(function(d) {
                        d.percentage = ((d.count / total) * 100).toFixed(2) + "%";
                    });

                    const width = 600;
                    const height = 400;

                    const svg = d3.select("#ne-bubble-chart-${speech.id}")
                        .append("svg")
                        .attr("width", width)
                        .attr("height", height)
                        .append("g")
                        .attr("transform", "translate(" + (width / 2) + "," + (height / 2) + ")");

                    const maxCount = d3.max(data, function(d) { return d.count; });
                    const radiusScale = d3.scaleSqrt()
                        .domain([0, maxCount])
                        .range([0, 100]);

                    const colorScale = d3.scaleOrdinal(d3.schemeCategory10)
                        .domain(data.map(function(d) { return d.type; }));

                    const pack = d3.pack()
                        .size([width, height])
                        .padding(5);

                    const root = d3.hierarchy({ children: data })
                        .sum(function(d) { return d.count; });

                    const nodes = pack(root).leaves();

                    // Tooltip erstellen
                    const tooltip = d3.select("body")
                        .append("div")
                        .attr("class", "tooltip")
                        .style("opacity", 0);

                    svg.selectAll("circle")
                        .data(nodes)
                        .enter()
                        .append("circle")
                        .attr("cx", function(d) { return d.x - width / 2; })
                        .attr("cy", function(d) { return d.y - height / 2; })
                        .attr("r", function(d) { return radiusScale(d.data.count); })
                        .attr("fill", function(d) { return colorScale(d.data.type); })
                        .attr("stroke", "black")
                        .attr("stroke-width", 1)
                        .attr("opacity", 0.7)
                        .on("mouseover", function(event, d) {
                            tooltip.transition()
                                .duration(200)
                                .style("opacity", .9);
                            tooltip.html(d.data.type + ": " + d.data.percentage)
                                .style("left", (event.pageX) + "px")
                                .style("top", (event.pageY - 28) + "px");
                        })
                        .on("mouseout", function(d) {
                            tooltip.transition()
                                .duration(500)
                                .style("opacity", 0);
                        });

                    // Labels hinzufügen
                    svg.selectAll("text")
                        .data(nodes)
                        .enter()
                        .append("text")
                        .attr("x", function(d) { return d.x - width / 2; })
                        .attr("y", function(d) { return d.y - height / 2; })
                        .attr("text-anchor", "middle")
                        .attr("dy", ".3em")
                        .text(function(d) { return d.data.type; })
                        .style("font-size", "12px")
                        .style("fill", "black");

                    // Prozentuale Anteile als Legende hinzufügen
                    const legend = svg.append("g")
                        .attr("transform", "translate(" + (-width / 2 + 20) + "," + (-height / 2 + 20) + ")");

                    data.forEach(function(d, i) {
                        legend.append("rect")
                            .attr("x", 0)
                            .attr("y", i * 20)
                            .attr("width", 15)
                            .attr("height", 15)
                            .attr("fill", colorScale(d.type));

                        legend.append("text")
                            .attr("x", 20)
                            .attr("y", i * 20 + 12)
                            .text(d.type + " (" + d.percentage + ")");
                    });

                    // Titel hinzufügen
                    svg.append("text")
                        .attr("x", 0)
                        .attr("y", -height / 2 + 20)
                        .attr("text-anchor", "middle")
                        .style("font-size", "16px")
                        .style("text-decoration", "underline")
                        .text("Named Entities Häufigkeit");
                })();
            });
        </script>
    </#list>
</div>

<!--
    Rechte Spalte: Historie-Box.
    Zeigt die Änderungshistorie von Kommentaren und Bild-URLs an,
    sortiert nach Zeitpunkt.
-->
<div class="history-box">
    <h2>Änderungshistorie</h2>
    <#if history?? && history?size gt 0>
        <ul>
            <#list history as entry>
                <li>
                    <p><strong>Geändert am:</strong> ${entry.timestamp?number_to_date?string("dd.MM.yyyy HH:mm:ss")}</p>
                    <#if entry.type == "comment">
                        <p><strong>Typ:</strong> Kommentarzuordnung</p>
                        <p><strong>Vorher:</strong> ${entry.oldValue!''}</p>
                        <p><strong>Nachher:</strong> ${entry.newValue!''}</p>
                    <#elseif entry.type == "image">
                        <p><strong>Typ:</strong> Bildänderung</p>
                        <p><strong>Vorher:</strong> ${entry.oldValue!''}</p>
                        <p><strong>Nachher:</strong> ${entry.newValue!''}</p>
                    </#if>
                </li>
            </#list>
        </ul>
    <#else>
        <p>Keine Änderungen vorhanden.</p>
    </#if>
</div>
</body>
</html>





