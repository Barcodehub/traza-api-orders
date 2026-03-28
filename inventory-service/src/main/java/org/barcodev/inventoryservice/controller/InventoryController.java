package org.barcodev.inventoryservice.controller;

import lombok.extern.slf4j.Slf4j;
import org.barcodev.inventoryservice.dto.InventoryRequest;
import org.barcodev.inventoryservice.dto.InventoryResponse;
import org.barcodev.inventoryservice.dto.ReleaseRequest;
import org.barcodev.inventoryservice.service.InventoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/inventory")
@Slf4j
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping("/reserve")
    public ResponseEntity<InventoryResponse> reserveInventory(@RequestBody InventoryRequest request) {

        log.info("Received inventory reservation request for orderId: {}", request.getOrderId());
        InventoryResponse response = inventoryService.reserveInventory(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/release")
    public ResponseEntity<String> releaseInventory(@RequestBody ReleaseRequest request) {

        log.info("Received inventory release request for reservationId: {}", request.getReservationId());
        inventoryService.releaseInventory(request);
        return ResponseEntity.ok("Inventory released");
    }
}
