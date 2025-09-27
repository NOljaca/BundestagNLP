package org.texttechnologylab.project.Nikola_Oljaca.rest;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import io.javalin.Javalin;
import io.javalin.http.Context;
import java.io.IOException;
import java.io.StringWriter;

public class JavalinConfig {

    public static Javalin createApp(int port) {
        Javalin app = Javalin.create(config -> {
            Configuration freemarkerConfig = new Configuration(Configuration.VERSION_2_3_32);
            freemarkerConfig.setClassForTemplateLoading(RESTHandler.class, "/templates");
            freemarkerConfig.setDefaultEncoding("UTF-8");

            config.fileRenderer((filePath, model, ctx) -> {
                try {
                    Template template = freemarkerConfig.getTemplate(filePath);
                    StringWriter writer = new StringWriter();
                    template.process(model, writer);
                    return writer.toString();
                } catch (IOException | TemplateException e) {
                    System.err.println("Fehler beim Laden des Templates: " + filePath);
                    throw new RuntimeException(e);
                }
            });
        }).start(port);

        System.out.println("Javalin wurde auf Port " + port + " gestartet.");
        System.out.println("Registriere Route für Video-Streaming: /video/{videoId}");

        app.get("/video/{videoId}", new Videostreaminghandler());

        return app;
    }
}




