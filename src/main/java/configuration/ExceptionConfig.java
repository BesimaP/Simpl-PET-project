package configuration;

import exceptions.DatabaseException;
import io.javalin.config.JavalinConfig;

// Samlet håndtering af exceptions, der ikke fanges i controllerne. Kaldes én gang fra Main: ExceptionConfig.register(config)
// Brugeren ser en pæn fejlside (templates/fejl.html) – den rigtige fejl skrives i konsollen i IntelliJ, så I kan finde den.
public class ExceptionConfig {

    public static void register(JavalinConfig config) {
        // kaster en mapper en DatabaseException (fx er databasen ikke startet i Docker)
        config.routes.exception(DatabaseException.class, (e, ctx) -> {
            e.printStackTrace();   // hele fejlen (også SQLException bagved) i konsollen
            ctx.status(500);
            ctx.attribute("message", "Vi kunne ikke få fat i databasen. Prøv igen om lidt.");
            ctx.render("fejl");
        });

        // alle andre fejl, vi ikke har forudset (fx en fil, der ikke kunne gemmes). Javalin vælger den mest præcise handler,
        // så en DatabaseException ender stadig i den ovenfor
        config.routes.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            ctx.status(500);
            ctx.attribute("message", "Noget gik galt. Prøv igen – eller gå tilbage til forsiden.");
            ctx.render("fejl");
        });
    }
}
