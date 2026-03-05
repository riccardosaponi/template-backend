package it.quix.nomecliente.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Centralized CORS configuration.
 *
 * <p>All modules share this configuration via the shared-core dependency.
 * Use this class to manage allowed origins, methods, and headers globally.
 * Do NOT use {@code @CrossOrigin} on individual controllers.</p>
 *
 * <p>Override allowed origins via the {@code CORS_ALLOWED_ORIGINS} environment variable.</p>
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
