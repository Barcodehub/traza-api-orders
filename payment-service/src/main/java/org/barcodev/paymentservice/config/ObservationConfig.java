package org.barcodev.paymentservice.config;
import io.micrometer.observation.ObservationPredicate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.observation.ServerRequestObservationContext;
import jakarta.servlet.http.HttpServletRequest;

import io.micrometer.observation.ObservationFilter;
import io.micrometer.common.KeyValue;
import org.springframework.kafka.support.micrometer.KafkaRecordReceiverContext;
import java.nio.charset.StandardCharsets;

@Configuration
public class ObservationConfig {

    @Bean
    public ObservationFilter kafkaObservationFilter() {
        return context -> {
            if (context instanceof KafkaRecordReceiverContext kafkaContext) {
                var record = kafkaContext.getRecord();
                var correlationHeader = record.headers().lastHeader("X-Correlation-Id");
                var userHeader = record.headers().lastHeader("X-User-Id");

                if (correlationHeader != null) {
                    context.addHighCardinalityKeyValue(KeyValue.of("eventId", new String(correlationHeader.value(), StandardCharsets.UTF_8)));
                }
                if (userHeader != null) {
                    context.addHighCardinalityKeyValue(KeyValue.of("userId", new String(userHeader.value(), StandardCharsets.UTF_8)));
                }
            }
            return context;
        };
    }

    @Bean
    public ObservationPredicate observationPredicate() {
        return (name, context) -> {
            if (context instanceof ServerRequestObservationContext serverContext) {
                HttpServletRequest request = serverContext.getCarrier();
                String uri = request.getRequestURI();
                if (uri != null && uri.startsWith("/actuator")) {
                    return false; // NO trazar
                }
            }
            return true; // Trazar el resto
        };
    }
}
