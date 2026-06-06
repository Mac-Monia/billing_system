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

    @Query("SELECT DISTINCT t FROM Tariff t LEFT JOIN FETCH t.tiers ORDER BY t.id")
    List<Tariff> findAllWithTiers();

    @Query("SELECT t FROM Tariff t LEFT JOIN FETCH t.tiers WHERE t.id = :id")
    Optional<Tariff> findByIdWithTiers(@Param("id") Long id);

    List<Tariff> findByMeterTypeOrderByVersionDesc(MeterType meterType);

    @Query("""
            SELECT t FROM Tariff t LEFT JOIN FETCH t.tiers
            WHERE t.meterType = :meterType
              AND t.active = true
              AND t.effectiveFrom <= :billingDate
              AND (t.effectiveTo IS NULL OR t.effectiveTo > :billingDate)
            ORDER BY t.version DESC
            """)
    List<Tariff> findEffectiveTariffs(@Param("meterType") MeterType meterType,
                                     @Param("billingDate") LocalDate billingDate);

    Optional<Tariff> findTopByMeterTypeOrderByVersionDesc(MeterType meterType);
}
