package configuration;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

// Opsætning af Thymeleaf. Bliver kaldt fra Main, når Javalin starter.
// Vores templates ligger i resources/templates og ender på .html.
public class ThymeleafConfig {

    public static TemplateEngine templateEngine() {

        TemplateEngine templateEngine = new TemplateEngine();

        // resolveren finder filen, så ctx.render("dashboard") bliver til templates/dashboard.html
        ClassLoaderTemplateResolver templateResolver =
                new ClassLoaderTemplateResolver();

        templateResolver.setPrefix("templates/");
        templateResolver.setSuffix(".html");

        templateEngine.setTemplateResolver(templateResolver);

        return templateEngine;
    }
}
