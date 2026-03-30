package org.barcodev.orderservice.service;

import lombok.extern.slf4j.Slf4j;
import org.barcodev.orderservice.client.ServiceClient;
import org.barcodev.orderservice.dto.*;
import org.barcodev.orderservice.model.Order;
import org.barcodev.orderservice.model.OrderStatus;
import org.barcodev.orderservice.repository.OrderRepository;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;
import java.util.UUID;
import java.math.BigDecimal;
import io.micrometer.tracing.Tracer;

@Service
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final ServiceClient serviceClient;
    private final Counter ordersCreatedCounter;
    private final Counter sagaFailedCounter;
    private final Tracer tracer;

    public OrderService(OrderRepository orderRepository, ServiceClient serviceClient, MeterRegistry meterRegistry, Tracer tracer) {
        this.orderRepository = orderRepository;
        this.serviceClient = serviceClient;
        this.ordersCreatedCounter = meterRegistry.counter("orders.created.total", "type", "order_creation");
        this.sagaFailedCounter = meterRegistry.counter("saga.failed.total", "type", "saga_processing");
        this.tracer = tracer;
    }

    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        String sagaId = UUID.randomUUID().toString();
        String orderId = UUID.randomUUID().toString();

        MDC.put("sagaId", sagaId);
        MDC.put("userId", request.getUserId());

        if (tracer.currentSpan() != null) {
            tracer.currentSpan().tag("sagaId", sagaId);
            tracer.currentSpan().tag("userId", request.getUserId());
        }

        try {
            log.info("[SAGA:{}] Starting order creation for orderId: {}", sagaId, orderId);

            // Step 1: Create order in PENDING status
            Order order = new Order(orderId, request.getUserId(), OrderStatus.PENDING, request.getTotal(), sagaId);
            order = orderRepository.save(order);
            log.info("[SAGA:{}] Order created with status PENDING", sagaId);

            String paymentId = null;
            String reservationId = null;

            try {
                // Step 2: Process payment
                log.info("[SAGA:{}] Calling payment-service", sagaId);
                PaymentRequest paymentRequest = new PaymentRequest(orderId, request.getUserId(), request.getTotal());
                PaymentResponse paymentResponse = serviceClient.processPayment(paymentRequest);

                if (!"SUCCESS".equals(paymentResponse.getStatus())) {
                    throw new RuntimeException("Payment failed: " + paymentResponse.getMessage());
                }

                paymentId = paymentResponse.getPaymentId();
                log.info("[SAGA:{}] Payment successful, paymentId: {}", sagaId, paymentId);

                // Step 3: Reserve inventory
                log.info("[SAGA:{}] Calling inventory-service", sagaId);
                InventoryRequest inventoryRequest = new InventoryRequest(orderId, "PRODUCT-001", 1);
                InventoryResponse inventoryResponse = serviceClient.reserveInventory(inventoryRequest);

                if (!"SUCCESS".equals(inventoryResponse.getStatus())) {
                    throw new RuntimeException("Inventory reservation failed: " + inventoryResponse.getMessage());
                }

                reservationId = inventoryResponse.getReservationId();
                log.info("[SAGA:{}] Inventory reserved, reservationId: {}", sagaId, reservationId);

                // Step 4: Confirm order
                order.setStatus(OrderStatus.CONFIRMED);
                order = orderRepository.save(order);
                log.info("[SAGA:{}] Order CONFIRMED successfully", sagaId);
                
                // Simular un fallo HTTP 500 en Jaeger si el total es negativo y forzar la compensación
                if (request.getTotal() != null && request.getTotal().compareTo(BigDecimal.ZERO) < 0) {
                    throw new RuntimeException("Simulated error to trigger 500 tracing in Jaeger");
                }

                // Increment custom metric for successful orders
                ordersCreatedCounter.increment();

                return order;

            } catch (Exception e) {
                log.error("[SAGA:{}] Error in saga: {}", sagaId, e.getMessage());
                
                // Increment custom metric for failed sagas
                sagaFailedCounter.increment();

                // COMPENSATIONS
                if (reservationId != null) {
                    try {
                        log.info("[SAGA:{}] Compensating: releasing inventory", sagaId);
                        ReleaseRequest releaseRequest = new ReleaseRequest(reservationId, orderId);
                        serviceClient.releaseInventory(releaseRequest);
                        log.info("[SAGA:{}] Inventory released", sagaId);
                    } catch (Exception ex) {
                        log.error("[SAGA:{}] Failed to release inventory: {}", sagaId, ex.getMessage());
                    }
                }

                if (paymentId != null) {
                    try {
                        log.info("[SAGA:{}] Compensating: refunding payment", sagaId);
                        RefundRequest refundRequest = new RefundRequest(paymentId, orderId);
                        serviceClient.refundPayment(refundRequest);
                        log.info("[SAGA:{}] Payment refunded", sagaId);
                    } catch (Exception ex) {
                        log.error("[SAGA:{}] Failed to refund payment: {}", sagaId, ex.getMessage());
                    }
                }

                // Cancel order
                order.setStatus(OrderStatus.CANCELLED);
                orderRepository.save(order);
                log.info("[SAGA:{}] Order CANCELLED", sagaId);

                if (tracer.currentSpan() != null) {
                    tracer.currentSpan().tag("error", "true");
                    tracer.currentSpan().event("SAGA_FAILED: " + e.getMessage());
                }

                throw new RuntimeException("Order creation failed: " + e.getMessage());
            }
        } finally {
            MDC.clear();
        }
    }

    public Order getOrder(String orderId) {
        return orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
    }
}
