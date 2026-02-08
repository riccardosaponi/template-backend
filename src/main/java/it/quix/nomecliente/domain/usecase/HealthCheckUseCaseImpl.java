package it.quix.nomecliente.domain.usecase;
import java.time.Instant;
import java.util.Map;
import org.springframework.stereotype.Service;
/**
 * Use Case per il health check.
 * Contiene la business logic del controllo dello stato di salute del servizio.
 */
@Service
public class HealthCheckUseCaseImpl implements HealthCheckUseCase {
    /**
     * Esegue un controllo dello stato di salute del servizio.
     *
     * @return mappa con lo stato del servizio e informazioni aggiuntive
     */
    @Override
    public Map<String, Object> check() {
        // Business logic del health check
        return Map.of(
            "status", "UP",
            "timestamp", Instant.now().toString(),
            "service", "nomecliente-backend"
        );
    }
}
