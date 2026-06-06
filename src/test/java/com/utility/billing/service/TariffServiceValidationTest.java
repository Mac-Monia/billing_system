package com.utility.billing.service;

import com.utility.billing.dto.request.TariffRequest;
import com.utility.billing.entity.Tariff;
import com.utility.billing.enums.MeterType;
import com.utility.billing.enums.TariffType;
import com.utility.billing.exception.BusinessRuleException;
import com.utility.billing.repository.TariffRepository;
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
class TariffServiceValidationTest {

    @Mock private TariffRepository tariffRepository;
    @Mock private AuditService auditService;

    @InjectMocks
    private TariffService tariffService;

    private TariffRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new TariffRequest();
        validRequest.setMeterType(MeterType.WATER);
        validRequest.setTariffType(TariffType.FLAT);
        validRequest.setRatePerUnit(new BigDecimal("250"));
        validRequest.setFixedCharge(new BigDecimal("1500"));
        validRequest.setVatPercent(new BigDecimal("18"));
        validRequest.setLatePenaltyPercent(new BigDecimal("5"));
        validRequest.setEffectiveFrom(LocalDate.now().plusMonths(1));
    }

    @Test
    void create_effectiveDateBeforeCurrentTariff_throws() {
        Tariff current = Tariff.builder()
                .version(1)
                .meterType(MeterType.WATER)
                .effectiveFrom(LocalDate.now().plusMonths(2))
                .build();
        validRequest.setEffectiveFrom(LocalDate.now().plusMonths(1));

        when(tariffRepository.findTopByMeterTypeOrderByVersionDesc(MeterType.WATER))
                .thenReturn(Optional.of(current));

        assertThrows(BusinessRuleException.class, () -> tariffService.create(validRequest));
        verify(tariffRepository, never()).save(any());
    }

    @Test
    void create_flatWithoutRate_throws() {
        validRequest.setRatePerUnit(null);

        assertThrows(BusinessRuleException.class, () -> tariffService.create(validRequest));
    }

    @Test
    void update_publishedTariff_throws() {
        Tariff published = Tariff.builder()
                .id(1L)
                .version(1)
                .effectiveFrom(LocalDate.now().minusDays(1))
                .tariffType(TariffType.FLAT)
                .build();
        when(tariffRepository.findByIdWithTiers(1L)).thenReturn(Optional.of(published));

        assertThrows(BusinessRuleException.class, () -> tariffService.update(1L, validRequest));
    }
}
