package org.barcodev.orderservice.client;

import org.barcodev.orderservice.dto.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class ServiceClient {

    private final RestTemplate restTemplate;

    @Value("${services.payment.url}")
    private String paymentServiceUrl;

    @Value("${services.inventory.url}")
    private String inventoryServiceUrl;

    public ServiceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public PaymentResponse processPayment(PaymentRequest request) {
        return restTemplate.postForObject(
            paymentServiceUrl + "/payments",
            request,
            PaymentResponse.class
        );
    }

    public void refundPayment(RefundRequest request) {
        restTemplate.postForObject(
            paymentServiceUrl + "/payments/refund",
            request,
            String.class
        );
    }

    public InventoryResponse reserveInventory(InventoryRequest request) {
        return restTemplate.postForObject(
            inventoryServiceUrl + "/inventory/reserve",
            request,
            InventoryResponse.class
        );
    }

    public void releaseInventory(ReleaseRequest request) {
        restTemplate.postForObject(
            inventoryServiceUrl + "/inventory/release",
            request,
            String.class
        );
    }
}
