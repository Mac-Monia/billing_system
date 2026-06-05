package com.utility.billing.repository;

import com.utility.billing.entity.Tariff;
import com.utility.billing.enums.MeterType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TariffRepository extends JpaRepository<Tariff, Long> {
    List<Tariff> findByMeterTypeOrderByVersionDesc(MeterType meterType);

    @Query("""
            SELECT t FROM Tariff t
            WHERE t.meterType = :meterType
              AND t.active = true
              AND t.effectiveFrom <= :billingDate
              AND (t.effectiveTo IS NULL OR t.effectiveTo > :billingDate)
            ORDER BY t.version DESC
            """)
    Optional<Tariff> findEffectiveTariff(@Param("meterType") MeterType meterType,
                                         @Param("billingDate") LocalDate billingDate);

    Optional<Tariff> findTopByMeterTypeOrderByVersionDesc(MeterType meterType);
}
