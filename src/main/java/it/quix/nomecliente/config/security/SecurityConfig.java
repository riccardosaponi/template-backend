package it.quix.nomecliente.config.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configurazione Spring Security per proteggere le API con JWT di Keycloak.
 *
 * Protezione:
 * - Tutte le API richiedono autenticazione (tranne health check)
 * - JWT validato tramite Keycloak
 * - Stateless (no session)
 * - CORS configurato
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomJwtAuthenticationConverter customJwtAuthenticationConverter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disabilita CSRF (API stateless con JWT)
            .csrf(AbstractHttpConfigurer::disable)

            // Configura sessioni stateless
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // Configura autorizzazioni
            .authorizeHttpRequests(auth -> auth
                // Health check pubblico (per load balancer)
                .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                .requestMatchers("/api/health").permitAll()

                // Swagger UI pubblico (opzionale, commentare per produzione)
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

                // Tutte le altre API richiedono autenticazione
                .requestMatchers("/api/**").authenticated()

                // Tutti gli altri endpoint richiedono autenticazione
                .anyRequest().authenticated()
            )

            // Configura OAuth2 Resource Server con JWT
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtAuthenticationConverter(customJwtAuthenticationConverter)
                )
            );

        return http.build();
    }
}

