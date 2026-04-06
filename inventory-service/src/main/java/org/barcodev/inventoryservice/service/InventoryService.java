package org.barcodev.inventoryservice.service;

import lombok.extern.slf4j.Slf4j;
import org.barcodev.inventoryservice.dto.InventoryRequest;
import org.barcodev.inventoryservice.dto.InventoryResponse;
import org.barcodev.inventoryservice.dto.ReleaseRequest;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.MDC;
import io.micrometer.tracing.Tracer;

@Service
@Slf4j
public class InventoryService {

    private boolean forcedFailure = false;
    private final Tracer tracer;

    public InventoryService(Tracer tracer) {
        this.tracer = tracer;
    }

    public InventoryResponse reserveInventory(InventoryRequest request) {
        String sagaId = MDC.get("sagaId");
        String userId = MDC.get("userId");
        
        log.info("[SAGA:{}] [X-User-Id:{}] Reserving inventory for orderId: {}, productId: {}, quantity: {}",
            sagaId, userId, request.getOrderId(), request.getProductId(), request.getQuantity());

        // Simulate random failure (20% probability)
        if (forcedFailure || shouldSimulateFailure(20)) {
            log.warn("[SAGA:{}] Inventory reservation FAILED (simulated)", sagaId);
            if (tracer.currentSpan() != null) {
                tracer.currentSpan().error(new RuntimeException("Simulated Inventory Failure"));
            }
            return new InventoryResponse(null, "FAILED", "Out of stock or inventory service error");
        }

        String reservationId = "RES-" + UUID.randomUUID().toString();
        log.info("[SAGA:{}] Inventory reservation SUCCESS, reservationId: {}", sagaId, reservationId);

        return new InventoryResponse(reservationId, "SUCCESS", "Inventory reserved successfully");
    }

    public void releaseInventory(ReleaseRequest request) {
        String sagaId = MDC.get("sagaId");
        String userId = MDC.get("userId");
        
        log.info("[SAGA:{}] [X-User-Id:{}] Releasing inventory reservation: {}", sagaId, userId, request.getReservationId());

        // Simulate release processing
        log.info("[SAGA:{}] Inventory released for reservationId: {}", sagaId, request.getReservationId());
    }

    public void setForcedFailure(boolean forcedFailure) {
        this.forcedFailure = forcedFailure;
        log.info("Forced failure mode: {}", forcedFailure);
    }

    private boolean shouldSimulateFailure(int percentage) {
        return ThreadLocalRandom.current().nextInt(100) < percentage;
    }
}
