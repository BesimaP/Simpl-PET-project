package controllers;

import enums.DocumentType;
import enums.ServiceResult;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import io.javalin.http.UploadedFile;
import persistence.ConnectionPool;
import services.DocumentService;
import entities.Document;

import java.io.IOException;

// Controller for dokumenter.html (US11). Læser formularen, kalder DocumentService og sender brugeren videre.
public class DocumentController {
    private DocumentService documentService;

    public DocumentController(ConnectionPool connectionPool) {
        this.documentService = new DocumentService(connectionPool);
    }

    public void setRoutes(JavalinConfig config) {
        config.routes.get("/dokumenter", ctx -> showDocuments(ctx));
        config.routes.post("/dokumenter", ctx -> uploadDocument(ctx));
        config.routes.get("/dokumenter/{id}", ctx -> openDocument(ctx));   // {id} er en path-parameter
        config.routes.post("/dokumenter/slet", ctx -> deleteDocument(ctx));
    }

    // POST /dokumenter/slet – sletter både rækken og filen. Service tjekker, at det er patientens eget
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

    // GET /dokumenter/{id} – sender selve filen, så browseren kan vise PDF'en eller billedet
    private void openDocument(Context ctx) {
        Integer patientId = ctx.sessionAttribute("patientId");
        if (patientId == null) {
            ctx.redirect("/login");
            return;
        }

        int documentId;
        try {
            documentId = Integer.parseInt(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            ctx.status(404);
            return;
        }

        // null, hvis dokumentet ikke findes eller tilhører en anden patient
        Document document = documentService.findDocument(patientId, documentId);
        if (document == null) {
            ctx.status(404);
            return;
        }

        // FileStorage læser filen fra disken. contentType fortæller browseren, om det er PDF eller billede
        try {
            ctx.contentType(documentService.contentType(document));
            ctx.result(documentService.readFile(document));
        } catch (IOException e) {
            ctx.status(404);   // rækken findes i databasen, men filen er væk
        }
    }

    // GET /dokumenter – alle patientens dokumenter
    private void showDocuments(Context ctx) {
        Integer patientId = ctx.sessionAttribute("patientId");
        if (patientId == null) {
            ctx.redirect("/login");
            return;
        }
        ctx.attribute("documents", documentService.getDocuments(patientId));
        ctx.attribute("documentTypes", DocumentType.values());   // dropdownen bygges ud fra enum'en
        // ?gemt= / ?fejl= fra sidste POST vises af fragmentet besked.html
        ctx.attribute("gemt", ctx.queryParam("gemt"));
        ctx.attribute("fejl", ctx.queryParam("fejl"));
        ctx.render("dokumenter");
    }

    // POST /dokumenter – når brugeren trykker "Upload"
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
            ctx.redirect("/dokumenter?fejl=fil");
            return;
        }

        ServiceResult result = documentService.uploadDocument(patientId, title, type, file.filename(), file.content(), file.size());

        switch (result) {
            case OK -> ctx.redirect("/dokumenter?gemt=1");
            case INVALID_INPUT -> ctx.redirect("/dokumenter?fejl=fil");
            default -> ctx.redirect("/dokumenter?fejl=ukendt");
        }
    }
}