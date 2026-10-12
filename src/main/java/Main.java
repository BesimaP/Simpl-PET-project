import configuration.ExceptionConfig;
import configuration.RouteConfig;
import configuration.ThymeleafConfig;
import io.javalin.Javalin;
import io.javalin.rendering.template.JavalinThymeleaf;
import persistence.ConnectionPool;

// Her starter programmet. Vi opretter forbindelsen til databasen, starter Javalin på port 7070 og registrerer controllerne.
public class Main {
    // login og adresse til vores lokale PostgreSQL-database
    private static final String USER = "postgres";
    private static final String PASSWORD = "postgres";
    private static final String URL = "jdbc:postgresql://localhost:5432/%s?currentSchema=public";
    private static final String DB = "Simpl";

    // connection pool'en laves én gang her og sendes videre til controllerne
    private static final ConnectionPool connectionPool = ConnectionPool.getInstance(USER, PASSWORD, URL, DB);
    public static void main(String[] args) {

        // i Javalin 7 sættes det hele op inde i create(config -> ...)
        Javalin app = Javalin.create(config -> {
            // filerne i resources/public (css, js, billeder) kan hentes direkte
            config.staticFiles.add("/public");

            // ctx.render(...) bruger Thymeleaf, se ThymeleafConfig
            config.fileRenderer(new JavalinThymeleaf(ThymeleafConfig.templateEngine()));

            // alle ruter registreres i RouteConfig
            RouteConfig.register(config, connectionPool);

            // fejl, som controllerne ikke selv fanger, håndteres i ExceptionConfig
            ExceptionConfig.register(config);
        }).start(7070);
    }
}
