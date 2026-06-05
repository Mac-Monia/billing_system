package com.utility.billing.service;

import com.utility.billing.dto.request.MeterRequest;
import com.utility.billing.entity.Customer;
import com.utility.billing.entity.Meter;
import com.utility.billing.enums.AuditActionType;
import com.utility.billing.enums.MeterStatus;
import com.utility.billing.exception.BusinessRuleException;
import com.utility.billing.exception.ResourceNotFoundException;
import com.utility.billing.repository.BillRepository;
import com.utility.billing.repository.MeterReadingRepository;
import com.utility.billing.repository.MeterRepository;
import com.utility.billing.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MeterService {

    private final MeterRepository meterRepository;
    private final BillRepository billRepository;
    private final PaymentRepository paymentRepository;
    private final MeterReadingRepository meterReadingRepository;
    private final CustomerService customerService;
    private final DuplicateCheckService duplicateCheckService;
    private final AuditService auditService;

    public List<Meter> findAll() {
        return meterRepository.findAllWithCustomer();
    }

    public Meter findById(Long id) {
        return meterRepository.findByIdWithCustomer(id)
                .orElseThrow(() -> new ResourceNotFoundException("Meter not found: " + id));
    }

    public List<Meter> findByCustomerId(Long customerId) {
        return meterRepository.findByCustomerIdWithCustomer(customerId);
    }

    @Transactional
    public Meter create(MeterRequest request) {
        Customer customer = customerService.findById(request.getCustomerId());
        duplicateCheckService.assertUniqueMeterNumber(request.getMeterNumber(), null);

        Meter meter = Meter.builder()
                .meterNumber(request.getMeterNumber())
                .meterType(request.getMeterType())
                .installationDate(request.getInstallationDate())
                .status(request.getStatus() != null ? request.getStatus() : MeterStatus.ACTIVE)
                .customer(customer)
                .build();
        meter = meterRepository.save(meter);
        auditService.log(AuditActionType.CREATE, "Meter", meter.getId(), null, meter.getMeterNumber());
        return meter;
    }

    @Transactional
    public Meter update(Long id, MeterRequest request) {
        Meter meter = findById(id);
        duplicateCheckService.assertUniqueMeterNumber(request.getMeterNumber(), id);

        if (!meter.getCustomer().getId().equals(request.getCustomerId())) {
            throw new BusinessRuleException("A meter cannot be reassigned to another customer");
        }

        meter.setMeterNumber(request.getMeterNumber());
        meter.setMeterType(request.getMeterType());
        meter.setInstallationDate(request.getInstallationDate());
        if (request.getStatus() != null) {
            meter.setStatus(request.getStatus());
        }
        return meterRepository.save(meter);
    }

    @Transactional
    public void delete(Long id) {
        Meter meter = findById(id);
        billRepository.findByMeterId(id).forEach(bill -> paymentRepository.deleteByBillId(bill.getId()));
        billRepository.deleteByMeterId(id);
        meterReadingRepository.deleteByMeterId(id);
        meterRepository.delete(meter);
        auditService.log(AuditActionType.DELETE, "Meter", id, meter.getMeterNumber(), null);
    }

    @Transactional
    public void disconnectMeter(Long meterId, Long billId) {
        Meter meter = findById(meterId);
        meter.setStatus(MeterStatus.DISCONNECTED);
        meterRepository.save(meter);
    }

    @Transactional
    public Meter reconnectMeter(Long meterId) {
        Meter meter = findById(meterId);
        meter.setStatus(MeterStatus.ACTIVE);
        meter = meterRepository.save(meter);
        auditService.log(AuditActionType.RECONNECT, "Meter", meter.getId(),
                MeterStatus.DISCONNECTED.name(), MeterStatus.ACTIVE.name());
        return meter;
    }
}
