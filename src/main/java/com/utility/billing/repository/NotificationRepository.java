package com.utility.billing.repository;

import com.utility.billing.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
    void deleteByCustomerId(Long customerId);
    boolean existsByCustomerIdAndMessageContaining(Long customerId, String fragment);
}
