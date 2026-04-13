package org.barcodev.orderservice.controller;

import org.barcodev.orderservice.dto.CreateOrderRequest;
import org.barcodev.orderservice.dto.OrderEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;

import io.opentelemetry.api.trace.Span;

import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.producer.ProducerRecord;

@RestController
@RequestMapping("/api/kafka")
public class KafkaOrderController {

    private static final Logger log = LoggerFactory.getLogger(KafkaOrderController.class);
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaOrderController(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @PostMapping("/send")
    public ResponseEntity<String> sendOrderEvent(@RequestBody CreateOrderRequest request) {
        String eventId = UUID.randomUUID().toString();

        OrderEvent event = new OrderEvent(eventId, request.getUserId(), request.getTotal(), "CREATED");

        MDC.put("userId", request.getUserId());
        MDC.put("eventId", eventId);

        try {
            log.info("Sending order event to Kafka for userId: {}", request.getUserId());

            ProducerRecord<String, Object> record = new ProducerRecord<>(
                "order-events-topic", null, request.getUserId(), event
            );

            // Capture MDC before send: whenComplete runs on Kafka IO thread (no MDC by default)
            Map<String, String> mdcCopy = MDC.getCopyOfContextMap();

            kafkaTemplate.send(record).whenComplete((result, ex) -> {
                if (mdcCopy != null) MDC.setContextMap(mdcCopy);
                try {
                    if (ex != null) {
                        log.error("Failed to send event {}", eventId, ex);
                    } else {
                        log.info("Event {} sent to partition {} offset {}",
                            eventId,
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                    }
                } finally {
                    MDC.clear();
                }
            });

            return ResponseEntity.ok("Order event sent to Kafka with ID: " + eventId);
        } finally {
            MDC.clear();
        }
    }

    //simular el fallo in endpoint kafka
    @PostMapping("/send-fail")
    public ResponseEntity<String> sendFailOrderEvent(@RequestBody CreateOrderRequest request) {
        String eventId = UUID.randomUUID().toString();

        OrderEvent event = new OrderEvent(eventId, request.getUserId(), request.getTotal(), "FAIL_ON_PURPOSE");

        MDC.put("userId", request.getUserId());
        MDC.put("eventId", eventId);

        try {
            log.info("Sending intentionally failing order event to Kafka for userId: {}", request.getUserId());

            ProducerRecord<String, Object> record = new ProducerRecord<>(
                "order-events-topic", null, request.getUserId(), event
            );

            Map<String, String> mdcCopy = MDC.getCopyOfContextMap();

            kafkaTemplate.send(record).whenComplete((result, ex) -> {
                if (mdcCopy != null) MDC.setContextMap(mdcCopy);
                try {
                    if (ex != null) {
                        log.error("Failed to send failing event {}", eventId, ex);
                    } else {
                        log.info("Event {} sent to partition {} offset {}",
                            eventId,
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                    }
                } finally {
                    MDC.clear();
                }
            });

            return ResponseEntity.ok("Intentionally failing order event sent to Kafka with ID: " + eventId);
        } finally {
            MDC.clear();
        }
    }
}
