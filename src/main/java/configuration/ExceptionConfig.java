package configuration;

import exceptions.DatabaseException;
import io.javalin.config.JavalinConfig;

// Samlet håndtering af exceptions, der ikke fanges i controllerne. Kaldes én gang fra Main: ExceptionConfig.register(config)
public class ExceptionConfig {

    public static void register(JavalinConfig config) {
        // kaster en DAO en DatabaseException, ender den her i stedet for som en 500-side uden forklaring.
        // (senere: ctx.render("error") med beskeden, når fejlsiden er en template)
        config.routes.exception(DatabaseException.class, (e, ctx) -> {
            ctx.status(500);
            ctx.result("Databasefejl: " + e.getMessage());
        });
    }
}
