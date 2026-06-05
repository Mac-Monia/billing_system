package com.utility.billing.service;

import com.utility.billing.dto.request.PaymentRequest;
import com.utility.billing.entity.Bill;
import com.utility.billing.entity.Customer;
import com.utility.billing.enums.BillStatus;
import com.utility.billing.enums.PaymentMethod;
import com.utility.billing.exception.BusinessRuleException;
import com.utility.billing.repository.BillRepository;
import com.utility.billing.repository.MeterRepository;
import com.utility.billing.repository.PaymentRepository;
import com.utility.billing.repository.UserRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceValidationTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private BillRepository billRepository;
    @Mock private MeterRepository meterRepository;
    @Mock private BillService billService;
    @Mock private UserRepository userRepository;
    @Mock private SecurityAccessService securityAccessService;
    @Mock private AuditService auditService;

    @InjectMocks
    private PaymentService paymentService;

    private Bill approvedBill;
    private PaymentRequest request;

    @BeforeEach
    void setUp() {
        Customer customer = Customer.builder().id(1L).build();
        approvedBill = Bill.builder()
                .id(10L)
                .billNumber("BILL-TEST01")
                .customer(customer)
                .status(BillStatus.APPROVED)
                .outstandingBalance(new BigDecimal("15000"))
                .amountPaid(BigDecimal.ZERO)
                .build();

        request = new PaymentRequest();
        request.setBillReference("BILL-TEST01");
        request.setAmount(new BigDecimal("15000"));
        request.setPaymentMethod(PaymentMethod.MOMO);
        request.setPaymentDate(LocalDate.now());
    }

    @Test
    void recordPayment_overpayment_rejected() {
        request.setAmount(new BigDecimal("20000"));
        when(billRepository.findByBillNumber("BILL-TEST01")).thenReturn(Optional.of(approvedBill));
        when(billService.applyOverdueRules(any())).thenReturn(approvedBill);

        assertThrows(BusinessRuleException.class, () -> paymentService.recordPayment(request));
    }

    @Test
    void recordPayment_zeroAmount_rejected() {
        request.setAmount(BigDecimal.ZERO);
        assertThrows(BusinessRuleException.class, () -> paymentService.recordPayment(request));
    }

    @Test
    void recordPayment_futureDate_rejected() {
        request.setPaymentDate(LocalDate.now().plusDays(1));
        assertThrows(BusinessRuleException.class, () -> paymentService.recordPayment(request));
    }
}
