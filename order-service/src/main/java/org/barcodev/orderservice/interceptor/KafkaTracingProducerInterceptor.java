package org.barcodev.orderservice.interceptor;

import io.opentelemetry.api.trace.Span;
import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.MDC;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public class KafkaTracingProducerInterceptor implements ProducerInterceptor<String, Object> {

    @Override
    public ProducerRecord<String, Object> onSend(ProducerRecord<String, Object> record) {

        String eventId = MDC.get("eventId");
        String userId = MDC.get("userId");

        if (eventId != null) {
            record.headers().add("X-Correlation-Id", eventId.getBytes(StandardCharsets.UTF_8));
        }

        if (userId != null) {
            record.headers().add("X-User-Id", userId.getBytes(StandardCharsets.UTF_8));
        }

        Span currentSpan = Span.current();
        if (currentSpan != null) {
            currentSpan.setAttribute("userId", userId != null ? userId : "unknown");
            currentSpan.setAttribute("eventId", eventId != null ? eventId : "unknown");
        }

        return record;
    }

    @Override public void onAcknowledgement(RecordMetadata metadata, Exception exception) {}
    @Override public void close() {}
    @Override
    public void configure(Map<String, ?> configs) {

    }

}