package com.utility.billing.repository;

import com.utility.billing.entity.TaxConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface TaxConfigurationRepository extends JpaRepository<TaxConfiguration, Long> {
    @Query("""
            SELECT t FROM TaxConfiguration t
            WHERE t.active = true
              AND t.effectiveFrom <= :date
              AND (t.effectiveTo IS NULL OR t.effectiveTo > :date)
            """)
    List<TaxConfiguration> findEffectiveTaxes(@Param("date") LocalDate date);
}
