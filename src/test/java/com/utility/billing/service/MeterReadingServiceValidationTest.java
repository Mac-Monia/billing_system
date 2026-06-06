package com.utility.billing.service;

import com.utility.billing.dto.request.MeterReadingRequest;
import com.utility.billing.entity.Customer;
import com.utility.billing.entity.Meter;
import com.utility.billing.enums.CustomerStatus;
import com.utility.billing.enums.MeterStatus;
import com.utility.billing.enums.MeterType;
import com.utility.billing.exception.BusinessRuleException;
import com.utility.billing.repository.MeterReadingRepository;
import com.utility.billing.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MeterReadingServiceValidationTest {

    @Mock private MeterReadingRepository meterReadingRepository;
    @Mock private MeterService meterService;
    @Mock private UserRepository userRepository;
    @Mock private BillService billService;
    @Mock private AuditService auditService;

    @InjectMocks
    private MeterReadingService meterReadingService;

    private MeterReadingRequest validRequest;
    private Meter activeMeter;

    @BeforeEach
    void setUp() {
        Customer customer = Customer.builder()
                .id(1L)
                .status(CustomerStatus.ACTIVE)
                .build();

        activeMeter = Meter.builder()
                .id(5L)
                .status(MeterStatus.ACTIVE)
                .meterType(MeterType.WATER)
                .installationDate(LocalDate.of(2024, 1, 1))
                .customer(customer)
                .build();

        validRequest = new MeterReadingRequest();
        validRequest.setMeterId(5L);
        validRequest.setPreviousReading(BigDecimal.valueOf(100));
        validRequest.setCurrentReading(BigDecimal.valueOf(150));
        validRequest.setReadingDate(LocalDate.of(2024, 6, 1));
    }

    @Test
    void capture_inactiveCustomer_throws() {
        activeMeter.setCustomer(Customer.builder().id(1L).status(CustomerStatus.INACTIVE).build());
        when(meterService.findById(5L)).thenReturn(activeMeter);

        assertThrows(BusinessRuleException.class, () -> meterReadingService.capture(validRequest));
    }

    @Test
    void capture_inactiveMeter_throws() {
        activeMeter.setStatus(MeterStatus.INACTIVE);
        when(meterService.findById(5L)).thenReturn(activeMeter);

        assertThrows(BusinessRuleException.class, () -> meterReadingService.capture(validRequest));
    }

    @Test
    void capture_readingBeforeInstallation_throws() {
        validRequest.setReadingDate(LocalDate.of(2023, 12, 1));
        when(meterService.findById(5L)).thenReturn(activeMeter);

        assertThrows(BusinessRuleException.class, () -> meterReadingService.capture(validRequest));
    }

    @Test
    void capture_currentNotGreaterThanPrevious_throws() {
        validRequest.setCurrentReading(BigDecimal.valueOf(100));
        when(meterService.findById(5L)).thenReturn(activeMeter);

        assertThrows(BusinessRuleException.class, () -> meterReadingService.capture(validRequest));
    }

    @Test
    void capture_duplicateMonthYear_throws() {
        when(meterService.findById(5L)).thenReturn(activeMeter);
        when(meterReadingRepository.existsByMeterIdAndBillingMonthAndBillingYear(5L, 6, 2024))
                .thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> meterReadingService.capture(validRequest));
    }

    @Test
    void capture_futureReadingDate_throws() {
        validRequest.setReadingDate(LocalDate.now().plusDays(1));
        when(meterService.findById(5L)).thenReturn(activeMeter);

        assertThrows(BusinessRuleException.class, () -> meterReadingService.capture(validRequest));
    }

    @Test
    void capture_disconnectedMeter_throws() {
        activeMeter.setStatus(MeterStatus.DISCONNECTED);
        when(meterService.findById(5L)).thenReturn(activeMeter);

        assertThrows(BusinessRuleException.class, () -> meterReadingService.capture(validRequest));
    }
}
