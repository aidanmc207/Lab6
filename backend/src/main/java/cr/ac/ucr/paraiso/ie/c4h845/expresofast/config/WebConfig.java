package cr.ac.ucr.paraiso.ie.c4h845.expresofast.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Unica fuente de la politica CORS: la consulta tanto Spring MVC como Spring Security. */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /** Origenes del servidor estatico de desarrollo (Live Server de VS Code). */
    private static final String[] ORIGENES_CLIENTE = {
            "http://localhost:5500",
            "http://127.0.0.1:5500"
    };

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(ORIGENES_CLIENTE)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Authorization", "Location")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
