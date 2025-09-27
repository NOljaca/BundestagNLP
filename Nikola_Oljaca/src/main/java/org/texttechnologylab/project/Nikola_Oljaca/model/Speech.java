package org.texttechnologylab.project.Nikola_Oljaca.model;

/**
 * Reine Datenklasse (Model) für eine Rede.
 * Hält ID, Speaker und den (ggf. zusammengefügten) Text.
 * Enthält KEINE UIMA-Logik!
 */
public class Speech {

    private String id;
    private String speaker;
    private String text;

    public Speech() {
        // no-args
    }

    public Speech(String id, String speaker, String text) {
        this.id = id;
        this.speaker = speaker;
        this.text = text;
    }

    // Getter/Setter

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }

    public String getSpeaker() {
        return speaker;
    }
    public void setSpeaker(String speaker) {
        this.speaker = speaker;
    }

    public String getText() {
        return text;
    }
    public void setText(String text) {
        this.text = text;
    }
}




