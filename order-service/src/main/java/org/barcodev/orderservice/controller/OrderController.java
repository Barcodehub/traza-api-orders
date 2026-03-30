package org.barcodev.orderservice.controller;

import lombok.extern.slf4j.Slf4j;
import org.barcodev.orderservice.dto.CreateOrderRequest;
import org.barcodev.orderservice.model.Order;
import org.barcodev.orderservice.service.OrderService;
import java.math.BigDecimal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
@Slf4j
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody CreateOrderRequest request) {
        try {
            log.info("Received order creation request for userId: {}", request.getUserId());
            Order order = orderService.createOrder(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(order);
        } catch (Exception e) {
            log.error("Order creation failed: {}", e.getMessage());
            if (request.getTotal() != null && request.getTotal().compareTo(BigDecimal.ZERO) < 0) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(e.getMessage()));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOrder(@PathVariable String id) {
        try {
            Order order = orderService.getOrder(id);
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(e.getMessage()));
        }
    }

    private record ErrorResponse(String error) {}
}
