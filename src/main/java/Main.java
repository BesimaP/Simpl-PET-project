import configuration.ThymeleafConfig;
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

            // alle ruter (controllerne meldes til i RouteConfig
            RouteConfig.register(config);

            // fejl, der ikke fanges i controllerne (opsætningen ligger i ExceptionConfig)
            ExceptionConfig.register(config);
        }).start(7070);
    }
}
