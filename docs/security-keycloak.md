# Security: Keycloak JWT Authentication & Authorization

## Overview

Questo backend implementa un sistema di autenticazione e autorizzazione basato su **JWT** (JSON Web Token) emessi da **Keycloak**. L'architettura segue il pattern **OAuth2 Resource Server** di Spring Security, dove il backend agisce come Resource Server protetto.

---

## 📐 Architettura Generale

```
┌──────────────┐         ┌──────────────┐         ┌──────────────┐
│   Frontend   │         │   Keycloak   │         │   Backend    │
│  (Angular/   │         │   (Auth      │         │  (Resource   │
│   React)     │         │   Server)    │         │   Server)    │
└──────┬───────┘         └──────┬───────┘         └──────┬───────┘
       │                        │                        │
       │  1. Login Request      │                        │
       │  (username/password)   │                        │
       ├───────────────────────>│                        │
       │                        │                        │
       │  2. JWT Token          │                        │
       │  (access_token)        │                        │
       │<───────────────────────┤                        │
       │                        │                        │
       │  3. API Request        │                        │
       │  + Bearer Token        │                        │
       ├───────────────────────────────────────────────>│
       │                        │                        │
       │                        │  4. Validate JWT       │
       │                        │  (JWK public keys)     │
       │                        │<───────────────────────┤
       │                        │                        │
       │                        │  5. JWT Valid          │
       │                        │  + User Info           │
       │                        ├───────────────────────>│
       │                        │                        │
       │  6. API Response       │                        │
       │<───────────────────────────────────────────────┤
       │                        │                        │
```

### Flusso di Autenticazione

1. **Login Frontend**: L'utente inserisce credenziali nel frontend
2. **Keycloak Token**: Keycloak valida le credenziali e restituisce un JWT
3. **API Request**: Il frontend invia il JWT come header `Authorization: Bearer <token>`
4. **Validazione JWT**: Il backend valida il token usando le chiavi pubbliche di Keycloak (JWK Set)
5. **Estrazione Claims**: Il backend estrae username, email, ruoli dal JWT
6. **Response**: Il backend processa la richiesta e risponde

---

## 🔐 Componenti di Spring Security

### 1. SecurityConfig

**Classe**: `it.quix.nomecliente.config.security.SecurityConfig`

