package com.securebiz.inventory.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.securebiz.audit.repository.ActivityLogRepository;
import com.securebiz.inventory.api.InventoryDtos.AdjustmentRequest;
import com.securebiz.inventory.repository.InventoryRepository;
import com.securebiz.shared.api.BusinessException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class InventoryServiceTest {
    @Test
    void zeroQuantityAdjustmentIsRejectedBeforeMutation() {
        InventoryService service = new InventoryService(mock(InventoryRepository.class),
                mock(ActivityLogRepository.class), new ObjectMapper());
        AdjustmentRequest request = new AdjustmentRequest(UUID.randomUUID(), UUID.randomUUID(),
                BigDecimal.ZERO, "Cycle count correction");
        assertThatThrownBy(() -> service.adjust(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("zero");
    }
}
