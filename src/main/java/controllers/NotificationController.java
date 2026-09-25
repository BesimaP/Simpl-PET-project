package controllers;

import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import services.NotificationService;

// Koordinatoren for notifikationer (US12). Viser patientens påmindelser og markerer dem som læst.
public class NotificationController {

    public static void setRoutes(JavalinConfig config) {
        config.routes.get("/notifikationer", ctx -> showNotifications(ctx));   // vis listen (Thymeleaf)
        config.routes.post("/notifikationer/laest", ctx -> markRead(ctx));     // knappen på én notifikation
        config.routes.post("/notifikationer/laest-alle", ctx -> markAllRead(ctx));
    }

    // GET /notifikationer – hent listen og fyld skabelonen
    private static void showNotifications(Context ctx) {
        Integer patientId = ctx.sessionAttribute("patientId");
        if (patientId == null) {
            ctx.redirect("/login");
            return;
        }
        NotificationService service = new NotificationService();
        ctx.attribute("notifications", service.getNotifications(patientId));   // requestscope -> ${notifications}
        ctx.attribute("unread", service.countUnread(patientId));               // requestscope -> ${unread} (knappen vises kun, hvis > 0)
        ctx.render("notifikationer");
    }

    // POST /notifikationer/laest-alle – "Markér alle som læst" øverst på siden
    private static void markAllRead(Context ctx) {
        Integer patientId = ctx.sessionAttribute("patientId");
        if (patientId == null) {
            ctx.redirect("/login");
            return;
        }
        new NotificationService().markAllRead(patientId);
        // tilbage til den side, knappen blev trykket på (pop-op på dashboard eller /notifikationer). Referer = "hvor kom du fra"
        String from = ctx.header("Referer");
        ctx.redirect(from != null && from.contains("/dashboard") ? "/dashboard" : "/notifikationer");
    }

    // POST /notifikationer/laest – markér én som læst (id i et skjult felt). Bruges ikke i UI'et lige nu, men ruten findes
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