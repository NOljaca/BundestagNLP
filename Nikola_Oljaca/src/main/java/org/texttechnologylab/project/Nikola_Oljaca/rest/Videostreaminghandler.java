package org.texttechnologylab.project.Nikola_Oljaca.rest;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class Videostreaminghandler implements Handler {
    private static final String VIDEO_FOLDER = "/home/nikola/Schreibtisch/uebung4/Nikola_Oljaca/src/main/resources/static/video/";

    @Override
    public void handle(Context ctx) throws Exception {
        String videoId = ctx.pathParam("videoId");
        String decodedVideoId = URLDecoder.decode(videoId, StandardCharsets.UTF_8);
        File videoFile = new File(VIDEO_FOLDER + decodedVideoId);

        if (!videoFile.exists()) {
            ctx.status(404).result("Video nicht gefunden: " + decodedVideoId);
            return;
        }

        long fileLength = videoFile.length();
        String range = ctx.header("Range");

        ctx.res().setContentType("video/mp4");
        ctx.res().setHeader("Accept-Ranges", "bytes");

        try (RandomAccessFile raf = new RandomAccessFile(videoFile, "r");
             OutputStream os = ctx.res().getOutputStream()) {

            if (range == null) {
                // Volle Datei streamen
                ctx.res().setHeader("Content-Length", String.valueOf(fileLength));
                byte[] buffer = new byte[1024 * 16];
                int bytesRead;
                while ((bytesRead = raf.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
            } else {
                // Range-Request verarbeiten
                String[] ranges = range.replace("bytes=", "").split("-");
                long start = Long.parseLong(ranges[0]);
                long end = ranges.length > 1 && !ranges[1].isEmpty() ? Long.parseLong(ranges[1]) : fileLength - 1;
                long contentLength = end - start + 1;

                ctx.status(206);  // 206 Partial Content
                ctx.res().setHeader("Content-Range", "bytes " + start + "-" + end + "/" + fileLength);
                ctx.res().setHeader("Content-Length", String.valueOf(contentLength));

                raf.seek(start);
                byte[] buffer = new byte[1024 * 16];
                long bytesRemaining = contentLength;

                while (bytesRemaining > 0) {
                    int bytesToRead = (int) Math.min(buffer.length, bytesRemaining);
                    int bytesRead = raf.read(buffer, 0, bytesToRead);
                    if (bytesRead == -1) break;
                    os.write(buffer, 0, bytesRead);
                    bytesRemaining -= bytesRead;
                }
            }
            os.flush();
        }
    }
}






