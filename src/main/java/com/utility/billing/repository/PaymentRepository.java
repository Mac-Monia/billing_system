package com.utility.billing.repository;

import com.utility.billing.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByPaymentReference(String paymentReference);
    List<Payment> findByBillId(Long billId);
    boolean existsByPaymentReference(String paymentReference);
    void deleteByBillId(Long billId);

    @Query("SELECT p FROM Payment p JOIN p.bill b WHERE b.customer.id = :customerId ORDER BY p.paymentDate DESC")
    List<Payment> findByCustomerId(@Param("customerId") Long customerId);

    @Modifying
    @Query("UPDATE Payment p SET p.recordedBy = null WHERE p.recordedBy.id = :userId")
    void clearRecordedByUserId(@Param("userId") Long userId);
}
