package com.utility.billing.repository;

import com.utility.billing.entity.Bill;
import com.utility.billing.enums.BillStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BillRepository extends JpaRepository<Bill, Long> {
    Optional<Bill> findByBillNumber(String billNumber);
    List<Bill> findByCustomerId(Long customerId);
    List<Bill> findByMeterId(Long meterId);
    List<Bill> findByStatus(BillStatus status);
    boolean existsByMeterIdAndBillingMonthAndBillingYear(Long meterId, Integer month, Integer year);
    void deleteByCustomerId(Long customerId);
    void deleteByMeterId(Long meterId);
}
