package it.quix.nomecliente.config.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Converter che estrae i ruoli dal JWT di Keycloak e li converte in GrantedAuthority.
 *
 * Estrae:
 * - Realm roles da "realm_access.roles"
 * - Client roles da "resource_access.{client-id}.roles"
 */
@Component
public class JwtGrantedAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final String REALM_ACCESS_CLAIM = "realm_access";
    private static final String RESOURCE_ACCESS_CLAIM = "resource_access";
    private static final String ROLES_CLAIM = "roles";
    private static final String ROLE_PREFIX = "ROLE_";

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Set<GrantedAuthority> authorities = new HashSet<>();

        // Estrai realm roles
        authorities.addAll(extractRealmRoles(jwt));

        // Estrai client roles (opzionale, se necessario)
        authorities.addAll(extractClientRoles(jwt));

        return authorities;
    }

    @SuppressWarnings("unchecked")
    private Collection<GrantedAuthority> extractRealmRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim(REALM_ACCESS_CLAIM);

        if (realmAccess == null || !realmAccess.containsKey(ROLES_CLAIM)) {
            return Collections.emptyList();
        }

        List<String> roles = (List<String>) realmAccess.get(ROLES_CLAIM);

        return roles.stream()
                .map(role -> new SimpleGrantedAuthority(ROLE_PREFIX + role.toUpperCase()))
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private Collection<GrantedAuthority> extractClientRoles(Jwt jwt) {
        Map<String, Object> resourceAccess = jwt.getClaim(RESOURCE_ACCESS_CLAIM);

        if (resourceAccess == null) {
            return Collections.emptyList();
        }

        Set<GrantedAuthority> clientAuthorities = new HashSet<>();

        // Itera su tutti i client e i loro ruoli
        resourceAccess.values().forEach(resource -> {
            if (resource instanceof Map) {
                Map<String, Object> clientResource = (Map<String, Object>) resource;
                if (clientResource.containsKey(ROLES_CLAIM)) {
                    List<String> roles = (List<String>) clientResource.get(ROLES_CLAIM);
                    roles.forEach(role ->
                        clientAuthorities.add(new SimpleGrantedAuthority(ROLE_PREFIX + role.toUpperCase()))
                    );
                }
            }
        });

        return clientAuthorities;
    }
}

