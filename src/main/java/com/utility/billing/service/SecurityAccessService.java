package com.utility.billing.service;

import com.utility.billing.entity.Bill;
import com.utility.billing.entity.User;
import com.utility.billing.exception.BusinessRuleException;
import com.utility.billing.repository.UserRepository;
import com.utility.billing.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SecurityAccessService {

    private final UserRepository userRepository;

    public boolean isAdmin() {
        return hasRole("ROLE_ADMIN");
    }

    public boolean isFinance() {
        return hasRole("ROLE_FINANCE");
    }

    public boolean isOperator() {
        return hasRole("ROLE_OPERATOR");
    }

    public boolean isCustomer() {
        return hasRole("ROLE_CUSTOMER");
    }

    public boolean isAdminOrFinance() {
        return isAdmin() || isFinance();
    }

    public boolean isAdminOrOperator() {
        return isAdmin() || isOperator();
    }

    public User getCurrentUser() {
        UserPrincipal principal = getPrincipal();
        return userRepository.findById(principal.getId())
                .orElseThrow(() -> new BusinessRuleException("Authenticated user not found"));
    }

    public void assertAdminOrFinance() {
        if (!isAdminOrFinance()) {
            throw new AccessDeniedException("Admin or finance access required");
        }
    }

    public void assertAdminOrOperator() {
        if (!isAdminOrOperator()) {
            throw new AccessDeniedException("Admin or operator access required");
        }
    }

    public void assertCustomerOwnsCustomerId(Long customerId) {
        if (isAdminOrFinance()) {
            return;
        }
        User user = getCurrentUser();
        if (user.getCustomer() == null || !user.getCustomer().getId().equals(customerId)) {
            throw new AccessDeniedException("You can only access your own customer data");
        }
    }

    public void assertCustomerOwnsBill(Bill bill) {
        if (isAdminOrFinance()) {
            return;
        }
        User user = getCurrentUser();
        if (user.getCustomer() == null || !user.getCustomer().getId().equals(bill.getCustomer().getId())) {
            throw new AccessDeniedException("You can only access your own bills");
        }
    }

    private UserPrincipal getPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new AccessDeniedException("Authentication required");
        }
        return principal;
    }

    private boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role::equals);
    }
}
