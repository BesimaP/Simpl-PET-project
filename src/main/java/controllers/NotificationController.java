package controllers;

import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import persistence.ConnectionPool;
import services.NotificationService;

// Koordinatoren for notifikationer (US12). Viser patientens påmindelser og markerer dem som læst.
public class NotificationController {
    private NotificationService notificationService; // "den der bestemmer" for påmindelser

    public NotificationController(ConnectionPool connectionPool) {
        this.notificationService = new NotificationService(connectionPool);
    }

    public void setRoutes(JavalinConfig config) {
        config.routes.get("/notifikationer", ctx -> showNotifications(ctx));   // vis listen (Thymeleaf)
        config.routes.post("/notifikationer/laest", ctx -> markRead(ctx));     // knappen på én notifikation
        config.routes.post("/notifikationer/laest-alle", ctx -> markAllRead(ctx));
    }

    // GET /notifikationer – hent listen og fyld skabelonen
    private void showNotifications(Context ctx) {
        Integer patientId = ctx.sessionAttribute("patientId");
        if (patientId == null) {
            ctx.redirect("/login");
            return;
        }
        NotificationService service = notificationService;
        ctx.attribute("notifications", service.getNotifications(patientId));   // requestscope -> ${notifications}
        ctx.attribute("unread", service.countUnread(patientId));               // requestscope -> ${unread} (knappen vises kun, hvis > 0)
        ctx.render("notifikationer");
    }

    // POST /notifikationer/laest-alle – "Markér alle som læst" øverst på siden
    private void markAllRead(Context ctx) {
        Integer patientId = ctx.sessionAttribute("patientId");
        if (patientId == null) {
            ctx.redirect("/login");
            return;
        }
        notificationService.markAllRead(patientId);
        // tilbage til den side, knappen blev trykket på (pop-op på dashboard eller /notifikationer). Referer = "hvor kom du fra"
        String from = ctx.header("Referer");
        ctx.redirect(from != null && from.contains("/dashboard") ? "/dashboard" : "/notifikationer");
    }

    // POST /notifikationer/laest – "Markér som læst" på én notifikation (id i et skjult felt)
    private void markRead(Context ctx) {
        Integer patientId = ctx.sessionAttribute("patientId");
        if (patientId == null) {
            ctx.redirect("/login");
            return;
        }
        // try/catch: et ugyldigt id giver bare listen igen i stedet for en 500-fejl
        int notificationId;
        try {
            notificationId = Integer.parseInt(ctx.formParam("id"));
        } catch (NumberFormatException e) {
            ctx.redirect("/notifikationer");
            return;
        }
        notificationService.markRead(patientId, notificationId);   // service tjekker, at den er patientens egen
        ctx.redirect("/notifikationer");
    }
}