package com.utility.billing.service;

import com.utility.billing.dto.request.PenaltyConfigurationRequest;
import com.utility.billing.entity.PenaltyConfiguration;
import com.utility.billing.exception.BusinessRuleException;
import com.utility.billing.repository.PenaltyConfigurationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PenaltyServiceValidationTest {

    @Mock private PenaltyConfigurationRepository penaltyRepository;

    @InjectMocks
    private PenaltyService penaltyService;

    private PenaltyConfigurationRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new PenaltyConfigurationRequest();
        validRequest.setName("Standard Late Fee");
        validRequest.setRatePercent(new BigDecimal("5"));
        validRequest.setGraceDays(7);
        validRequest.setEffectiveFrom(LocalDate.now().plusMonths(1));
    }

    @Test
    void create_effectiveDateBeforeCurrentPenalty_throws() {
        PenaltyConfiguration current = PenaltyConfiguration.builder()
                .effectiveFrom(LocalDate.now().plusMonths(2))
                .build();
        validRequest.setEffectiveFrom(LocalDate.now().plusMonths(1));

        when(penaltyRepository.findOpenActivePenalty()).thenReturn(Optional.of(current));

        assertThrows(BusinessRuleException.class, () -> penaltyService.create(validRequest));
        verify(penaltyRepository, never()).save(any());
    }

    @Test
    void create_pastEffectiveDate_throws() {
        validRequest.setEffectiveFrom(LocalDate.now().minusDays(1));

        assertThrows(BusinessRuleException.class, () -> penaltyService.create(validRequest));
    }
}
