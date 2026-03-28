package org.barcodev.inventoryservice.controller;

import lombok.extern.slf4j.Slf4j;
import org.barcodev.inventoryservice.service.InventoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/test")
@Slf4j
public class TestController {

    private final InventoryService inventoryService;

    public TestController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping("/failure")
    public ResponseEntity<String> toggleFailure(@RequestParam(defaultValue = "true") boolean enable) {
        inventoryService.setForcedFailure(enable);
        String message = enable ? "Inventory service will FAIL on next request" : "Inventory service back to normal (20% random failures)";
        log.info(message);
        return ResponseEntity.ok(message);
    }
}
