package com.utility.billing.service;

import com.utility.billing.dto.request.PaymentRequest;
import com.utility.billing.entity.Bill;
import com.utility.billing.entity.Customer;
import com.utility.billing.entity.User;
import com.utility.billing.enums.BillStatus;
import com.utility.billing.enums.PaymentMethod;
import com.utility.billing.enums.UserStatus;
import com.utility.billing.exception.BusinessRuleException;
import com.utility.billing.repository.BillRepository;
import com.utility.billing.repository.MeterRepository;
import com.utility.billing.repository.PaymentRepository;
import com.utility.billing.repository.UserRepository;
import com.utility.billing.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class PaymentServiceValidationTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private BillRepository billRepository;
    @Mock
    private MeterRepository meterRepository;
    @Mock
    private BillService billService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SecurityAccessService securityAccessService;
    @Mock
    private AuditService auditService;
    @Mock
    private EmailService emailService;
    @Mock
    private ObjectProvider<DatabasePaymentExecutor> databasePaymentExecutor;

    @InjectMocks
    private PaymentService paymentService;

    private Bill approvedBill;
    private PaymentRequest request;

    @BeforeEach
    void setUp() {
        lenient().when(databasePaymentExecutor.getIfAvailable()).thenReturn(null);

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

    @Test
    void recordPayment_pendingBill_rejected() {
        approvedBill.setStatus(BillStatus.PENDING);
        when(billRepository.findByBillNumber("BILL-TEST01")).thenReturn(Optional.of(approvedBill));
        when(billService.applyOverdueRules(any())).thenReturn(approvedBill);

        assertThrows(BusinessRuleException.class, () -> paymentService.recordPayment(request));
    }

    @Test
    void recordPayment_alreadyPaid_rejected() {
        approvedBill.setStatus(BillStatus.PAID);
        approvedBill.setOutstandingBalance(BigDecimal.ZERO);
        when(billRepository.findByBillNumber("BILL-TEST01")).thenReturn(Optional.of(approvedBill));
        when(billService.applyOverdueRules(any())).thenReturn(approvedBill);

        assertThrows(BusinessRuleException.class, () -> paymentService.recordPayment(request));
    }

    @Test
    void recordPayment_partialPayment_updatesBalanceAndStatus() {
        request.setAmount(new BigDecimal("5000"));
        stubSuccessfulPaymentRecording();

        withSecurityContext(() -> paymentService.recordPayment(request));

        assertEquals(new BigDecimal("5000"), approvedBill.getAmountPaid());
        assertEquals(new BigDecimal("10000"), approvedBill.getOutstandingBalance());
        assertEquals(BillStatus.PARTIALLY_PAID, approvedBill.getStatus());
        verify(billRepository).save(approvedBill);
    }

    @Test
    void recordPayment_fullPayment_marksBillPaid() {
        stubSuccessfulPaymentRecording();
        when(billRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(approvedBill));

        withSecurityContext(() -> paymentService.recordPayment(request));

        assertEquals(new BigDecimal("15000"), approvedBill.getAmountPaid());
        assertEquals(BigDecimal.ZERO, approvedBill.getOutstandingBalance());
        assertEquals(BillStatus.PAID, approvedBill.getStatus());
        verify(billRepository).save(approvedBill);
        verify(emailService).sendBillPaidEmail(any());
    }

    private Authentication authentication;
    private SecurityContext securityContext;

    private void stubSuccessfulPaymentRecording() {
        when(billRepository.findByBillNumber("BILL-TEST01")).thenReturn(Optional.of(approvedBill));
        when(billService.applyOverdueRules(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentRepository.existsByPaymentReference(any())).thenReturn(false);
        when(paymentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(billRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        User recorder = User.builder()
                .id(1L)
                .email("customer@example.com")
                .password("secret")
                .status(UserStatus.ACTIVE)
                .roles(Collections.emptySet())
                .build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(recorder));

        authentication = mock(Authentication.class);
        securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserPrincipal(recorder));
    }

    private void withSecurityContext(Runnable action) {
        try (MockedStatic<SecurityContextHolder> mocked = mockStatic(SecurityContextHolder.class)) {
            mocked.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            action.run();
        }
    }
}
