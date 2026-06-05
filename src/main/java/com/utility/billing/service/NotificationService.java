package com.utility.billing.service;

import com.utility.billing.entity.Notification;
import com.utility.billing.exception.ResourceNotFoundException;
import com.utility.billing.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SecurityAccessService securityAccessService;

    public List<Notification> findAll() {
        return notificationRepository.findAll();
    }

    public List<Notification> findByCustomerId(Long customerId) {
        securityAccessService.assertCustomerOwnsCustomerId(customerId);
        return notificationRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    @Transactional
    public Notification markAsRead(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        securityAccessService.assertCustomerOwnsCustomerId(notification.getCustomer().getId());
        notification.setRead(true);
        return notificationRepository.save(notification);
    }
}
