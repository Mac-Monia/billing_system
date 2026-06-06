package com.utility.billing.repository;

import com.utility.billing.entity.Bill;
import com.utility.billing.enums.BillStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BillRepository extends JpaRepository<Bill, Long> {

    @Query("""
            SELECT b FROM Bill b
            LEFT JOIN FETCH b.customer
            LEFT JOIN FETCH b.meter
            LEFT JOIN FETCH b.meterReading
            WHERE b.id = :id
            """)
    Optional<Bill> findByIdWithDetails(@Param("id") Long id);

    @Query("""
            SELECT DISTINCT b FROM Bill b
            LEFT JOIN FETCH b.customer
            LEFT JOIN FETCH b.meter
            LEFT JOIN FETCH b.meterReading
            WHERE b.customer.id = :customerId
            ORDER BY b.generatedAt DESC
            """)
    List<Bill> findByCustomerIdWithDetails(@Param("customerId") Long customerId);

    Optional<Bill> findByBillNumber(String billNumber);
    List<Bill> findByCustomerId(Long customerId);
    List<Bill> findByMeterId(Long meterId);
    List<Bill> findByStatus(BillStatus status);
    boolean existsByMeterIdAndBillingMonthAndBillingYear(Long meterId, Integer month, Integer year);
    void deleteByCustomerId(Long customerId);
    void deleteByMeterId(Long meterId);
}
