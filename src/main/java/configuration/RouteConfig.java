package configuration;
import controllers.*;
import io.javalin.config.JavalinConfig;
import services.NotificationService;
import persistence.ConnectionPool;

// Vi samler alle controllere her, så Main ikke bliver rodet.
public class RouteConfig {

    public static void register(JavalinConfig config, ConnectionPool connectionPool) {
        // hver controller registrerer sine egne ruter. Mangler en controller her, virker dens sider ikke
        new LoginController(connectionPool).setRoutes(config);
        new DashboardController(connectionPool).setRoutes(config);
        new HormoneController(connectionPool).setRoutes(config);
        new DiaryController(connectionPool).setRoutes(config);
        new DiagnosisController(connectionPool).setRoutes(config);
        new MedicationController(connectionPool).setRoutes(config);
        new AppointmentController(connectionPool).setRoutes(config);
        new ProfileController(connectionPool).setRoutes(config);
        new NotificationController(connectionPool).setRoutes(config);
        new TimelineController(connectionPool).setRoutes(config);
        new DocumentController(connectionPool).setRoutes(config);

        config.routes.get("/", ctx -> ctx.redirect("/login"));

        // before kører før hver rute og lægger antal ulæste påmindelser i ${unread}, så klokken virker på alle sider.
        // Statiske filer springer vi over, så vi ikke spørger databasen unødigt
        NotificationService notificationService = new NotificationService(connectionPool);
        config.routes.before("/*", ctx -> {
            String path = ctx.path();
            if (path.startsWith("/css/") || path.startsWith("/js/") || path.startsWith("/img/")) {
                return;
            }
            Integer patientId = ctx.sessionAttribute("patientId");
            if (patientId != null) {
                ctx.attribute("unread", notificationService.countUnread(patientId));
            }
        });
    }
}