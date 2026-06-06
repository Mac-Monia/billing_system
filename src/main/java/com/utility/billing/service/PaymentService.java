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
import org.springframework.beans.factory.ObjectProvider;
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
    private final ObjectProvider<DatabasePaymentExecutor> databasePaymentExecutor;

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
        Bill billForPayment = billService.applyOverdueRules(bill);
        final Long billId = billForPayment.getId();

        if (billForPayment.getStatus() == BillStatus.PAID) {
            throw new BusinessRuleException("Bill is already fully paid");
        }
        if (billForPayment.getStatus() != BillStatus.APPROVED
                && billForPayment.getStatus() != BillStatus.PARTIALLY_PAID
                && billForPayment.getStatus() != BillStatus.OVERDUE) {
            throw new BusinessRuleException("Payments can only be recorded for approved bills");
        }
        if (request.getAmount().compareTo(billForPayment.getOutstandingBalance()) > 0) {
            throw new BusinessRuleException("Payment amount exceeds outstanding balance");
        }

        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User recorder = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String reference = generatePaymentReference();

        DatabasePaymentExecutor executor = databasePaymentExecutor.getIfAvailable();
        Payment payment;
        Bill updatedBill;

        if (executor != null) {
            executor.processPayment(
                    billId,
                    request.getAmount(),
                    request.getPaymentMethod(),
                    reference,
                    request.getPaymentDate());

            updatedBill = billRepository.findByIdWithDetails(billId)
                    .orElseThrow(() -> new ResourceNotFoundException("Bill not found: " + billId));
            payment = paymentRepository.findByPaymentReference(reference)
                    .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + reference));
            payment.setRecordedBy(recorder);
            payment = paymentRepository.save(payment);
        } else {
            payment = Payment.builder()
                    .paymentReference(reference)
                    .bill(billForPayment)
                    .amount(request.getAmount())
                    .paymentMethod(request.getPaymentMethod())
                    .paymentDate(request.getPaymentDate())
                    .recordedAt(LocalDateTime.now())
                    .recordedBy(recorder)
                    .build();
            paymentRepository.save(payment);

            billForPayment.setAmountPaid(billForPayment.getAmountPaid().add(request.getAmount()));
            billForPayment.setOutstandingBalance(billForPayment.getOutstandingBalance().subtract(request.getAmount()));

            if (billForPayment.getOutstandingBalance().compareTo(BigDecimal.ZERO) <= 0) {
                billForPayment.setOutstandingBalance(BigDecimal.ZERO);
                billForPayment.setStatus(BillStatus.PAID);
            } else {
                billForPayment.setStatus(BillStatus.PARTIALLY_PAID);
            }
            updatedBill = billRepository.save(billForPayment);
        }

        if (updatedBill.getStatus() == BillStatus.PAID) {
            reconnectMeterIfNeeded(updatedBill.getMeter());
        }

        auditService.log(AuditActionType.PAYMENT, "Bill", updatedBill.getId(),
                updatedBill.getOutstandingBalance().toPlainString(), updatedBill.getStatus().name());
        return payment;
    }

    private String generatePaymentReference() {
        String reference = "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        while (paymentRepository.existsByPaymentReference(reference)) {
            reference = "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
        return reference;
    }

    private void reconnectMeterIfNeeded(Meter meter) {
        if (meter != null && meter.getStatus() == MeterStatus.DISCONNECTED) {
            meter.setStatus(MeterStatus.ACTIVE);
            meterRepository.save(meter);
            auditService.log(AuditActionType.RECONNECT, "Meter", meter.getId(),
                    MeterStatus.DISCONNECTED.name(), MeterStatus.ACTIVE.name());
        }
    }
}
