package org.barcodev.inventoryservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderEvent {
    private String eventId;
    private String userId;
    private BigDecimal total;
    private String status;
}
