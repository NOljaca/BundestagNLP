<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Abgeordnete nach Fraktion</title>
</head>
<body>
<!--
    Hauptüberschrift: Zeigt an, dass auf dieser Seite Abgeordnete nach Fraktionen gruppiert dargestellt werden.
-->
<h1>Abgeordnete nach Fraktion</h1>

<!--
    Ausgabe der Gesamtanzahl der erfassten Abgeordneten.
    Die Variable "totalSpeakers" wird vom Backend gesetzt und hier ausgegeben.
-->
<p>Insgesamt erfasste Abgeordnete: ${totalSpeakers}</p>

<!-- Suchformular:
     Ermöglicht das Suchen nach Abgeordneten anhand von Namensfragmenten.
     Wird mit GET auf den Pfad "/search" geschickt.
-->
<form action="/search" method="get">
    <input type="text" name="q" placeholder="Nach Abgeordneten suchen">
    <button type="submit">Suchen</button>
</form>

<!--
    Prüfung, ob eine Suchanfrage ausgeführt wurde ("query" vorhanden).
    Wenn query?? (existiert query?), dann werden Suchergebnisse angezeigt,
    sonst die Standard-Übersicht nach Fraktionen.
-->
<#if query??>
    <!-- Überschrift für den Suchmodus -->
    <h2>Suchergebnisse für: "${query}"</h2>
    <!-- Überprüfung, ob es Suchergebnisse gibt -->
    <#if searchResults?size == 0>
        <!-- Keine Treffer -->
        <p>Keine Abgeordneten gefunden.</p>
    <#else>
        <!--
            Auflistung der Suchergebnisse:
            Jeder Treffer ist ein Link auf die Detailansicht des jeweiligen Abgeordneten.
        -->
        <ul>
            <#list searchResults as result>
                <li>
                    <a href="/abgeordneten_portfolio/${result.id}">
                        ${result.firstName} ${result.name} (${result.party}) - Redenanzahl: ${result.speechCount}
                    </a>
                </li>
            </#list>
        </ul>
    </#if>

    <!--
        Falls keine Suche erfolgt ist (query nicht vorhanden),
        werden die Abgeordneten nach Fraktionen gruppiert ausgegeben.
    -->
<#else>
    <h2>Nach Fraktion gruppiert</h2>
    <!--
        "parties" ist eine Liste von Fraktionen, jede mit einer "partyName" und einer Liste von "speakers".
        Jeder Sprecher ist ein Map-Objekt mit Feldern wie id, firstName, name, party und speechCount.
    -->
    <#list parties as p>
        <!-- Ausgabetitel pro Fraktion -->
        <h3>${p.partyName}</h3>
        <ul>
            <#list p.speakers as s>
                <!--
                    Link auf die Detailseite des Abgeordneten, mit Anzeige von Name, Partei und Redenanzahl.
                -->
                <li>
                    <a href="/abgeordneten_portfolio/${s.id}">
                        ${s.firstName} ${s.name} (${s.party}) - Redenanzahl: ${s.speechCount}
                    </a>
                </li>
            </#list>
        </ul>
    </#list>
</#if>

</body>
</html>

