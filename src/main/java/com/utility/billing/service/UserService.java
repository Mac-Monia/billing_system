package com.utility.billing.service;

import com.utility.billing.dto.request.UserCreateRequest;
import com.utility.billing.dto.request.UserUpdateRequest;
import com.utility.billing.dto.response.UserCreateResponse;
import com.utility.billing.entity.Role;
import com.utility.billing.entity.User;
import com.utility.billing.enums.AuditActionType;
import com.utility.billing.enums.RoleName;
import com.utility.billing.enums.UserStatus;
import com.utility.billing.exception.BusinessRuleException;
import com.utility.billing.exception.ResourceNotFoundException;
import com.utility.billing.repository.MeterReadingRepository;
import com.utility.billing.repository.PaymentRepository;
import com.utility.billing.repository.RoleRepository;
import com.utility.billing.repository.UserRepository;
import com.utility.billing.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final String TEMP_PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789@#$";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final MeterReadingRepository meterReadingRepository;
    private final PaymentRepository paymentRepository;
    private final PasswordEncoder passwordEncoder;
    private final DuplicateCheckService duplicateCheckService;
    private final AuditService auditService;
    private final EmailService emailService;

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    @Transactional
    public UserCreateResponse createStaff(UserCreateRequest request) {
        if (request.getRole() == RoleName.CUSTOMER) {
            throw new BusinessRuleException("Use customer registration for CUSTOMER role");
        }
        duplicateCheckService.assertUniqueUserEmail(request.getEmail());

        Role role = roleRepository.findByName(request.getRole())
                .orElseThrow(() -> new BusinessRuleException("Invalid role"));

        String temporaryPassword = generateTemporaryPassword();
        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .password(passwordEncoder.encode(temporaryPassword))
                .status(UserStatus.ACTIVE)
                .passwordExpired(true)
                .forcePasswordChange(true)
                .emailVerified(true)
                .roles(Set.of(role))
                .build();

        user = userRepository.save(user);
        auditService.log(AuditActionType.CREATE, "User", user.getId(), null, user.getEmail());

        emailService.sendStaffCredentialsEmail(user, request.getRole(), temporaryPassword);

        return UserCreateResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .passwordExpired(true)
                .forcePasswordChange(true)
                .message("Login credentials sent to " + user.getEmail())
                .build();
    }

    @Transactional
    public User update(Long id, UserUpdateRequest request) {
        User user = findById(id);
        if (Boolean.TRUE.equals(user.getSeededAdmin())) {
            throw new BusinessRuleException("Seeded default admin cannot be modified");
        }

        RoleName previousRole = user.getRoles().stream().findFirst().map(Role::getName).orElse(null);

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }

        if (request.getRole() != null && previousRole != request.getRole()) {
            Role newRole = roleRepository.findByName(request.getRole())
                    .orElseThrow(() -> new BusinessRuleException("Invalid role"));
            user.getRoles().clear();
            user.getRoles().add(newRole);
            emailService.sendRoleChangeEmail(user, previousRole, request.getRole());
        }

        user = userRepository.save(user);
        auditService.log(AuditActionType.UPDATE, "User", user.getId(),
                previousRole != null ? previousRole.name() : null,
                request.getRole() != null ? request.getRole().name() : user.getEmail());
        return user;
    }

    @Transactional
    public void delete(Long id) {
        User user = findById(id);
        if (Boolean.TRUE.equals(user.getSeededAdmin())) {
            throw new BusinessRuleException("Seeded default admin cannot be deleted");
        }

        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal.getId().equals(id)) {
            throw new BusinessRuleException("You cannot delete your own account");
        }

        meterReadingRepository.clearCapturedByUserId(id);
        paymentRepository.clearRecordedByUserId(id);
        user.getRoles().clear();
        user.setCustomer(null);
        userRepository.delete(user);
        auditService.log(AuditActionType.DELETE, "User", id, user.getEmail(), null);
    }

    private String generateTemporaryPassword() {
        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder(12);
        for (int i = 0; i < 12; i++) {
            password.append(TEMP_PASSWORD_CHARS.charAt(random.nextInt(TEMP_PASSWORD_CHARS.length())));
        }
        return password.toString();
    }
}
