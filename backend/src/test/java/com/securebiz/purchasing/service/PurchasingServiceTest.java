package com.securebiz.purchasing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.securebiz.audit.repository.ActivityLogRepository;
import com.securebiz.inventory.repository.InventoryRepository;
import com.securebiz.purchasing.api.PurchasingDtos.PurchaseItemRequest;
import com.securebiz.purchasing.api.PurchasingDtos.PurchaseOrderRequest;
import com.securebiz.purchasing.repository.PurchasingRepository;
import com.securebiz.shared.api.BusinessException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class PurchasingServiceTest {
    @Test
    void duplicateVariantLinesAreRejected() {
        PurchasingService service = new PurchasingService(mock(PurchasingRepository.class),
                mock(InventoryRepository.class), mock(ActivityLogRepository.class), new ObjectMapper());
        UUID variant = UUID.randomUUID();
        PurchaseOrderRequest request = new PurchaseOrderRequest(UUID.randomUUID(), UUID.randomUUID(), "VND",
                List.of(new PurchaseItemRequest(variant, BigDecimal.ONE, BigDecimal.TEN),
                        new PurchaseItemRequest(variant, BigDecimal.TWO, BigDecimal.TEN)));
        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("only once");
    }
}
