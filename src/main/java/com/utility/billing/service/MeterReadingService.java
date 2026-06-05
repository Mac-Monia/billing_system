package com.utility.billing.service;

import com.utility.billing.dto.request.MeterReadingRequest;
import com.utility.billing.entity.Meter;
import com.utility.billing.entity.MeterReading;
import com.utility.billing.entity.User;
import com.utility.billing.enums.AuditActionType;
import com.utility.billing.enums.MeterStatus;
import com.utility.billing.exception.BusinessRuleException;
import com.utility.billing.exception.ResourceNotFoundException;
import com.utility.billing.repository.MeterReadingRepository;
import com.utility.billing.repository.UserRepository;
import com.utility.billing.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class MeterReadingService {

    private final MeterReadingRepository meterReadingRepository;
    private final MeterService meterService;
    private final UserRepository userRepository;
    private final BillService billService;
    private final AuditService auditService;

    public MeterReadingService(MeterReadingRepository meterReadingRepository,
                               MeterService meterService,
                               UserRepository userRepository,
                               @Lazy BillService billService,
                               AuditService auditService) {
        this.meterReadingRepository = meterReadingRepository;
        this.meterService = meterService;
        this.userRepository = userRepository;
        this.billService = billService;
        this.auditService = auditService;
    }

    public List<MeterReading> findAll() {
        return meterReadingRepository.findAll();
    }

    public List<MeterReading> findByMeterId(Long meterId) {
        meterService.findById(meterId);
        return meterReadingRepository.findByMeterIdOrderByReadingDateDesc(meterId);
    }

    public MeterReading findById(Long id) {
        return meterReadingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Meter reading not found: " + id));
    }

    @Transactional
    public MeterReading capture(MeterReadingRequest request) {
        Meter meter = meterService.findById(request.getMeterId());

        if (meter.getStatus() != MeterStatus.ACTIVE) {
            throw new BusinessRuleException("Meter must be active to capture readings");
        }

        if (request.getCurrentReading().compareTo(request.getPreviousReading()) <= 0) {
            throw new BusinessRuleException("Current reading must be greater than previous reading");
        }

        if (request.getReadingDate().isAfter(LocalDate.now())) {
            throw new BusinessRuleException("Reading date cannot be in the future");
        }

        int month = request.getReadingDate().getMonthValue();
        int year = request.getReadingDate().getYear();

        if (meterReadingRepository.existsByMeterIdAndBillingMonthAndBillingYear(meter.getId(), month, year)) {
            throw new BusinessRuleException("Only one reading per meter per month/year is allowed");
        }

        BigDecimal consumption = request.getCurrentReading().subtract(request.getPreviousReading());
        if (consumption.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleException("Consumption cannot be negative");
        }

        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User operator = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Operator not found"));

        MeterReading reading = MeterReading.builder()
                .meter(meter)
                .previousReading(request.getPreviousReading())
                .currentReading(request.getCurrentReading())
                .consumption(consumption)
                .readingDate(request.getReadingDate())
                .billingMonth(month)
                .billingYear(year)
                .capturedBy(operator)
                .build();

        reading = meterReadingRepository.save(reading);
        auditService.log(AuditActionType.CREATE, "MeterReading", reading.getId(), null,
                consumption.toPlainString());
        billService.generateFromReading(reading.getId());
        return reading;
    }
}
