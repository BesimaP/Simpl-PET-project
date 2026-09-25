package controllers;

import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import services.NotificationService;

// Koordinatoren for notifikationer (US12). Viser patientens påmindelser og markerer dem som læst.
public class NotificationController {

    public static void setRoutes(JavalinConfig config) {
        config.routes.get("/notifikationer", ctx -> showNotifications(ctx));   // vis listen (Thymeleaf)
        config.routes.post("/notifikationer/laest", ctx -> markRead(ctx));     // knappen på én notifikation
    }

    // GET /notifikationer – hent listen og fyld skabelonen
    private static void showNotifications(Context ctx) {
        Integer patientId = ctx.sessionAttribute("patientId");
        if (patientId == null) {
            ctx.redirect("/login");
            return;
        }
        ctx.attribute("notifications", new NotificationService().getNotifications(patientId));   // requestscope -> ${notifications}
        ctx.render("notifikationer");
    }

    // POST /notifikationer/laest – markér én som læst (id i et skjult felt)
    private static void markRead(Context ctx) {
        Integer patientId = ctx.sessionAttribute("patientId");
        if (patientId == null) {
            ctx.redirect("/login");
            return;
        }
        new NotificationService().markRead(Integer.parseInt(ctx.formParam("id")));
        ctx.redirect("/notifikationer");
    }
}