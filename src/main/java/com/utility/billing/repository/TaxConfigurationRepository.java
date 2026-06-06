package com.utility.billing.repository;

import com.utility.billing.entity.TaxConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TaxConfigurationRepository extends JpaRepository<TaxConfiguration, Long> {
    @Query("""
            SELECT t FROM TaxConfiguration t
            WHERE t.active = true
              AND t.effectiveFrom <= :date
              AND (t.effectiveTo IS NULL OR t.effectiveTo > :date)
            """)
    List<TaxConfiguration> findEffectiveTaxes(@Param("date") LocalDate date);

    @Query("""
            SELECT t FROM TaxConfiguration t
            WHERE t.active = true
              AND t.effectiveTo IS NULL
              AND t.name = :name
            ORDER BY t.effectiveFrom DESC
            """)
    Optional<TaxConfiguration> findOpenActiveByName(@Param("name") String name);
}
