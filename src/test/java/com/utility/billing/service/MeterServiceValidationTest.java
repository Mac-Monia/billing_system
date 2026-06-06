package com.utility.billing.service;

import com.utility.billing.dto.request.MeterRequest;
import com.utility.billing.entity.Customer;
import com.utility.billing.entity.Meter;
import com.utility.billing.enums.CustomerStatus;
import com.utility.billing.enums.MeterType;
import com.utility.billing.exception.BusinessRuleException;
import com.utility.billing.exception.DuplicateResourceException;
import com.utility.billing.repository.BillRepository;
import com.utility.billing.repository.MeterReadingRepository;
import com.utility.billing.repository.MeterRepository;
import com.utility.billing.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MeterServiceValidationTest {

    @Mock private MeterRepository meterRepository;
    @Mock private BillRepository billRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private MeterReadingRepository meterReadingRepository;
    @Mock private CustomerService customerService;
    @Mock private DuplicateCheckService duplicateCheckService;
    @Mock private AuditService auditService;

    @InjectMocks
    private MeterService meterService;

    private MeterRequest validRequest;
    private Customer activeCustomer;

    @BeforeEach
    void setUp() {
        activeCustomer = Customer.builder()
                .id(1L)
                .status(CustomerStatus.ACTIVE)
                .build();

        validRequest = new MeterRequest();
        validRequest.setCustomerId(1L);
        validRequest.setMeterNumber("WM-10001");
        validRequest.setMeterType(MeterType.WATER);
        validRequest.setInstallationDate(LocalDate.of(2024, 1, 15));
    }

    @Test
    void create_inactiveCustomer_throws() {
        when(customerService.findById(1L)).thenReturn(
                Customer.builder().id(1L).status(CustomerStatus.INACTIVE).build());

        assertThrows(BusinessRuleException.class, () -> meterService.create(validRequest));
        verify(meterRepository, never()).save(any());
    }

    @Test
    void create_futureInstallationDate_throws() {
        when(customerService.findById(1L)).thenReturn(activeCustomer);
        validRequest.setInstallationDate(LocalDate.now().plusDays(1));

        assertThrows(BusinessRuleException.class, () -> meterService.create(validRequest));
        verify(meterRepository, never()).save(any());
    }

    @Test
    void create_meterNumberPrefixMismatch_throws() {
        when(customerService.findById(1L)).thenReturn(activeCustomer);
        validRequest.setMeterNumber("EM-10001");

        assertThrows(BusinessRuleException.class, () -> meterService.create(validRequest));
        verify(meterRepository, never()).save(any());
    }

    @Test
    void create_duplicateMeterNumber_throws() {
        when(customerService.findById(1L)).thenReturn(activeCustomer);
        doThrow(new DuplicateResourceException("Meter number already exists"))
                .when(duplicateCheckService).assertUniqueMeterNumber("WM-10001", null);

        assertThrows(DuplicateResourceException.class, () -> meterService.create(validRequest));
        verify(meterRepository, never()).save(any());
    }

    @Test
    void update_cannotReassignCustomer_throws() {
        Meter meter = Meter.builder()
                .id(10L)
                .customer(Customer.builder().id(1L).status(CustomerStatus.ACTIVE).build())
                .build();
        when(meterRepository.findByIdWithCustomer(10L)).thenReturn(Optional.of(meter));
        validRequest.setCustomerId(2L);

        assertThrows(BusinessRuleException.class, () -> meterService.update(10L, validRequest));
    }
}
