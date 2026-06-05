package com.utility.billing.service;

import com.utility.billing.dto.request.PaymentRequest;
import com.utility.billing.entity.Bill;
import com.utility.billing.entity.Meter;
import com.utility.billing.entity.Payment;
import com.utility.billing.entity.User;
import com.utility.billing.enums.AuditActionType;
import com.utility.billing.enums.BillStatus;
import com.utility.billing.enums.MeterStatus;
import com.utility.billing.exception.BusinessRuleException;
import com.utility.billing.exception.ResourceNotFoundException;
import com.utility.billing.repository.BillRepository;
import com.utility.billing.repository.MeterRepository;
import com.utility.billing.repository.PaymentRepository;
import com.utility.billing.repository.UserRepository;
import com.utility.billing.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BillRepository billRepository;
    private final MeterRepository meterRepository;
    private final BillService billService;
    private final UserRepository userRepository;
    private final SecurityAccessService securityAccessService;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<Payment> findAll() {
        return paymentRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Payment> findByBillId(Long billId) {
        Bill bill = billService.findById(billId);
        securityAccessService.assertCustomerOwnsBill(bill);
        return paymentRepository.findByBillId(billId);
    }

    @Transactional(readOnly = true)
    public List<Payment> findByCustomerId(Long customerId) {
        securityAccessService.assertCustomerOwnsCustomerId(customerId);
        return paymentRepository.findByCustomerId(customerId);
    }

    @Transactional
    public Payment recordPayment(PaymentRequest request) {
        if (request.getPaymentDate().isAfter(LocalDate.now())) {
            throw new BusinessRuleException("Payment date cannot be in the future");
        }
        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("Payment amount must be greater than 0");
        }

        Bill bill = billRepository.findByBillNumber(request.getBillReference())
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found: " + request.getBillReference()));

        securityAccessService.assertCustomerOwnsBill(bill);
        bill = billService.applyOverdueRules(bill);

        if (bill.getStatus() == BillStatus.PAID) {
            throw new BusinessRuleException("Bill is already fully paid");
        }
        if (bill.getStatus() != BillStatus.APPROVED && bill.getStatus() != BillStatus.PARTIALLY_PAID) {
            throw new BusinessRuleException("Payments can only be recorded for approved bills");
        }
        if (request.getAmount().compareTo(bill.getOutstandingBalance()) > 0) {
            throw new BusinessRuleException("Payment amount exceeds outstanding balance");
        }

        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User recorder = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String reference = "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        while (paymentRepository.existsByPaymentReference(reference)) {
            reference = "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }

        Payment payment = Payment.builder()
                .paymentReference(reference)
                .bill(bill)
                .amount(request.getAmount())
                .paymentMethod(request.getPaymentMethod())
                .paymentDate(request.getPaymentDate())
                .recordedAt(LocalDateTime.now())
                .recordedBy(recorder)
                .build();

        paymentRepository.save(payment);

        bill.setAmountPaid(bill.getAmountPaid().add(request.getAmount()));
        bill.setOutstandingBalance(bill.getOutstandingBalance().subtract(request.getAmount()));

        if (bill.getOutstandingBalance().compareTo(BigDecimal.ZERO) <= 0) {
            bill.setOutstandingBalance(BigDecimal.ZERO);
            bill.setStatus(BillStatus.PAID);
            reconnectMeterIfNeeded(bill.getMeter());
        } else {
            bill.setStatus(BillStatus.PARTIALLY_PAID);
        }
        billRepository.save(bill);

        auditService.log(AuditActionType.PAYMENT, "Bill", bill.getId(),
                bill.getOutstandingBalance().toPlainString(), bill.getStatus().name());
        return payment;
    }

    private void reconnectMeterIfNeeded(Meter meter) {
        if (meter.getStatus() == MeterStatus.DISCONNECTED) {
            meter.setStatus(MeterStatus.ACTIVE);
            meterRepository.save(meter);
            auditService.log(AuditActionType.RECONNECT, "Meter", meter.getId(),
                    MeterStatus.DISCONNECTED.name(), MeterStatus.ACTIVE.name());
        }
    }
}
