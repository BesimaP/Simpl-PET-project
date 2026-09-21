import controller.DashboardController;
import controller.DiagnosisController;
import controller.DiaryController;
import controller.HormoneController;
import controller.LoginController;
import dao.DatabaseInitializer;
import io.javalin.Javalin;

public class Main {
    public static void main(String[] args) {
        // opret tabellerne, hvis de ikke findes (kører schema.sql)
        DatabaseInitializer.initialize();

        // start Javalin og sig: alt i resources/public må hentes direkte (html, css, js, img)
        Javalin app = Javalin.create(config -> {
            config.staticFiles.add("/public");
        }).start(7070);

        LoginController.registerRoutes(app);
        DashboardController.registerRoutes(app);
        HormoneController.registerRoutes(app);
        DiaryController.registerRoutes(app);
        DiagnosisController.registerRoutes(app);

        // forsiden: / sender videre til login-siden
        app.get("/", ctx -> ctx.redirect("/login.html"));
    }
}