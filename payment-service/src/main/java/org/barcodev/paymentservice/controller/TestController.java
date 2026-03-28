package org.barcodev.paymentservice.controller;

import lombok.extern.slf4j.Slf4j;
import org.barcodev.paymentservice.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/test")
@Slf4j
public class TestController {

    private final PaymentService paymentService;

    public TestController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/failure")
    public ResponseEntity<String> toggleFailure(@RequestParam(defaultValue = "true") boolean enable) {
        paymentService.setForcedFailure(enable);
        String message = enable ? "Payment service will FAIL on next request" : "Payment service back to normal (30% random failures)";
        log.info(message);
        return ResponseEntity.ok(message);
    }
}
