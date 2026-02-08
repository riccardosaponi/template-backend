package it.quix.nomecliente.domain.usecase;

import it.quix.nomecliente.BackendApplication;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base class for UseCase integration tests.
 * Provides:
 * - Shared PostgreSQL container via Testcontainers (singleton pattern)
 * - Full Spring Boot application context
 * - Transaction management with automatic rollback (each test runs in an isolated transaction)
 * - Liquibase schema migrations
 * - Mock authenticated user for security context
 */
@SpringBootTest(classes = BackendApplication.class)
@Transactional
@WithMockUser(username = "test-user", roles = {"USER"})
public abstract class BaseUseCaseIT {

    // Singleton container shared across all test classes
    private static final PostgreSQLContainer<?> postgres;

    static {
        postgres = new PostgreSQLContainer<>("postgres:16-alpine")
                .withDatabaseName("testdb")
                .withUsername("test")
                .withPassword("test")
                .withReuse(true);
        postgres.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @BeforeEach
    void setUp() {
        // Common setup for all UseCase integration tests
        // Database is automatically configured via @DynamicPropertySource
        // Liquibase migrations are applied automatically by Spring Boot
    }
}
