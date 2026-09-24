import configuration.ThymeleafConfig;
import controllers.*;
import dao.DatabaseInitializer;
import io.javalin.Javalin;
import io.javalin.rendering.template.JavalinThymeleaf;

// Programmets startpunkt. Gør tre ting: klargør databasen, tænder Javalin (døren på port 7070) og melder alle controllere til.
public class Main {
    public static void main(String[] args) {
        // opret tabellerne, hvis de ikke findes (kører schema.sql)
        DatabaseInitializer.initialize();

        // start Javalin. I Javalin 7 sættes ALT op inde i create(config -> …): statiske filer, templates og ruter
        Javalin app = Javalin.create(config -> {
            // alt i resources/public må hentes direkte (html, css, js, img)
            config.staticFiles.add("/public");

            // templates: ctx.render("side", model) sendes til Thymeleaf (opsætningen ligger i ThymeleafConfig)
            config.fileRenderer(new JavalinThymeleaf(ThymeleafConfig.templateEngine()));

            // hver controller skriver sine ruter (config.routes.post("/…")) på Javalins liste –
            // står en controller ikke her, virker dens formularer ikke
            LoginController.setRoutes(config);
            DashboardController.setRoutes(config);
            HormoneController.setRoutes(config);
            DiaryController.setRoutes(config);
            DiagnosisController.setRoutes(config);
            MedicationController.setRoutes(config);
            AppointmentController.setRoutes(config);
            ProfileController.setRoutes(config);
            // TODO: DocumentController, NotificationController, TimelineController – når de får ruter

            // forsiden: / sender videre til login-siden
            config.routes.get("/", ctx -> ctx.redirect("/login.html"));
        }).start(7070);
    }
}
