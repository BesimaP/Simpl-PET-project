package configuration;

import exceptions.DatabaseException;
import io.javalin.config.JavalinConfig;

// Vi håndterer de exceptions, som controllerne ikke fanger, ét sted.
// Brugeren ser fejl.html, mens selve fejlen bliver skrevet i konsollen, så vi kan finde den.
public class ExceptionConfig {

    public static void register(JavalinConfig config) {
        // fx hvis databasen ikke kører i Docker
        config.routes.exception(DatabaseException.class, (e, ctx) -> {
            e.printStackTrace();   // viser også den SQLException, der ligger bagved
            ctx.status(500);
            ctx.attribute("message", "Vi kunne ikke få fat i databasen. Prøv igen om lidt.");
            ctx.render("fejl");
        });

        // alle andre uventede fejl. Javalin vælger den mest præcise handler, så DatabaseException rammer stadig den ovenfor
        config.routes.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            ctx.status(500);
            ctx.attribute("message", "Noget gik galt. Prøv igen – eller gå tilbage til forsiden.");
            ctx.render("fejl");
        });
    }
}
