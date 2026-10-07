import configuration.ExceptionConfig;
import configuration.RouteConfig;
import configuration.ThymeleafConfig;
import io.javalin.Javalin;
import io.javalin.rendering.template.JavalinThymeleaf;
import persistence.ConnectionPool;

// Programmets startpunkt. Gør tre ting: klargør databasen, tænder Javalin (døren på port 7070) og melder alle controllere til.
public class Main {
    // hvor databasen er: brugernavn, kodeord, adresse og databasens navn
    private static final String USER = "postgres";
    private static final String PASSWORD = "postgres";
    private static final String URL = "jdbc:postgresql://localhost:5432/%s?currentSchema=public";
    private static final String DB = "Simpl";

    // nøgleringen: laves ÉN gang her og gives videre til controllerne
    private static final ConnectionPool connectionPool = ConnectionPool.getInstance(USER, PASSWORD, URL, DB);
    public static void main(String[] args) {

        // start Javalin. I Javalin 7 sættes ALT op inde i create(config -> …): statiske filer, templates og ruter
        Javalin app = Javalin.create(config -> {
            // alt i resources/public må hentes direkte (html, css, js, img)
            config.staticFiles.add("/public");

            // templates: ctx.render("side", model) sendes til Thymeleaf (opsætningen ligger i ThymeleafConfig)
            config.fileRenderer(new JavalinThymeleaf(ThymeleafConfig.templateEngine()));

            // alle ruter (controllerne meldes til i RouteConfig) – nøgleringen gives med
            RouteConfig.register(config, connectionPool);

            // fejl, der ikke fanges i controllerne (opsætningen ligger i ExceptionConfig)
            ExceptionConfig.register(config);
        }).start(7070);
    }
}
