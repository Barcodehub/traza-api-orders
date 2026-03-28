package org.barcodev.orderservice.client;

import org.barcodev.orderservice.dto.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
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

    public PaymentResponse processPayment(PaymentRequest request, String sagaId, String userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Saga-Id", sagaId);
        headers.set("X-User-Id", userId);

        HttpEntity<PaymentRequest> entity = new HttpEntity<>(request, headers);

        return restTemplate.postForObject(
            paymentServiceUrl + "/payments",
            entity,
            PaymentResponse.class
        );
    }

    public void refundPayment(RefundRequest request, String sagaId, String userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Saga-Id", sagaId);
        headers.set("X-User-Id", userId);

        HttpEntity<RefundRequest> entity = new HttpEntity<>(request, headers);

        restTemplate.postForObject(
            paymentServiceUrl + "/payments/refund",
            entity,
            String.class
        );
    }

    public InventoryResponse reserveInventory(InventoryRequest request, String sagaId, String userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Saga-Id", sagaId);
        headers.set("X-User-Id", userId);

        HttpEntity<InventoryRequest> entity = new HttpEntity<>(request, headers);

        return restTemplate.postForObject(
            inventoryServiceUrl + "/inventory/reserve",
            entity,
            InventoryResponse.class
        );
    }

    public void releaseInventory(ReleaseRequest request, String sagaId, String userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Saga-Id", sagaId);
        headers.set("X-User-Id", userId);

        HttpEntity<ReleaseRequest> entity = new HttpEntity<>(request, headers);

        restTemplate.postForObject(
            inventoryServiceUrl + "/inventory/release",
            entity,
            String.class
        );
    }
}
