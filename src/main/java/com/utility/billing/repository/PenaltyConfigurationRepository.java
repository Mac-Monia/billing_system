package com.utility.billing.repository;

import com.utility.billing.entity.PenaltyConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface PenaltyConfigurationRepository extends JpaRepository<PenaltyConfiguration, Long> {
    @Query("""
            SELECT p FROM PenaltyConfiguration p
            WHERE p.active = true
              AND p.effectiveFrom <= :date
              AND (p.effectiveTo IS NULL OR p.effectiveTo > :date)
            ORDER BY p.id DESC
            """)
    Optional<PenaltyConfiguration> findEffectivePenalty(@Param("date") LocalDate date);
}
