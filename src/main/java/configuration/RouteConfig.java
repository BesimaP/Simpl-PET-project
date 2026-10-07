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

        // before = kører FØR hver eneste rute. Er man logget ind, lægges antal ulæste påmindelser i request scope,
        // så klokken på ALLE sider kan vise den røde prik (${unread}) – uden at hver controller skal gentage det
        // NotificationService laves ÉN gang her – ikke ved hver request inde i lambdaen.
        // Statiske filer (css/js/img) springes over: de har ingen klokke, så der er ingen grund til at spørge databasen
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