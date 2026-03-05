package it.quix.nomecliente.domain.port.in;

import java.util.Map;

public interface HealthCheckIn {

    Map<String, Object> check();
}
