package org.barcodev.paymentservice.service;

import lombok.extern.slf4j.Slf4j;
import org.barcodev.paymentservice.dto.PaymentRequest;
import org.barcodev.paymentservice.dto.PaymentResponse;
import org.barcodev.paymentservice.dto.RefundRequest;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
@Slf4j
public class PaymentService {

    private boolean forcedFailure = false;

    public PaymentResponse processPayment(PaymentRequest request) {
        String sagaId = MDC.get("sagaId");
        String userId = MDC.get("userId");

        log.info("[SAGA:{}] [X-User-Id:{}] Processing payment for orderId: {}, amount: {}",
                sagaId, userId, request.getOrderId(), request.getAmount());

        // Simulate random failure (30% probability)
        if (forcedFailure || shouldSimulateFailure(30)) {
            log.warn("[SAGA:{}] Payment FAILED (simulated)", sagaId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient funds or payment gateway error");
        }

        String paymentId = "PAY-" + UUID.randomUUID().toString();
        log.info("[SAGA:{}] Payment SUCCESS, paymentId: {}", sagaId, paymentId);

        return new PaymentResponse(paymentId, "SUCCESS", "Payment processed successfully");
    }

    public void refundPayment(RefundRequest request) {
        String sagaId = MDC.get("sagaId");
        String userId = MDC.get("userId");

        log.info("[SAGA:{}] [X-User-Id:{}] Refunding payment: {}", sagaId, userId, request.getPaymentId());

        // Simulate refund processing
        log.info("[SAGA:{}] Refund completed for paymentId: {}", sagaId, request.getPaymentId());
    }

    public void setForcedFailure(boolean forcedFailure) {
        this.forcedFailure = forcedFailure;
        log.info("Forced failure mode: {}", forcedFailure);
    }

    private boolean shouldSimulateFailure(int percentage) {
        return ThreadLocalRandom.current().nextInt(100) < percentage;
    }
}