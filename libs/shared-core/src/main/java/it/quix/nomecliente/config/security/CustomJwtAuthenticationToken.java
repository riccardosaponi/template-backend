package it.quix.nomecliente.config.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Custom JWT Authentication Token che estrae e espone le informazioni utente dal JWT di Keycloak.
 *
 * Fornisce accesso a:
 * - username
 * - email
 * - nome completo (firstName + lastName)
 * - ruoli (realm roles + client roles)
 */
@Getter
public class CustomJwtAuthenticationToken extends JwtAuthenticationToken {

    private final String username;
    private final String email;
    private final String firstName;
    private final String lastName;
    private final String fullName;
    private final List<String> roles;

    public CustomJwtAuthenticationToken(Jwt jwt, Collection<? extends GrantedAuthority> authorities) {
        super(jwt, authorities);

        // Estrai username (preferred_username è lo standard Keycloak)
        this.username = jwt.getClaimAsString("preferred_username");

        // Estrai email
        this.email = jwt.getClaimAsString("email");

        // Estrai nome e cognome
        this.firstName = jwt.getClaimAsString("given_name");
        this.lastName = jwt.getClaimAsString("family_name");
        this.fullName = buildFullName(firstName, lastName);

        // Estrai ruoli (realm roles + client roles)
        this.roles = extractRoles(jwt);
    }

    private String buildFullName(String firstName, String lastName) {
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        } else if (firstName != null) {
            return firstName;
        } else if (lastName != null) {
            return lastName;
        }
        return username; // fallback
    }

    @SuppressWarnings("unchecked")
    private List<String> extractRoles(Jwt jwt) {
        // Estrai realm roles
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        List<String> realmRoles = realmAccess != null
            ? (List<String>) realmAccess.get("roles")
            : List.of();

        // Estrai resource access (client roles)
        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");

        // Combina tutti i ruoli
        return realmRoles != null ? realmRoles : List.of();
    }

    /**
     * Verifica se l'utente ha un ruolo specifico.
     */
    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    /**
     * Verifica se l'utente ha almeno uno dei ruoli specificati.
     */
    public boolean hasAnyRole(String... roles) {
        for (String role : roles) {
            if (this.roles.contains(role)) {
                return true;
            }
        }
        return false;
    }
}

