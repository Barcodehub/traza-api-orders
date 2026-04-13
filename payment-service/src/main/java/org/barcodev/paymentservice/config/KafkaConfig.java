package org.barcodev.paymentservice.config;

import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;
import org.slf4j.MDC;
import java.nio.charset.StandardCharsets;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;

@Configuration
public class KafkaConfig {

    @Bean
    public DefaultErrorHandler errorHandler(KafkaTemplate<Object, Object> template) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(template,
            (record, ex) -> {
                Span currentSpan = Span.current();
                if (currentSpan != null) {
                    currentSpan.recordException(ex);
                    currentSpan.setStatus(StatusCode.ERROR, ex.getMessage());
                }
                return new TopicPartition(
                    record.topic() + ".DLT",
                    record.partition()
                );
            }
        );

        FixedBackOff backoff = new FixedBackOff(1000L, 3); // 3 reintentos, 1s entre cada uno
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backoff);

        errorHandler.setRetryListeners((record, ex, deliveryAttempt) -> {
            var correlationHeader = record.headers().lastHeader("X-Correlation-Id");
            var userHeader = record.headers().lastHeader("X-User-Id");

            if (correlationHeader != null) {
                MDC.put("correlationId", new String(correlationHeader.value(), StandardCharsets.UTF_8));
                MDC.put("eventId", new String(correlationHeader.value(), StandardCharsets.UTF_8));
            }
            if (userHeader != null) {
                MDC.put("userId", new String(userHeader.value(), StandardCharsets.UTF_8));
            }
        });

        return errorHandler;
    }
}
