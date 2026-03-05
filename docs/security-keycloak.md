# Security: Keycloak JWT Authentication & Authorization

## Overview

This backend implements an authentication and authorisation system based on **JWT** (JSON Web Token) issued by **Keycloak**. The architecture follows the Spring Security **OAuth2 Resource Server** pattern, where the backend acts as a protected Resource Server.

---

## Architecture

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

### Authentication Flow

1. **Frontend Login**: The user enters credentials in the frontend.
2. **Keycloak Token**: Keycloak validates the credentials and returns a JWT.
3. **API Request**: The frontend sends the JWT as an `Authorization: Bearer <token>` header.
4. **JWT Validation**: The backend validates the token using Keycloak's public keys (JWK Set).
5. **Claims Extraction**: The backend extracts username, email, and roles from the JWT.
6. **Response**: The backend processes the request and responds.

---

## Spring Security Components

### 1. SecurityConfig

**Class**: `it.quix.nomecliente.config.security.SecurityConfig`

Main Spring Security configuration defining:
- **Endpoint protection** (which APIs are public / protected)
- **OAuth2 Resource Server** with JWT validation
- **Stateless session** (no HTTP session)
- **CSRF disabled** (not needed for stateless APIs)

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

| Pattern | Access | Notes |
|---------|--------|-------|
| `/actuator/health` | Public | Health check for load balancer |
| `/api/health` | Public | Application health check |
| `/swagger-ui/**` | Public (dev only) | Swagger UI |
| `/api/**` | **Authenticated** | All business APIs |

---

## JWT Token Structure

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

### Claims Used

| Claim | Description | Usage |
|-------|-------------|-------|
| `preferred_username` | Username | Audit trail (`createUser`, `lastUpdateUser`) |
| `email` | User email | Notifications, logging |
| `given_name` | First name | Display name, UI |
| `family_name` | Last name | Display name, UI |
| `realm_access.roles` | Keycloak realm roles | Base authorisation |
| `resource_access.{client}.roles` | Client-specific roles | Advanced authorisation |

---

## JWT Validation Flow

```
┌─────────────────────────────────────────────────────────────┐
│  1. HTTP Request with JWT                                   │
│     Authorization: Bearer eyJhbGciOiJSUzI1NiI...            │
└───────────────────────┬─────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────┐
│  2. Spring Security Filter Chain                             │
│     - BearerTokenAuthenticationFilter                        │
│     - Extracts JWT from Authorization header                 │
└───────────────────────┬─────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────┐
│  3. JwtDecoder (NimbusJwtDecoder)                           │
│     - Validates JWT signature with Keycloak public keys     │
│     - Verifies exp (expiration), iss (issuer), aud          │
│     - URL: {keycloak}/realms/{realm}/protocol/openid-       │
│            connect/certs                                     │
└───────────────────────┬─────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────┐
│  4. CustomJwtAuthenticationConverter                        │
│     - Converts JWT into CustomJwtAuthenticationToken        │
└───────────────────────┬─────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────┐
│  5. JwtGrantedAuthoritiesConverter                          │
│     - Extracts roles from realm_access.roles                │
│     - Converts to GrantedAuthority (ROLE_USER, ROLE_ADMIN)  │
└───────────────────────┬─────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────┐
│  6. CustomJwtAuthenticationToken created                    │
│     - username, email, firstName, lastName, roles            │
│     - Stored in SecurityContext                             │
└───────────────────────┬─────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────┐
│  7. Request processed by the Controller                      │
│     - SecurityContextHelper.getCurrentUsername()             │
│     - Use case receives authenticated user info              │
└─────────────────────────────────────────────────────────────┘
```

---

## Key Classes

### 1. CustomJwtAuthenticationToken

Extends `JwtAuthenticationToken` to expose user information in a type-safe way.

```java
public class CustomJwtAuthenticationToken extends JwtAuthenticationToken {
    private final String username;      // from preferred_username
    private final String email;         // from email
    private final String firstName;     // from given_name
    private final String lastName;      // from family_name
    private final String fullName;      // firstName + lastName
    private final List<String> roles;   // from realm_access.roles
    
    public boolean hasRole(String role);
    public boolean hasAnyRole(String... roles);
}
```

**Why custom?**
- **Type safety**: Avoids casts and null checks.
- **Convenience**: Helper methods for fast access.
- **Testability**: Easier to mock in tests.

### 2. JwtGrantedAuthoritiesConverter

Converts JWT roles into Spring Security `GrantedAuthority` objects.

```java
@Component
public class JwtGrantedAuthoritiesConverter 
    implements Converter<Jwt, Collection<GrantedAuthority>> {
    
    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        // Extract realm_access.roles
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        List<String> roles = (List<String>) realmAccess.get("roles");
        
        // Convert to ROLE_USER, ROLE_ADMIN, etc.
        return roles.stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
            .collect(Collectors.toList());
    }
}
```

**ROLE_ prefix pattern**
- Keycloak: `["user", "admin"]`
- Spring Security: `["ROLE_USER", "ROLE_ADMIN"]`

### 3. SecurityContextHelper

Utility class for simplified access to authenticated user information.

```java
@Component
public class SecurityContextHelper {
    
    public String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof CustomJwtAuthenticationToken jwt) {
            return jwt.getUsername();
        }
        return auth.getName(); // fallback for tests
    }
    
    public boolean hasRole(String role) {
        return getCurrentUser().hasRole(role);
    }
}
```

**Benefits:**
- **Centralised**: Access logic in a single place.
- **Test-friendly**: Handles both real JWT and mock users.
- **Type-safe**: Avoids manual casts.

---

## Authorisation Patterns

### 1. Controller-Level Protection

```java
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    
    @GetMapping("/stats")
    public StatsDto getStats() {
        // Only ADMIN can access
    }
}
```

