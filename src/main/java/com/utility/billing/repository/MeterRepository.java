package com.utility.billing.repository;

import com.utility.billing.entity.Meter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MeterRepository extends JpaRepository<Meter, Long> {

    @Query("SELECT m FROM Meter m JOIN FETCH m.customer WHERE m.id = :id")
    Optional<Meter> findByIdWithCustomer(@Param("id") Long id);

    @Query("SELECT DISTINCT m FROM Meter m JOIN FETCH m.customer")
    List<Meter> findAllWithCustomer();

    @Query("SELECT m FROM Meter m JOIN FETCH m.customer WHERE m.customer.id = :customerId")
    List<Meter> findByCustomerIdWithCustomer(@Param("customerId") Long customerId);

    Optional<Meter> findByMeterNumber(String meterNumber);
    boolean existsByMeterNumber(String meterNumber);
    boolean existsByMeterNumberAndIdNot(String meterNumber, Long id);
    List<Meter> findByCustomerId(Long customerId);
    void deleteByCustomerId(Long customerId);
}
