package com.utility.billing.service;

import com.utility.billing.dto.request.TaxConfigurationRequest;
import com.utility.billing.entity.TaxConfiguration;
import com.utility.billing.repository.TaxConfigurationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TaxService {

    private final TaxConfigurationRepository taxRepository;

    public List<TaxConfiguration> findAll() {
        return taxRepository.findAll();
    }

    @Transactional
    public TaxConfiguration create(TaxConfigurationRequest request) {
        TaxConfiguration tax = TaxConfiguration.builder()
                .name(request.getName())
                .rate(request.getRate())
                .effectiveFrom(request.getEffectiveFrom())
                .active(true)
                .build();
        return taxRepository.save(tax);
    }

    public BigDecimal calculateTax(BigDecimal subtotal, LocalDate date) {
        List<TaxConfiguration> taxes = taxRepository.findEffectiveTaxes(date);
        BigDecimal totalTax = BigDecimal.ZERO;
        for (TaxConfiguration tax : taxes) {
            BigDecimal taxAmount = subtotal.multiply(tax.getRate())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            totalTax = totalTax.add(taxAmount);
        }
        return totalTax;
    }
}
