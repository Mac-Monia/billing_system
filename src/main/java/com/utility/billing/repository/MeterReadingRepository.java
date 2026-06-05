package com.utility.billing.repository;

import com.utility.billing.entity.MeterReading;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MeterReadingRepository extends JpaRepository<MeterReading, Long> {
    boolean existsByMeterIdAndBillingMonthAndBillingYear(Long meterId, Integer month, Integer year);
    Optional<MeterReading> findByMeterIdAndBillingMonthAndBillingYear(Long meterId, Integer month, Integer year);
    List<MeterReading> findByMeterIdOrderByReadingDateDesc(Long meterId);
    void deleteByMeterId(Long meterId);

    @Modifying
    @Query("UPDATE MeterReading r SET r.capturedBy = null WHERE r.capturedBy.id = :userId")
    void clearCapturedByUserId(@Param("userId") Long userId);
}