### 2. Method-Level Protection

```java
@Service
public class EntityService {
    
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public void deleteEntity(UUID id) {
        // Only ADMIN or EDITOR
    }
    
    @PreAuthorize("hasRole('VIEWER')")
    public EntityDto getEntity(UUID id) {
        // Any user with VIEWER role
    }
}
```

### 3. Programmatic Protection

```java
@Service
@RequiredArgsConstructor
public class EntityUseCaseImpl implements EntityUseCase {
    
    private final SecurityContextHelper securityHelper;
    
    public void execute(EntityRequest request) {
        String currentUser = securityHelper.getCurrentUsername();
        
        // Custom business-logic authorisation check
        if (!securityHelper.hasRole("admin") && 
            !request.getOwner().equals(currentUser)) {
            throw new ForbiddenException("You are not the owner");
        }
        
        // ... business logic
    }
}
```

---

## Security Context Flow

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
│  // Audit trail with real user                          │
└─────────────────────────────────────────────────────────┘
```

---

## Testing with Security

### BaseUseCaseIT — Mock User

```java
@SpringBootTest
@Transactional
@WithMockUser(username = "test-user", roles = {"USER"})
public abstract class BaseUseCaseIT {
    // Tests run with mocked "test-user"
}
```

### SecurityContextHelper — Test Compatibility

`SecurityContextHelper` is designed to work with both real JWT tokens and `@WithMockUser`:

```java
public String getCurrentUsername() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    
    // Real JWT in production
    if (auth instanceof CustomJwtAuthenticationToken jwt) {
        return jwt.getUsername();
    }
    
    // Mock user in tests
    return auth.getName(); // "test-user"
}
```

### Authorisation Tests

```java
@Test
@WithMockUser(username = "admin", roles = {"ADMIN"})
void shouldAllowAdmin() {
    // Test passes — user has ADMIN role
    adminService.deleteAll();
}

@Test
@WithMockUser(username = "user", roles = {"USER"})
void shouldDenyUser() {
    // Test fails with AccessDeniedException
    assertThrows(AccessDeniedException.class, () -> {
        adminService.deleteAll();
    });
}
```

---

## Keycloak Configuration

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

## Obtaining a JWT Token

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
1. Frontend redirects to Keycloak:
   http://localhost:8180/realms/nomecliente/protocol/openid-connect/auth
     ?client_id=nomecliente-client
     &redirect_uri=http://localhost:4200/callback
     &response_type=code
     &scope=openid profile email

2. User logs in via Keycloak

3. Keycloak redirects with authorization code:
   http://localhost:4200/callback?code=abc123...

4. Frontend exchanges code for token:
   POST /token
   grant_type=authorization_code
   code=abc123...
   redirect_uri=...
```

---

## Security Error Handling

### 401 Unauthorized — Missing or Invalid Token

```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "Full authentication is required to access this resource",
  "path": "/api/v1/entities"
}
```

**Common causes:**
- `Authorization` header missing.
- Expired token (`exp` claim).
- Invalid token signature.
- Issuer mismatch.

### 403 Forbidden — Insufficient Permissions

```json
{
  "status": 403,
  "error": "Forbidden",
  "message": "Access Denied",
  "path": "/api/v1/admin/stats"
}
```

**Common causes:**
- Authenticated user missing the required role.
- `@PreAuthorize("hasRole('ADMIN')")` fails.
- Custom authorisation check fails.

---

## Best Practices

### DO

1. **Always validate the JWT server-side** — never trust a token without validation; use Keycloak's public keys (JWK Set).

2. **Use SecurityContextHelper**
   ```java
   String user = securityHelper.getCurrentUsername();
   entity.setCreateUser(user); // ✅
   ```

3. **Stateless sessions** — no `HttpSession`; every request carries all information (JWT).

4. **Audit trail with real user**
   ```java
   entity.setLastUpdateUser(securityHelper.getCurrentUsername());
   ```

5. **Protect sensitive endpoints**
   ```java
   @PreAuthorize("hasRole('ADMIN')")
   ```

### DON'T

1. **Do not call Keycloak on every request** — validate the JWT locally using cached public keys.

2. **Do not store passwords** — Keycloak manages passwords; the backend only receives JWT tokens.

3. **Do not use hardcoded users**
   ```java
   entity.setCreateUser("system"); // ❌
   entity.setCreateUser(securityHelper.getCurrentUsername()); // ✅
   ```

4. **Do not expose endpoints without protection**
   ```java
   @RequestMapping("/api/admin")
   // ❌ Missing @PreAuthorize
   ```

5. **Do not log full JWT tokens**
   ```java
   log.info("Token: {}", jwt);      // ❌ Security risk
   log.info("User: {}", username);  // ✅
   ```

---

## Debugging

### Verify a JWT Token

Use **jwt.io** to decode and verify a token:
1. Copy the token from Keycloak.
2. Open https://jwt.io.
3. Paste the token.
4. Verify claims and expiry.

### Spring Security Logging

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

## References

- **Spring Security**: https://spring.io/projects/spring-security
- **OAuth2 Resource Server**: https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html
- **Keycloak**: https://www.keycloak.org/documentation
- **JWT RFC**: https://datatracker.ietf.org/doc/html/rfc7519

---

## Implementation Checklist

- [ ] Spring Security configured
- [ ] OAuth2 Resource Server active
- [ ] JWT validation with Keycloak JWK
- [ ] `CustomJwtAuthenticationToken` for user info
- [ ] `SecurityContextHelper` for simplified access
- [ ] Audit trail with real username
- [ ] Tests compatible with `@WithMockUser`
- [ ] Endpoint protection configured
- [ ] Error handling for 401/403
- [ ] Documentation up to date

---