package controllers;

import enums.ServiceResult;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import persistence.ConnectionPool;
import services.NotificationService;

// Controller for notifikationer (US12). Viser patientens påmindelser og markerer dem som læst.
public class NotificationController {
    private NotificationService notificationService;

    public NotificationController(ConnectionPool connectionPool) {
        this.notificationService = new NotificationService(connectionPool);
    }

    public void setRoutes(JavalinConfig config) {
        config.routes.get("/notifikationer", ctx -> showNotifications(ctx));
        config.routes.post("/notifikationer/laest", ctx -> markRead(ctx));
        config.routes.post("/notifikationer/laest-alle", ctx -> markAllRead(ctx));
    }

    // GET /notifikationer
    private void showNotifications(Context ctx) {
        Integer patientId = ctx.sessionAttribute("patientId");
        if (patientId == null) {
            ctx.redirect("/login");
            return;
        }
        NotificationService service = notificationService;
        ctx.attribute("notifications", service.getNotifications(patientId));
        ctx.attribute("unread", service.countUnread(patientId));               // "Markér alle" vises kun, hvis der er ulæste
        // ?fejl= fra sidste POST vises af fragmentet besked.html
        ctx.attribute("fejl", ctx.queryParam("fejl"));
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
        // Referer fortæller, om knappen blev trykket i pop-op'en på dashboard eller på /notifikationer
        String from = ctx.header("Referer");
        ctx.redirect(from != null && from.contains("/dashboard") ? "/dashboard" : "/notifikationer");
    }

    // POST /notifikationer/laest – id kommer fra et skjult felt
    private void markRead(Context ctx) {
        Integer patientId = ctx.sessionAttribute("patientId");
        if (patientId == null) {
            ctx.redirect("/login");
            return;
        }
        // et ugyldigt id giver bare listen igen i stedet for en 500-fejl
        int notificationId;
        try {
            notificationId = Integer.parseInt(ctx.formParam("id"));
        } catch (NumberFormatException e) {
            ctx.redirect("/notifikationer");
            return;
        }
        ServiceResult result = notificationService.markRead(patientId, notificationId);   // service tjekker, at den er patientens egen
        ctx.redirect(result == ServiceResult.OK ? "/notifikationer" : "/notifikationer?fejl=ukendt");
    }
}