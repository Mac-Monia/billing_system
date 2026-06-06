package com.utility.billing.service;

import com.utility.billing.entity.Bill;
import com.utility.billing.enums.BillStatus;
import com.utility.billing.exception.BusinessRuleException;
import com.utility.billing.repository.BillRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BillServiceValidationTest {

    @Mock private BillRepository billRepository;
    @Mock private MeterReadingService meterReadingService;
    @Mock private TariffService tariffService;
    @Mock private TaxService taxService;
    @Mock private PenaltyService penaltyService;
    @Mock private EmailService emailService;
    @Mock private SecurityAccessService securityAccessService;
    @Mock private MeterService meterService;
    @Mock private AuditService auditService;

    @InjectMocks
    private BillService billService;

    @Test
    void approve_alreadyApproved_rejected() {
        Bill bill = Bill.builder().id(1L).status(BillStatus.APPROVED).build();
        when(billRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(bill));

        assertThrows(BusinessRuleException.class, () -> billService.approve(1L));
    }
}
