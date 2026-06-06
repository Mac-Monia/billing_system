package com.utility.billing.service;

import com.utility.billing.dto.request.PenaltyConfigurationRequest;
import com.utility.billing.entity.PenaltyConfiguration;
import com.utility.billing.exception.BusinessRuleException;
import com.utility.billing.repository.PenaltyConfigurationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PenaltyService {

    private final PenaltyConfigurationRepository penaltyRepository;

    public List<PenaltyConfiguration> findAll() {
        return penaltyRepository.findAll();
    }

    @Transactional
    public PenaltyConfiguration create(PenaltyConfigurationRequest request) {
        validateEffectiveFrom(request.getEffectiveFrom());

        penaltyRepository.findOpenActivePenalty().ifPresent(current -> {
            if (!request.getEffectiveFrom().isAfter(current.getEffectiveFrom())) {
                throw new BusinessRuleException(
                        "New penalty configuration must take effect after the current one (after "
                                + current.getEffectiveFrom() + ")");
            }
            current.setEffectiveTo(request.getEffectiveFrom());
            current.setActive(false);
            penaltyRepository.save(current);
        });

        PenaltyConfiguration penalty = PenaltyConfiguration.builder()
                .name(request.getName())
                .ratePercent(request.getRatePercent())
                .graceDays(request.getGraceDays())
                .effectiveFrom(request.getEffectiveFrom())
                .active(true)
                .build();
        return penaltyRepository.save(penalty);
    }

    public BigDecimal calculatePenalty(BigDecimal outstanding, LocalDate dueDate, LocalDate asOf) {
        return penaltyRepository.findEffectivePenalty(asOf)
                .filter(p -> dueDate.plusDays(p.getGraceDays()).isBefore(asOf))
                .map(p -> outstanding.multiply(p.getRatePercent())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP))
                .orElse(BigDecimal.ZERO);
    }

    private void validateEffectiveFrom(LocalDate effectiveFrom) {
        if (effectiveFrom.isBefore(LocalDate.now())) {
            throw new BusinessRuleException("Effective date cannot be in the past");
        }
    }
}
