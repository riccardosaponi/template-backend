package it.quix.nomecliente.domain.usecase;
import java.util.Map;
public interface HealthCheckUseCase {
    /**
     * Esegue un controllo dello stato di salute del servizio.
     *
     * @return mappa con lo stato del servizio e informazioni aggiuntive
     */
    Map<String, Object> check();
}
