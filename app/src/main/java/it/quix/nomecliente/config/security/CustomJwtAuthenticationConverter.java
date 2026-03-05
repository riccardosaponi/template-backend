package it.quix.nomecliente.config.security;

import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Converter che trasforma un JWT in CustomJwtAuthenticationToken.
 * Utilizzato da Spring Security per creare l'oggetto Authentication dal JWT.
 */
@Component
@RequiredArgsConstructor
public class CustomJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter;

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        return new CustomJwtAuthenticationToken(
            jwt,
            jwtGrantedAuthoritiesConverter.convert(jwt)
        );
    }
}

