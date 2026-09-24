package configuration;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

// Opsætning af Thymeleaf (template-motoren). Kaldes én gang fra Main, når Javalin startes.
// Fortæller Thymeleaf, HVOR skabelonerne ligger (resources/templates) og at de ender på .html.
public class ThymeleafConfig {

    // Motoren, der fylder en skabelon med data: ctx.render("dashboard", model) -> færdig HTML til browseren
    public static TemplateEngine templateEngine() {

        TemplateEngine templateEngine = new TemplateEngine();

        // "Resolver" = den, der finder filen. ctx.render("dashboard") -> templates/dashboard.html på classpath
        ClassLoaderTemplateResolver templateResolver =
                new ClassLoaderTemplateResolver();

        templateResolver.setPrefix("templates/"); // mappen (resources/templates lander som templates/ på classpath)
        templateResolver.setSuffix(".html");      // filendelsen sættes automatisk på

        templateEngine.setTemplateResolver(templateResolver);

        return templateEngine;
    }
}
