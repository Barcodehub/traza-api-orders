package org.barcodev.paymentservice.controller;

import lombok.extern.slf4j.Slf4j;
import org.barcodev.paymentservice.dto.PaymentRequest;
import org.barcodev.paymentservice.dto.PaymentResponse;
import org.barcodev.paymentservice.dto.RefundRequest;
import org.barcodev.paymentservice.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments")
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> processPayment(
            @RequestBody PaymentRequest request,
            @RequestHeader(value = "X-Saga-Id", required = false) String sagaId,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {

        log.info("Received payment request for orderId: {}", request.getOrderId());
        PaymentResponse response = paymentService.processPayment(request, sagaId, userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refund")
    public ResponseEntity<String> refundPayment(
            @RequestBody RefundRequest request,
            @RequestHeader(value = "X-Saga-Id", required = false) String sagaId,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {

        log.info("Received refund request for paymentId: {}", request.getPaymentId());
        paymentService.refundPayment(request, sagaId, userId);
        return ResponseEntity.ok("Refund processed");
    }
}
