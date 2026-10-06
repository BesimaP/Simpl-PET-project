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
        LoginController.setRoutes(config);
        DashboardController.setRoutes(config);
        HormoneController.setRoutes(config);
        DiaryController.setRoutes(config);
        DiagnosisController.setRoutes(config);
        MedicationController.setRoutes(config);
        AppointmentController.setRoutes(config);
        ProfileController.setRoutes(config);
        NotificationController.setRoutes(config);
        TimelineController.setRoutes(config);
        DocumentController.setRoutes(config);

        config.routes.get("/", ctx -> ctx.redirect("/login"));

        // before = kører FØR hver eneste rute. Er man logget ind, lægges antal ulæste påmindelser i request scope,
        // så klokken på ALLE sider kan vise den røde prik (${unread}) – uden at hver controller skal gentage det
        config.routes.before("/*", ctx -> {
            Integer patientId = ctx.sessionAttribute("patientId");
            if (patientId != null) {
                ctx.attribute("unread", new NotificationService().countUnread(patientId));
            }
        });
    }
}