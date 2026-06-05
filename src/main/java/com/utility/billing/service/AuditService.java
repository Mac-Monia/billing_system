package com.utility.billing.service;

import com.utility.billing.entity.AuditLog;
import com.utility.billing.enums.AuditActionType;
import com.utility.billing.repository.AuditLogRepository;
import com.utility.billing.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public void log(AuditActionType actionType, String entityName, Long entityId, String oldValue, String newValue) {
        auditLogRepository.save(AuditLog.builder()
                .userEmail(resolveCurrentUserEmail())
                .actionType(actionType)
                .entityName(entityName)
                .entityId(entityId)
                .oldValue(oldValue)
                .newValue(newValue)
                .createdAt(LocalDateTime.now())
                .build());
    }

    private String resolveCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            return principal.getEmail();
        }
        return "SYSTEM";
    }
}
