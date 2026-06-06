package com.utility.billing.service;

import com.utility.billing.dto.request.TaxConfigurationRequest;
import com.utility.billing.entity.TaxConfiguration;
import com.utility.billing.exception.BusinessRuleException;
import com.utility.billing.repository.TaxConfigurationRepository;
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
class TaxServiceValidationTest {

    @Mock private TaxConfigurationRepository taxRepository;

    @InjectMocks
    private TaxService taxService;

    private TaxConfigurationRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new TaxConfigurationRequest();
        validRequest.setName("Environmental Levy");
        validRequest.setRate(new BigDecimal("2.5"));
        validRequest.setEffectiveFrom(LocalDate.now().plusMonths(1));
    }

    @Test
    void create_effectiveDateBeforeCurrentTax_throws() {
        TaxConfiguration current = TaxConfiguration.builder()
                .name("Environmental Levy")
                .effectiveFrom(LocalDate.now().plusMonths(2))
                .build();
        validRequest.setEffectiveFrom(LocalDate.now().plusMonths(1));

        when(taxRepository.findOpenActiveByName("Environmental Levy")).thenReturn(Optional.of(current));

        assertThrows(BusinessRuleException.class, () -> taxService.create(validRequest));
        verify(taxRepository, never()).save(any());
    }

    @Test
    void create_pastEffectiveDate_throws() {
        validRequest.setEffectiveFrom(LocalDate.now().minusDays(1));

        assertThrows(BusinessRuleException.class, () -> taxService.create(validRequest));
    }
}
