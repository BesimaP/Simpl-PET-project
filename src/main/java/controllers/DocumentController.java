package controllers;

import enums.ServiceResult;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import io.javalin.http.UploadedFile;
import persistence.ConnectionPool;
import services.DocumentService;
import entities.Document;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

// Koordinatoren for dokumenter.html (US11). Læser formularen, kalder DocumentService og sender brugeren videre.
public class DocumentController {
    private DocumentService documentService;

    public DocumentController(ConnectionPool connectionPool) {
        this.documentService = new DocumentService(connectionPool);
    }

    public void setRoutes(JavalinConfig config) {
        config.routes.get("/dokumenter", ctx -> showDocuments(ctx));
        config.routes.post("/dokumenter", ctx -> uploadDocument(ctx));
        config.routes.get("/dokumenter/{id}", ctx -> openDocument(ctx));
        config.routes.post("/dokumenter/slet", ctx -> deleteDocument(ctx));   // "Slet" på ét dokument (id i et skjult felt)   // "Åbn" på listen – {id} = path-parameter
    }

    // POST /dokumenter/slet – slet ét dokument (rækken + filen). Service tjekker, at det er patientens eget
    private void deleteDocument(Context ctx) {
        Integer patientId = ctx.sessionAttribute("patientId");
        if (patientId == null) {
            ctx.redirect("/login");
            return;
        }
        int documentId;
        try {
            documentId = Integer.parseInt(ctx.formParam("id"));
        } catch (NumberFormatException e) {
            ctx.redirect("/dokumenter?fejl=ukendt");
            return;
        }
        ServiceResult result = documentService.deleteDocument(patientId, documentId);
        ctx.redirect(result == ServiceResult.OK ? "/dokumenter?gemt=slettet" : "/dokumenter?fejl=ukendt");
    }

    // GET /dokumenter/{id} – send selve filen tilbage til browseren (PDF vises, billeder vises)
    private void openDocument(Context ctx) {
        Integer patientId = ctx.sessionAttribute("patientId");
        if (patientId == null) {
            ctx.redirect("/login");
            return;
        }

        // 1. id fra URL'en: /dokumenter/7 -> 7. Ikke et tal -> 404
        int documentId;
        try {
            documentId = Integer.parseInt(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            ctx.status(404);
            return;
        }

        // 2. kun patientens egne dokumenter – ellers null
        Document document = documentService.findDocument(patientId, documentId);
        if (document == null) {
            ctx.status(404);
            return;
        }

        // 3. læs filen fra disken og send den. contentType fortæller browseren, om det er PDF eller billede
        try {
            Path path = Path.of(document.getFilePath());
            ctx.contentType(Files.probeContentType(path));
            ctx.result(Files.readAllBytes(path));
        } catch (IOException e) {
            ctx.status(404);   // rækken findes i databasen, men filen er væk
        }
    }

    // GET /dokumenter – hent rundens dokumenter og fyld skabelonen
    private void showDocuments(Context ctx) {
        Integer patientId = ctx.sessionAttribute("patientId");
        if (patientId == null) {
            ctx.redirect("/login");
            return;
        }
        ctx.attribute("documents", documentService.getDocuments(patientId));  // requestscope -> ${documents}
        // besked fra sidste POST (?gemt= / ?fejl= i URL'en) -> request scope -> fragmentet besked.html
        ctx.attribute("gemt", ctx.queryParam("gemt"));
        ctx.attribute("fejl", ctx.queryParam("fejl"));
        ctx.render("dokumenter");
    }

    // POST /dokumenter – når brugeren trykker "Upload". Filen læses med ctx.uploadedFile("file")
    private void uploadDocument(Context ctx) {
        Integer patientId = ctx.sessionAttribute("patientId");
        if (patientId == null) {
            ctx.redirect("/login");
            return;
        }

        String title = ctx.formParam("title");
        String type = ctx.formParam("documentType");
        UploadedFile file = ctx.uploadedFile("file");  // null, hvis der ikke blev valgt en fil

        if (file == null) {
            ctx.redirect("/dokumenter?fejl=felter");
            return;
        }

        ServiceResult result = documentService.uploadDocument(patientId, title, type, file.filename(), file.content(), file.size());

        switch (result) {
            case OK -> ctx.redirect("/dokumenter?gemt=1");
            case INVALID_INPUT -> ctx.redirect("/dokumenter?fejl=felter");
            default -> ctx.redirect("/dokumenter?fejl=ukendt");
        }
    }
}