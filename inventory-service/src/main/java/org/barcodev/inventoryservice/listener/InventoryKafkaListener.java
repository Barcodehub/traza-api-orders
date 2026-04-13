package org.barcodev.inventoryservice.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import org.barcodev.inventoryservice.dto.OrderEvent;

@Component
public class InventoryKafkaListener {

    private static final Logger log = LoggerFactory.getLogger(InventoryKafkaListener.class);

    @KafkaListener(topics = "order-events-topic", groupId = "inventory-group")
    public void listen(
            @Payload OrderEvent payload,
            @Header(KafkaHeaders.RECEIVED_KEY) String key) {

        log.info("Received Kafka event in Inventory service for userId: {}. Payload: {}", key, payload);

        //simular el fallo en un endpoint de kafka
        if ("FAIL_ON_PURPOSE".equals(payload.getStatus())) {
            log.error("Simulated processing failure in Inventory service for event: {}", payload.getEventId());
            throw new RuntimeException("Simulated error to trigger Kafka processing failure in Inventory service");
        }

        // Simulating processing logic...
        log.info("Checking inventory for user {}", key);
    }
}