Configurazione principale di Spring Security che definisce:
- **Protezione endpoint** (quali API sono pubbliche/protette)
- **OAuth2 Resource Server** con validazione JWT
- **Stateless session** (nessuna sessione HTTP)
- **CSRF disabled** (non necessario per API stateless)

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/api/health").permitAll()
                .requestMatchers("/api/**").authenticated()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(customConverter))
            );
        return http.build();
    }
}
```

#### Endpoint Protection

| Pattern | Accesso | Note |
|---------|---------|------|
| `/actuator/health` | Pubblico | Health check per load balancer |
| `/api/health` | Pubblico | Health check applicativo |
| `/swagger-ui/**` | Pubblico (dev only) | Swagger UI |
| `/api/**` | **Autenticato** | Tutte le API business |

---

## 🎫 JWT Token Structure

### Token Example (decoded)

```json
{
  "exp": 1709917200,
  "iat": 1709913600,
  "jti": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "iss": "http://localhost:8180/realms/nomecliente",
  "sub": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "typ": "Bearer",
  "azp": "nomecliente-client",
  "preferred_username": "mario.rossi",
  "email": "mario.rossi@example.com",
  "email_verified": true,
  "given_name": "Mario",
  "family_name": "Rossi",
  "realm_access": {
    "roles": ["user", "admin"]
  },
  "resource_access": {
    "nomecliente-client": {
      "roles": ["client-admin", "viewer"]
    }
  }
}
```

### Claims Utilizzati

| Claim | Descrizione | Uso |
|-------|-------------|-----|
| `preferred_username` | Username utente | Audit trail (`createUser`, `lastUpdateUser`) |
| `email` | Email utente | Notifiche, logging |
| `given_name` | Nome | Display name, UI |
| `family_name` | Cognome | Display name, UI |
| `realm_access.roles` | Ruoli realm Keycloak | Autorizzazione base |
| `resource_access.{client}.roles` | Ruoli client specifici | Autorizzazione avanzata |

---

## 🔄 JWT Validation Flow

```
┌─────────────────────────────────────────────────────────────┐
│  1. HTTP Request con JWT                                    │
│     Authorization: Bearer eyJhbGciOiJSUzI1NiI...            │
└───────────────────────┬─────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────┐
│  2. Spring Security Filter Chain                             │
│     - BearerTokenAuthenticationFilter                        │
│     - Estrae JWT dall'header Authorization                   │
└───────────────────────┬─────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────┐
│  3. JwtDecoder (NimbusJwtDecoder)                           │
│     - Valida firma JWT con chiavi pubbliche di Keycloak     │
│     - Verifica exp (expiration), iss (issuer), aud          │
│     - URL: {keycloak}/realms/{realm}/protocol/openid-       │
│            connect/certs                                     │
└───────────────────────┬─────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────┐
│  4. CustomJwtAuthenticationConverter                        │
│     - Converte JWT in CustomJwtAuthenticationToken          │
└───────────────────────┬─────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────┐
│  5. JwtGrantedAuthoritiesConverter                          │
│     - Estrae ruoli da realm_access.roles                    │
│     - Converte in GrantedAuthority (ROLE_USER, ROLE_ADMIN)  │
└───────────────────────┬─────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────┐
│  6. CustomJwtAuthenticationToken creato                     │
│     - username, email, firstName, lastName, roles            │
│     - Salvato in SecurityContext                            │
└───────────────────────┬─────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────┐
│  7. Request processata dal Controller                        │
│     - SecurityContextHelper.getCurrentUsername()             │
│     - Use Case riceve info utente autenticato                │
└─────────────────────────────────────────────────────────────┘
```

---

## 🧩 Classi Principali

### 1. CustomJwtAuthenticationToken

Estende `JwtAuthenticationToken` per esporre informazioni utente in modo type-safe.

```java
public class CustomJwtAuthenticationToken extends JwtAuthenticationToken {
    private final String username;      // da preferred_username
    private final String email;         // da email
    private final String firstName;     // da given_name
    private final String lastName;      // da family_name
    private final String fullName;      // firstName + lastName
    private final List<String> roles;   // da realm_access.roles
    
    public boolean hasRole(String role);
    public boolean hasAnyRole(String... roles);
}
```

**Perché Custom?**
- **Type safety**: Evita cast e controlli null
- **Convenienza**: Metodi helper per accesso veloce
- **Testabilità**: Facilita mock nei test

### 2. JwtGrantedAuthoritiesConverter

Converte i ruoli del JWT in `GrantedAuthority` di Spring Security.

```java
@Component
public class JwtGrantedAuthoritiesConverter 
    implements Converter<Jwt, Collection<GrantedAuthority>> {
    
    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        // Estrae realm_access.roles
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        List<String> roles = (List<String>) realmAccess.get("roles");
        
        // Converte in ROLE_USER, ROLE_ADMIN, etc.
        return roles.stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
            .collect(Collectors.toList());
    }
}
```

**Pattern: ROLE_prefix**
- Keycloak: `["user", "admin"]`
- Spring Security: `["ROLE_USER", "ROLE_ADMIN"]`

### 3. SecurityContextHelper

Utility class per accesso semplificato alle informazioni utente.

```java
@Component
public class SecurityContextHelper {
    
    public String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof CustomJwtAuthenticationToken jwt) {
            return jwt.getUsername();
        }
        return auth.getName(); // fallback per test
    }
    
    public boolean hasRole(String role) {
        return getCurrentUser().hasRole(role);
    }
}
```

**Vantaggi**:
- **Centralizzato**: Logica di accesso in un solo punto
- **Test-friendly**: Gestisce sia JWT reali che mock user
- **Type-safe**: Evita cast manuali

---

## 🔒 Authorization Patterns

### 1. Protezione a Livello Controller

```java
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    
    @GetMapping("/stats")
    public StatsDto getStats() {
        // Solo ADMIN può accedere
    }
}
```

### 2. Protezione a Livello Metodo

```java
@Service
public class EntityService {
    
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public void deleteEntity(UUID id) {
        // Solo ADMIN o EDITOR
    }
    
    @PreAuthorize("hasRole('VIEWER')")
    public EntityDto getEntity(UUID id) {
        // Qualsiasi utente con ruolo VIEWER
    }
}
```

### 3. Protezione Programmatica

```java
@Service
@RequiredArgsConstructor
public class EntityUseCaseImpl implements EntityUseCase {
    
    private final SecurityContextHelper securityHelper;
    
    public void execute(EntityRequest request) {
        String currentUser = securityHelper.getCurrentUsername();
        
        // Business logic con controllo custom
        if (!securityHelper.hasRole("admin") && 
            !request.getOwner().equals(currentUser)) {
            throw new ForbiddenException("Non sei il proprietario");
        }
        
        // ... business logic
    }
}
```

---

## 📊 Security Context Flow

```
┌─────────────────────────────────────────────────────────┐
│  Thread-Local SecurityContext                           │
│  ┌────────────────────────────────────────────────┐    │
│  │  Authentication (CustomJwtAuthenticationToken) │    │
│  │  ├─ username: "mario.rossi"                    │    │
│  │  ├─ email: "mario.rossi@example.com"           │    │
│  │  ├─ fullName: "Mario Rossi"                    │    │
│  │  ├─ roles: ["user", "admin"]                   │    │
│  │  └─ authorities: [ROLE_USER, ROLE_ADMIN]       │    │
│  └────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────┘
                        │
                        │ SecurityContextHelper
                        │
                        ▼
┌─────────────────────────────────────────────────────────┐
│  Use Case / Service                                      │
│                                                          │
│  String user = securityHelper.getCurrentUsername();     │
│  // "mario.rossi"                                       │
│                                                          │
│  entity.setCreateUser(user);                            │
│  // Audit trail con utente reale                        │
└─────────────────────────────────────────────────────────┘
```

---

## 🧪 Testing con Security

### BaseUseCaseIT - Mock User

```java
@SpringBootTest
@Transactional
@WithMockUser(username = "test-user", roles = {"USER"})
public abstract class BaseUseCaseIT {
    // Test con utente mockato "test-user"
}
```

### SecurityContextHelper - Test Compatibility

Il `SecurityContextHelper` è stato progettato per funzionare sia con JWT reali che con `@WithMockUser`:

```java
public String getCurrentUsername() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    
    // JWT reale in produzione
    if (auth instanceof CustomJwtAuthenticationToken jwt) {
        return jwt.getUsername();
    }
    
    // Mock user nei test
    return auth.getName(); // "test-user"
}
```

### Test di Autorizzazione

```java
@Test
@WithMockUser(username = "admin", roles = {"ADMIN"})
void shouldAllowAdmin() {
    // Test passa - utente ha ruolo ADMIN
    adminService.deleteAll();
}

@Test
@WithMockUser(username = "user", roles = {"USER"})
void shouldDenyUser() {
    // Test fallisce con AccessDeniedException
    assertThrows(AccessDeniedException.class, () -> {
        adminService.deleteAll();
    });
}
```

---

## 🛠️ Configurazione Keycloak

### 1. Realm Setup

```yaml
Realm: nomecliente
├── Clients
│   └── nomecliente-client
│       ├── Client Protocol: openid-connect
│       ├── Access Type: public
│       └── Valid Redirect URIs: http://localhost:4200/*
├── Roles
│   ├── user (default)
│   ├── admin
│   └── editor
└── Users
    └── mario.rossi
        ├── Email: mario.rossi@example.com
        ├── Roles: user, admin
        └── Password: ***
```

### 2. Client Scopes

```
Standard Claims (default):
- profile: given_name, family_name, preferred_username
- email: email, email_verified
- roles: realm_access.roles, resource_access
```

### 3. Token Lifespan

```yaml
Access Token Lifespan: 5 minutes (300s)
SSO Session Idle: 30 minutes
SSO Session Max: 10 hours
Client Session Idle: 30 minutes
Client Session Max: 10 hours
```

---

## 🔑 Ottenere un Token JWT

### 1. Password Grant (Development)

```bash
curl -X POST http://localhost:8180/realms/nomecliente/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=nomecliente-client" \
  -d "username=mario.rossi" \
  -d "password=password123"
```

**Response:**
```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expires_in": 300,
  "refresh_expires_in": 1800,
  "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "Bearer",
  "not-before-policy": 0,
  "session_state": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "scope": "profile email"
}
```

### 2. Authorization Code Flow (Production)

```
1. Frontend redirect to Keycloak:
   http://localhost:8180/realms/nomecliente/protocol/openid-connect/auth
     ?client_id=nomecliente-client
     &redirect_uri=http://localhost:4200/callback
     &response_type=code
     &scope=openid profile email

2. User login in Keycloak

3. Keycloak redirect con authorization code:
   http://localhost:4200/callback?code=abc123...

4. Frontend exchange code for token:
   POST /token
   grant_type=authorization_code
   code=abc123...
   redirect_uri=...
```

---

## 🚨 Gestione Errori Security

### 401 Unauthorized - Token Mancante o Invalido

```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "Full authentication is required to access this resource",
  "path": "/api/v1/entities"
}
```

**Cause comuni**:
- Token non presente nell'header `Authorization`
- Token scaduto (`exp` claim)
- Token con firma invalida
- Issuer non corrispondente

### 403 Forbidden - Autorizzazione Insufficiente

```json
{
  "status": 403,
  "error": "Forbidden",
  "message": "Access Denied",
  "path": "/api/v1/admin/stats"
}
```

**Cause comuni**:
- Utente autenticato ma senza il ruolo richiesto
- `@PreAuthorize("hasRole('ADMIN')")` fallisce
- Controllo custom di autorizzazione fallisce

---

## 📈 Best Practices

### ✅ DO

1. **Valida sempre il JWT lato server**
   - Non fidarti mai del token senza validazione
   - Usa le chiavi pubbliche di Keycloak (JWK Set)

2. **Usa SecurityContextHelper**
   ```java
   String user = securityHelper.getCurrentUsername();
   entity.setCreateUser(user); // ✅
   ```

3. **Stateless sessions**
   - No `HttpSession`
   - Ogni request contiene tutte le info (JWT)

4. **Audit trail con utente reale**
   ```java
   entity.setLastUpdateUser(securityHelper.getCurrentUsername());
   ```

5. **Proteggi endpoint sensibili**
   ```java
   @PreAuthorize("hasRole('ADMIN')")
   ```

### ❌ DON'T

1. **Non chiamare Keycloak per ogni request**
   - ❌ Request → Keycloak verification → Response
   - ✅ Request → Local JWT validation → Response

2. **Non salvare password in chiaro**
   - Keycloak gestisce le password
   - Backend riceve solo JWT

3. **Non usare utente hardcoded**
   ```java
   entity.setCreateUser("system"); // ❌
   entity.setCreateUser(securityHelper.getCurrentUsername()); // ✅
   ```

4. **Non esporre endpoint senza protezione**
   ```java
   @RequestMapping("/api/admin")
   // ❌ Manca @PreAuthorize
   ```

5. **Non loggare JWT completi**
   ```java
   log.info("Token: {}", jwt); // ❌ Security risk
   log.info("User: {}", username); // ✅
   ```

---

## 🔍 Debugging

### Verifica Token JWT

**Usa jwt.io** per decodificare e verificare il token:
1. Copia il token da Keycloak
2. Vai su https://jwt.io
3. Incolla il token
4. Verifica claims e scadenza

### Log Spring Security

```yaml
logging:
  level:
    org.springframework.security: DEBUG
    org.springframework.security.oauth2: TRACE
```

### SecurityContext Inspection

```java
Authentication auth = SecurityContextHolder.getContext().getAuthentication();
log.debug("Auth type: {}", auth.getClass().getName());
log.debug("Principal: {}", auth.getPrincipal());
log.debug("Authorities: {}", auth.getAuthorities());
```

---

## 📚 Riferimenti

- **Spring Security**: https://spring.io/projects/spring-security
- **OAuth2 Resource Server**: https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html
- **Keycloak**: https://www.keycloak.org/documentation
- **JWT RFC**: https://datatracker.ietf.org/doc/html/rfc7519
- **SECURITY-JWT-KEYCLOAK.md**: Guida implementazione completa

---

## ✅ Checklist Implementazione

- [x] Spring Security configurato
- [x] OAuth2 Resource Server attivo
- [x] JWT validation con Keycloak JWK
- [x] CustomJwtAuthenticationToken per info utente
- [x] SecurityContextHelper per accesso semplificato
- [x] Audit trail con username reale
- [x] Test compatibili con @WithMockUser
- [x] Endpoint protection configurata
- [x] Error handling per 401/403
- [x] Documentazione completa

**🔐 Sistema di sicurezza production-ready implementato!**

