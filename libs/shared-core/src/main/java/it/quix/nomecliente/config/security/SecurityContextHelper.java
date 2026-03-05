package it.quix.nomecliente.config.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Helper per accedere alle informazioni dell'utente autenticato.
 * Semplifica l'accesso al CustomJwtAuthenticationToken dal SecurityContext.
 */
@Component
public class SecurityContextHelper {

    /**
     * Ottiene l'utente corrente autenticato.
     *
     * @return CustomJwtAuthenticationToken con informazioni utente
     * @throws IllegalStateException se l'utente non è autenticato
     */
    public CustomJwtAuthenticationToken getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("No authenticated user found");
        }

        if (!(authentication instanceof CustomJwtAuthenticationToken)) {
            throw new IllegalStateException("Authentication is not a CustomJwtAuthenticationToken");
        }

        return (CustomJwtAuthenticationToken) authentication;
    }

    /**
     * Ottiene lo username dell'utente corrente.
     * Supporta sia JWT reali che mock user (per i test).
     *
     * @return username
     */
    public String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return "system"; // fallback per contesti senza autenticazione
        }

        // Se è un CustomJwtAuthenticationToken, usa i metodi specifici
        if (authentication instanceof CustomJwtAuthenticationToken customJwt) {
            return customJwt.getUsername();
        }

        // Altrimenti usa il name standard (per test con @WithMockUser)
        return authentication.getName();
    }

    /**
     * Ottiene l'email dell'utente corrente.
     * Supporta sia JWT reali che mock user (per i test).
     *
     * @return email
     */
    public String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof CustomJwtAuthenticationToken customJwt) {
            return customJwt.getEmail();
        }

        // Fallback per test
        return authentication != null ? authentication.getName() + "@test.com" : "system@test.com";
    }

    /**
     * Ottiene il nome completo dell'utente corrente.
     * Supporta sia JWT reali che mock user (per i test).
     *
     * @return nome completo (firstName + lastName)
     */
    public String getCurrentUserFullName() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof CustomJwtAuthenticationToken customJwt) {
            return customJwt.getFullName();
        }

        // Fallback per test
        return authentication != null ? authentication.getName() : "system";
    }

    /**
     * Verifica se l'utente corrente ha un ruolo specifico.
     * Supporta sia JWT reali che mock user (per i test).
     *
     * @param role ruolo da verificare
     * @return true se l'utente ha il ruolo
     */
    public boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof CustomJwtAuthenticationToken customJwt) {
            return customJwt.hasRole(role);
        }

        // Fallback per test con @WithMockUser
        if (authentication != null) {
            return authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_" + role) ||
                                  auth.getAuthority().equals(role));
        }

        return false;
    }

    /**
     * Verifica se l'utente è autenticato.
     *
     * @return true se autenticato
     */
    public boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated();
    }
}




