package controllers;

import enums.ServiceResult;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import persistence.ConnectionPool;
import services.DiaryService;

// Controller for dagbog.html (US4). Læser formularen, kalder DiaryService og sender brugeren videre.
// SQL og mappers ligger i service- og persistence-laget.
public class DiaryController {
    private DiaryService diaryService;

    public DiaryController(ConnectionPool connectionPool) {
        this.diaryService = new DiaryService(connectionPool);
    }

    // bliver kaldt én gang fra RouteConfig
    public void setRoutes(JavalinConfig config) {
        config.routes.post("/dagbog", ctx -> saveEntry(ctx));
        config.routes.get("/dagbog", ctx -> showEntries(ctx));
        config.routes.post("/dagbog/slet", ctx -> deleteEntry(ctx));   // "Slet" på én note
    }

    public void showEntries(Context ctx) {
        Integer patientId = ctx.sessionAttribute("patientId");
        if (patientId == null) {
            ctx.redirect("/login");
            return;
        }
        ctx.attribute("entries", diaryService.getEntries(patientId));
        // ?gemt= / ?fejl= fra sidste POST vises af fragmentet besked.html
        ctx.attribute("gemt", ctx.queryParam("gemt"));
        ctx.attribute("fejl", ctx.queryParam("fejl"));
        ctx.render("dagbog");

    }

    // POST /dagbog – når brugeren trykker "Gem note"
    public void saveEntry(Context ctx) {
        String date = ctx.formParam("date");
        String title = ctx.formParam("title");
        String note = ctx.formParam("note");

        Integer patientId = ctx.sessionAttribute("patientId");
        if (patientId == null) {
            ctx.redirect("/login");
            return;
        }

        ServiceResult result = diaryService.saveEntry(patientId, date, title, note);

        // noter hænger på patienten, så der kræves hverken forløb eller runde
        switch (result) {
            case OK -> ctx.redirect("/dagbog?gemt=1");
            case INVALID_INPUT -> ctx.redirect("/dagbog?fejl=felter"); // et felt var tomt
            default -> ctx.redirect("/dagbog?fejl=ukendt");
        }
    }

    // POST /dagbog/slet – id kommer fra et skjult felt. Service tjekker, at noten er patientens egen
    private void deleteEntry(Context ctx) {
        Integer patientId = ctx.sessionAttribute("patientId");
        if (patientId == null) {
            ctx.redirect("/login");
            return;
        }
        int entryId;
        try {
            entryId = Integer.parseInt(ctx.formParam("id"));
        } catch (NumberFormatException e) {
            ctx.redirect("/dagbog?fejl=ukendt");
            return;
        }
        ServiceResult result = diaryService.deleteEntry(patientId, entryId);
        ctx.redirect(result == ServiceResult.OK ? "/dagbog?gemt=slettet" : "/dagbog?fejl=ukendt");
    }
}
