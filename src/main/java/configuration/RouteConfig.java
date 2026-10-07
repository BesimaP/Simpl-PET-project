package configuration;
import controllers.*;
import io.javalin.config.JavalinConfig;
import services.NotificationService;
import persistence.ConnectionPool;

// Samler registreringen af alle controllere ét sted, så Main slipper for den lange liste.
public class RouteConfig {

    public static void register(JavalinConfig config, ConnectionPool connectionPool) {
        // hver controller skriver sine ruter (config.routes.post("/…")) på Javalins liste –
        // står en controller ikke her, virker dens formularer ikke
        new LoginController(connectionPool).setRoutes(config);
        new DashboardController(connectionPool).setRoutes(config);
        HormoneController.setRoutes(config);
        DiaryController.setRoutes(config);
        DiagnosisController.setRoutes(config, connectionPool);
        MedicationController.setRoutes(config);
        AppointmentController.setRoutes(config);
        new ProfileController(connectionPool).setRoutes(config);
        new NotificationController(connectionPool).setRoutes(config);
        new TimelineController(connectionPool).setRoutes(config);
        DocumentController.setRoutes(config);

        config.routes.get("/", ctx -> ctx.redirect("/login"));

        // before = kører FØR hver eneste rute. Er man logget ind, lægges antal ulæste påmindelser i request scope,
        // så klokken på ALLE sider kan vise den røde prik (${unread}) – uden at hver controller skal gentage det
        config.routes.before("/*", ctx -> {
            Integer patientId = ctx.sessionAttribute("patientId");
            if (patientId != null) {
                ctx.attribute("unread", new NotificationService(connectionPool).countUnread(patientId));
            }
        });
    }
}