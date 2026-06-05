package com.utility.billing.service;

import com.utility.billing.dto.request.TariffRequest;
import com.utility.billing.dto.request.TariffTierRequest;
import com.utility.billing.entity.Tariff;
import com.utility.billing.entity.TariffTier;
import com.utility.billing.enums.MeterType;
import com.utility.billing.enums.TariffType;
import com.utility.billing.exception.BusinessRuleException;
import com.utility.billing.exception.ResourceNotFoundException;
import com.utility.billing.repository.TariffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.utility.billing.enums.AuditActionType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TariffService {

    private final TariffRepository tariffRepository;
    private final AuditService auditService;

    public List<Tariff> findAll() {
        return tariffRepository.findAll();
    }

    public Tariff findById(Long id) {
        return tariffRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tariff not found: " + id));
    }

    public Tariff findActiveTariff(MeterType meterType) {
        return findEffectiveTariff(meterType, LocalDate.now());
    }

    @Transactional
    public Tariff create(TariffRequest request) {
        validateTariffRequest(request);

        int nextVersion = tariffRepository.findTopByMeterTypeOrderByVersionDesc(request.getMeterType())
                .map(t -> t.getVersion() + 1)
                .orElse(1);

        tariffRepository.findTopByMeterTypeOrderByVersionDesc(request.getMeterType())
                .ifPresent(current -> {
                    if (current.getEffectiveTo() == null
                            && current.getEffectiveFrom().isBefore(request.getEffectiveFrom())) {
                        current.setEffectiveTo(request.getEffectiveFrom());
                        current.setActive(false);
                        tariffRepository.save(current);
                    }
                });

        Tariff tariff = buildTariffFromRequest(request, nextVersion);
        tariff = tariffRepository.save(tariff);
        auditService.log(AuditActionType.CREATE, "Tariff", tariff.getId(), null, tariff.getVersion().toString());
        return tariff;
    }

    @Transactional
    public Tariff update(Long id, TariffRequest request) {
        Tariff tariff = findById(id);
        validateTariffRequest(request);
        tariff.setMeterType(request.getMeterType());
        tariff.setTariffType(request.getTariffType());
        tariff.setFlatRate(request.getRatePerUnit());
        tariff.setFixedCharge(request.getFixedCharge());
        tariff.setVatPercent(request.getVatPercent());
        tariff.setLatePenaltyPercent(request.getLatePenaltyPercent());
        tariff.setEffectiveFrom(request.getEffectiveFrom());
        tariff.getTiers().clear();
        if (request.getTariffType() == TariffType.TIERED && request.getTiers() != null) {
            for (TariffTierRequest tierReq : request.getTiers()) {
                TariffTier tier = TariffTier.builder()
                        .tariff(tariff)
                        .minConsumption(tierReq.getMinConsumption())
                        .maxConsumption(tierReq.getMaxConsumption())
                        .ratePerUnit(tierReq.getRatePerUnit())
                        .build();
                tariff.getTiers().add(tier);
            }
        }
        tariff = tariffRepository.save(tariff);
        auditService.log(AuditActionType.UPDATE, "Tariff", tariff.getId(), null, tariff.getVersion().toString());
        return tariff;
    }

    public BigDecimal calculateConsumptionCharge(Tariff tariff, BigDecimal consumption) {
        if (tariff.getTariffType() == TariffType.FLAT) {
            return consumption.multiply(tariff.getFlatRate()).setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal remaining = consumption;
        BigDecimal total = BigDecimal.ZERO;
        for (TariffTier tier : tariff.getTiers()) {
            BigDecimal tierMax = tier.getMaxConsumption() != null
                    ? tier.getMaxConsumption().subtract(tier.getMinConsumption())
                    : remaining;
            BigDecimal unitsInTier = remaining.min(tierMax);
            if (unitsInTier.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            total = total.add(unitsInTier.multiply(tier.getRatePerUnit()));
            remaining = remaining.subtract(unitsInTier);
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateVat(Tariff tariff, BigDecimal subtotal) {
        return subtotal.multiply(tariff.getVatPercent())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateLatePenalty(Tariff tariff, BigDecimal amount) {
        return amount.multiply(tariff.getLatePenaltyPercent())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    public Tariff findEffectiveTariff(MeterType meterType, LocalDate billingDate) {
        return tariffRepository.findEffectiveTariff(meterType, billingDate)
                .orElseThrow(() -> new BusinessRuleException(
                        "No active tariff for " + meterType + " on " + billingDate));
    }

    private Tariff buildTariffFromRequest(TariffRequest request, int version) {
        Tariff tariff = Tariff.builder()
                .version(version)
                .meterType(request.getMeterType())
                .tariffType(request.getTariffType())
                .flatRate(request.getRatePerUnit())
                .fixedCharge(request.getFixedCharge())
                .vatPercent(request.getVatPercent())
                .latePenaltyPercent(request.getLatePenaltyPercent())
                .effectiveFrom(request.getEffectiveFrom())
                .active(true)
                .build();

        if (request.getTariffType() == TariffType.TIERED && request.getTiers() != null) {
            for (TariffTierRequest tierReq : request.getTiers()) {
                TariffTier tier = TariffTier.builder()
                        .tariff(tariff)
                        .minConsumption(tierReq.getMinConsumption())
                        .maxConsumption(tierReq.getMaxConsumption())
                        .ratePerUnit(tierReq.getRatePerUnit())
                        .build();
                tariff.getTiers().add(tier);
            }
        }
        return tariff;
    }

    private void validateTariffRequest(TariffRequest request) {
        if (request.getEffectiveFrom().isBefore(LocalDate.now())) {
            throw new BusinessRuleException("Effective date cannot be in the past");
        }
        if (request.getTariffType() == TariffType.FLAT && request.getRatePerUnit() == null) {
            throw new BusinessRuleException("Rate per unit is required for FLAT tariff type");
        }
        if (request.getTariffType() == TariffType.TIERED
                && (request.getTiers() == null || request.getTiers().isEmpty())) {
            throw new BusinessRuleException("At least one tier is required for TIERED tariff type");
        }
        if (request.getTariffType() == TariffType.TIERED) {
            validateTierRanges(request.getTiers());
        }
    }

    private void validateTierRanges(List<TariffTierRequest> tiers) {
        List<TariffTierRequest> sorted = tiers.stream()
                .sorted(Comparator.comparing(TariffTierRequest::getMinConsumption))
                .toList();
        BigDecimal previousMax = null;
        for (TariffTierRequest tier : sorted) {
            if (tier.getMaxConsumption() != null
                    && tier.getMinConsumption().compareTo(tier.getMaxConsumption()) >= 0) {
                throw new BusinessRuleException("Tier min consumption must be less than max consumption");
            }
            if (previousMax != null && tier.getMinConsumption().compareTo(previousMax) < 0) {
                throw new BusinessRuleException("Tariff tier ranges must not overlap");
            }
            previousMax = tier.getMaxConsumption() != null ? tier.getMaxConsumption() : tier.getMinConsumption();
        }
    }
}
