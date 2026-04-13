package org.barcodev.paymentservice.filter;

import io.opentelemetry.api.trace.Span;
import org.apache.kafka.clients.consumer.Consumer;
import org.slf4j.MDC;
import org.springframework.kafka.listener.RecordInterceptor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class KafkaMdcInterceptor implements RecordInterceptor<String, Object> {

    @Nullable
    @Override
    public ConsumerRecord<String, Object> intercept(ConsumerRecord<String, Object> record, Consumer<String, Object> consumer) {
        var headers = record.headers();

        var correlationHeader = headers.lastHeader("X-Correlation-Id");
        var userHeader = headers.lastHeader("X-User-Id");

        if (correlationHeader != null) {
            MDC.put("correlationId", new String(correlationHeader.value(), StandardCharsets.UTF_8));
        }

        if (userHeader != null) {
            MDC.put("userId", new String(userHeader.value(), StandardCharsets.UTF_8));
        }

        Span currentSpan = Span.current();
        if (currentSpan != null) {
            currentSpan.setAttribute("correlationId", correlationHeader != null ? new String(correlationHeader.value(), StandardCharsets.UTF_8) : "unknown");
            currentSpan.setAttribute("userId", userHeader != null ? new String(userHeader.value(), StandardCharsets.UTF_8) : "unknown");
            currentSpan.setAttribute("eventId", correlationHeader != null ? new String(correlationHeader.value(), StandardCharsets.UTF_8) : "unknown");
        }

        return record;
    }
    @Override
    public void afterRecord(ConsumerRecord<String, Object> record, Consumer<String, Object> consumer) {
        MDC.clear();
    }
}