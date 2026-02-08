package it.quix.nomecliente.application;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.quix.nomecliente.domain.port.in.HealthCheckIn;
import it.quix.nomecliente.domain.usecase.HealthCheckUseCase;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Health Check REST endpoint.
 *
 * <p>This is a driving adapter (REST). It exposes an HTTP endpoint and delegates the business logic to
 * the {@link HealthCheckUseCase}.</p>
 */
@Tag(
        name = "System",
        description = "System endpoints (health checks / smoke tests)"
)
@RestController
@RequestMapping(value = "/api", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class HealthCheckRestAdapter implements HealthCheckIn {

    private final HealthCheckUseCase healthCheckUseCase;

    @Override
    @Operation(
            operationId = "healthCheck",
            summary = "Health check endpoint",
            description = "Returns service health status with timestamp and service information. Useful for monitoring and load balancers."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Service is healthy and operational",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Map.class),
                            examples = @ExampleObject(
                                    name = "healthy",
                                    summary = "Healthy response",
                                    value = "{\"status\":\"UP\",\"timestamp\":\"2024-02-08T10:15:30.123Z\",\"service\":\"nomecliente-backend\"}"
                            )
                    )
            )
    })
    @GetMapping("/health")
    public Map<String, Object> check() {
        return healthCheckUseCase.check();
    }
}
