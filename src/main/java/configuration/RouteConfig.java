package configuration;
import controllers.*;
import io.javalin.config.JavalinConfig;

// Samler registreringen af alle controllere ét sted, så Main slipper for den lange liste.
public class RouteConfig {

    public static void register(JavalinConfig config) {
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

        config.routes.get("/", ctx -> ctx.redirect("/login"));
    }
}