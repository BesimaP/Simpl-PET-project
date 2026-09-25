package controllers;

import enums.ServiceResult;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import io.javalin.http.UploadedFile;
import services.DocumentService;

// Koordinatoren for dokumenter.html (US11). Læser formularen, kalder DocumentService og sender brugeren videre.
public class DocumentController {

    public static void setRoutes(JavalinConfig config) {
        config.routes.get("/dokumenter", ctx -> showDocuments(ctx));
        config.routes.post("/dokumenter", ctx -> uploadDocument(ctx));
    }

    // GET /dokumenter – hent rundens dokumenter og fyld skabelonen
    private static void showDocuments(Context ctx) {
        Integer patientId = ctx.sessionAttribute("patientId");
        if (patientId == null) {
            ctx.redirect("/login");
            return;
        }
        ctx.attribute("documents", new DocumentService().getDocuments(patientId));  // requestscope -> ${documents}
        ctx.render("dokumenter");
    }

    // POST /dokumenter – når brugeren trykker "Upload". Filen læses med ctx.uploadedFile("file")
    private static void uploadDocument(Context ctx) {
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

        ServiceResult result = new DocumentService()
                .uploadDocument(patientId, title, type, file.filename(), file.content(), file.size());

        switch (result) {
            case OK -> ctx.redirect("/dokumenter");
            case INVALID_INPUT -> ctx.redirect("/dokumenter?fejl=felter");
            case NO_ACTIVE_JOURNEY -> ctx.redirect("/dokumenter?fejl=ingen-forloeb");
            case NO_ACTIVE_ROUND -> ctx.redirect("/dokumenter?fejl=ingen-runde");
            default -> ctx.redirect("/dokumenter?fejl=ukendt");
        }
    }
}