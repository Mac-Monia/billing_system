package com.utility.billing.service;

import com.utility.billing.entity.Bill;
import com.utility.billing.entity.Meter;
import com.utility.billing.entity.MeterReading;
import com.utility.billing.entity.Notification;
import com.utility.billing.entity.Tariff;
import com.utility.billing.enums.AuditActionType;
import com.utility.billing.enums.BillStatus;
import com.utility.billing.enums.CustomerStatus;
import com.utility.billing.enums.MeterStatus;
import com.utility.billing.exception.BusinessRuleException;
import com.utility.billing.exception.ResourceNotFoundException;
import com.utility.billing.repository.BillRepository;
import com.utility.billing.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BillService {

    private final BillRepository billRepository;
    private final MeterReadingService meterReadingService;
    private final TariffService tariffService;
    private final EmailService emailService;
    private final SecurityAccessService securityAccessService;
    private final NotificationRepository notificationRepository;
    private final MeterService meterService;
    private final AuditService auditService;

    @Value("${app.billing.overdue-penalty-days:30}")
    private int overduePenaltyDays;

    @Value("${app.billing.disconnection-days:60}")
    private int disconnectionDays;

    @Transactional(readOnly = true)
    public List<Bill> findAll() {
        return billRepository.findAll();
    }

    @Transactional
    public Bill findById(Long id) {
        Bill bill = billRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found: " + id));
        securityAccessService.assertCustomerOwnsBill(bill);
        return applyOverdueRules(bill);
    }

    @Transactional
    public List<Bill> findByCustomerId(Long customerId) {
        securityAccessService.assertCustomerOwnsCustomerId(customerId);
        List<Bill> bills = billRepository.findByCustomerId(customerId);
        bills.forEach(this::applyOverdueRules);
        return bills;
    }

    @Transactional
    public Bill generateFromReading(Long meterReadingId) {
        MeterReading reading = meterReadingService.findById(meterReadingId);
        Meter meter = reading.getMeter();

        if (meter.getStatus() != MeterStatus.ACTIVE) {
            throw new BusinessRuleException("Meter must be active to generate a bill");
        }
        if (meter.getCustomer().getStatus() != CustomerStatus.ACTIVE) {
            throw new BusinessRuleException("Inactive customers cannot receive bills");
        }

        if (billRepository.existsByMeterIdAndBillingMonthAndBillingYear(
                meter.getId(), reading.getBillingMonth(), reading.getBillingYear())) {
            throw new BusinessRuleException("Bill already exists for this meter and billing period");
        }

        BigDecimal consumption = reading.getConsumption();
        if (consumption == null || consumption.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("Consumption must be greater than 0");
        }

        LocalDate billingDate = LocalDate.of(reading.getBillingYear(), reading.getBillingMonth(), 1);
        Tariff tariff = tariffService.findEffectiveTariff(meter.getMeterType(), billingDate);

        BigDecimal tariffAmount = tariffService.calculateConsumptionCharge(tariff, consumption);
        BigDecimal fixedCharge = tariff.getFixedCharge();
        BigDecimal subtotal = tariffAmount.add(fixedCharge);
        BigDecimal taxAmount = tariffService.calculateVat(tariff, subtotal);
        BigDecimal totalAmount = subtotal.add(taxAmount);

        if (totalAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleException("Bill amount cannot be negative");
        }

        LocalDate dueDate = billingDate.plusMonths(1).withDayOfMonth(15);

        Bill bill = Bill.builder()
                .billNumber("BILL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .customer(meter.getCustomer())
                .meter(meter)
                .meterReading(reading)
                .billingMonth(reading.getBillingMonth())
                .billingYear(reading.getBillingYear())
                .consumption(consumption)
                .tariffAmount(tariffAmount)
                .fixedCharge(fixedCharge)
                .taxAmount(taxAmount)
                .penaltyAmount(BigDecimal.ZERO)
                .totalAmount(totalAmount)
                .amountPaid(BigDecimal.ZERO)
                .outstandingBalance(totalAmount)
                .status(BillStatus.UNPAID)
                .dueDate(dueDate)
                .generatedAt(LocalDateTime.now())
                .notificationSent(false)
                .build();

        bill = billRepository.save(bill);
        queueBillNotification(bill);
        auditService.log(AuditActionType.CREATE, "Bill", bill.getId(), null, bill.getBillNumber());
        return bill;
    }

    @Transactional
    public Bill approve(Long billId) {
        Bill bill = billRepository.findById(billId)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found: " + billId));

        if (bill.getStatus() == BillStatus.APPROVED) {
            throw new BusinessRuleException("Bill is already approved");
        }
        if (bill.getStatus() != BillStatus.UNPAID) {
            throw new BusinessRuleException("Only unpaid bills can be approved");
        }

        BillStatus oldStatus = bill.getStatus();
        bill.setStatus(BillStatus.APPROVED);
        bill = billRepository.save(bill);
        emailService.sendBillApprovalEmail(bill);
        auditService.log(AuditActionType.APPROVE, "Bill", bill.getId(),
                oldStatus.name(), bill.getStatus().name());
        return bill;
    }

    @Transactional
    public Bill applyOverdueRules(Bill bill) {
        if (bill.getStatus() != BillStatus.APPROVED && bill.getStatus() != BillStatus.PARTIALLY_PAID) {
            return bill;
        }
        if (bill.getOutstandingBalance().compareTo(BigDecimal.ZERO) <= 0 || bill.getDueDate() == null) {
            return bill;
        }

        long daysOverdue = ChronoUnit.DAYS.between(bill.getDueDate(), LocalDate.now());
        if (daysOverdue <= 0) {
            return bill;
        }

        if (daysOverdue > overduePenaltyDays) {
            LocalDate billingDate = LocalDate.of(bill.getBillingYear(), bill.getBillingMonth(), 1);
            Tariff tariff = tariffService.findEffectiveTariff(bill.getMeter().getMeterType(), billingDate);
            BigDecimal penalty = tariffService.calculateLatePenalty(tariff, bill.getOutstandingBalance());
            if (penalty.compareTo(BigDecimal.ZERO) > 0 && bill.getPenaltyAmount().compareTo(penalty) < 0) {
                BigDecimal increase = penalty.subtract(bill.getPenaltyAmount());
                bill.setPenaltyAmount(penalty);
                bill.setTotalAmount(bill.getTotalAmount().add(increase));
                bill.setOutstandingBalance(bill.getOutstandingBalance().add(increase));
            }
        }

        if (daysOverdue > disconnectionDays && bill.getMeter().getStatus() == MeterStatus.ACTIVE) {
            meterService.disconnectMeter(bill.getMeter().getId(), bill.getId());
            auditService.log(AuditActionType.DISCONNECT, "Meter", bill.getMeter().getId(),
                    MeterStatus.ACTIVE.name(), MeterStatus.DISCONNECTED.name());
        }

        return billRepository.save(bill);
    }

    private void queueBillNotification(Bill bill) {
        if (Boolean.TRUE.equals(bill.getNotificationSent())) {
            return;
        }
        String fragment = bill.getBillNumber();
        if (notificationRepository.existsByCustomerIdAndMessageContaining(
                bill.getCustomer().getId(), fragment)) {
            return;
        }

        String message = String.format(
                "Dear %s, Your utility bill of FRW %s has been successfully processed. Reference: %s",
                bill.getCustomer().getFullNames(),
                bill.getTotalAmount(),
                bill.getBillNumber());

        notificationRepository.save(Notification.builder()
                .customer(bill.getCustomer())
                .message(message)
                .createdAt(LocalDateTime.now())
                .read(false)
                .build());

        bill.setNotificationSent(true);
        billRepository.save(bill);

        if (bill.getCustomer().getEmail() == null || bill.getCustomer().getEmail().isBlank()) {
            return;
        }
        emailService.sendBillApprovalEmail(bill);
    }
}
