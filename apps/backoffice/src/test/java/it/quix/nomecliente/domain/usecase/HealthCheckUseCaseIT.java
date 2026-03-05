package it.quix.nomecliente.domain.usecase;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for HealthCheckUseCase.
 * Tests basic application health check and service status reporting.
 */
class HealthCheckUseCaseIT extends BaseUseCaseIT {

    @Autowired
    private HealthCheckUseCase healthCheckUseCase;

    @Test
    void check_should_returnUpStatus() {
        // When
        Map<String, Object> result = healthCheckUseCase.check();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).containsEntry("status", "UP");
        assertThat(result).containsKey("timestamp");
        assertThat(result).containsEntry("service", "nomecliente-backend");
    }
}
