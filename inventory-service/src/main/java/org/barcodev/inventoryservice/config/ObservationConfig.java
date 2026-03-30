package org.barcodev.inventoryservice.config;

import io.micrometer.observation.ObservationPredicate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.observation.ServerRequestObservationContext;
import jakarta.servlet.http.HttpServletRequest;

@Configuration
public class ObservationConfig {

    @Bean
    public ObservationPredicate observationPredicate() {
        return (name, context) -> {
            if (context instanceof ServerRequestObservationContext serverContext) {
                HttpServletRequest request = serverContext.getCarrier();
                String uri = request.getRequestURI();
                if (uri != null && uri.startsWith("/actuator")) {
                    return false; // ❌ NO trazar /actuator
                }
            }
            return true; // ✅ trazar todo lo demás
        };
    }
}
