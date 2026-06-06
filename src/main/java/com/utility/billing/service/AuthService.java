package com.utility.billing.service;

import com.utility.billing.dto.request.ChangePasswordRequest;
import com.utility.billing.dto.request.LoginRequest;
import com.utility.billing.dto.request.RegisterRequest;
import com.utility.billing.dto.request.VerifyOtpRequest;
import com.utility.billing.dto.response.AuthResponse;
import com.utility.billing.entity.Customer;
import com.utility.billing.entity.Role;
import com.utility.billing.entity.User;
import com.utility.billing.enums.AuditActionType;
import com.utility.billing.enums.CustomerStatus;
import com.utility.billing.enums.RoleName;
import com.utility.billing.enums.UserStatus;
import com.utility.billing.exception.BusinessRuleException;
import com.utility.billing.repository.CustomerRepository;
import com.utility.billing.repository.RoleRepository;
import com.utility.billing.repository.UserRepository;
import com.utility.billing.security.JwtTokenProvider;
import com.utility.billing.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final DuplicateCheckService duplicateCheckService;
    private final AuditService auditService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        RoleName targetRole = request.getRole() != null ? request.getRole() : RoleName.CUSTOMER;

        if (targetRole != RoleName.CUSTOMER) {
            assertAdminCanRegisterStaff();
            return registerStaff(request, targetRole);
        }

        return registerCustomer(request);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new DisabledException("Account is disabled");
        }
        if (Boolean.TRUE.equals(user.getAccountLocked())) {
            throw new LockedException("Account is locked");
        }

        Authentication authentication = authenticate(request.getEmail(), request.getPassword());
        auditService.log(AuditActionType.LOGIN, "User", user.getId(), null, user.getEmail());
        return buildAuthResponse(user, authentication);
    }

    @Transactional
    public AuthResponse changePassword(ChangePasswordRequest request) {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new BusinessRuleException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadCredentialsException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordExpired(false);
        user.setForcePasswordChange(false);
        userRepository.save(user);
        auditService.log(AuditActionType.UPDATE, "User", user.getId(), "password", "changed");

        AuthResponse response = buildAuthResponse(user, authenticate(user.getEmail(), request.getNewPassword()));
        response.setMessage("Password changed successfully");
        return response;
    }

    @Transactional
    public AuthResponse verifyOtp(VerifyOtpRequest request) {
        throw new BusinessRuleException("Email verification is not required. Register, then log in via POST /api/auth/login");
    }

    @Transactional
    public AuthResponse resendOtp(String email) {
        throw new BusinessRuleException("Email verification is not required. Log in via POST /api/auth/login");
    }

    private AuthResponse registerCustomer(RegisterRequest request) {
        duplicateCheckService.assertUniqueUserEmail(request.getEmail());

        if (request.getNationalId() == null || request.getNationalId().isBlank()) {
            throw new BusinessRuleException("National ID is required for customer registration");
        }
        if (request.getAddress() == null || request.getAddress().isBlank()) {
            throw new BusinessRuleException("Address is required for customer registration");
        }
        duplicateCheckService.assertUniqueCustomerNationalId(request.getNationalId(), null);
        duplicateCheckService.assertUniqueCustomerEmail(request.getEmail(), null);
        duplicateCheckService.assertUniqueCustomerPhone(request.getPhoneNumber(), null);

        Role role = roleRepository.findByName(RoleName.CUSTOMER)
                .orElseThrow(() -> new BusinessRuleException("Invalid role"));

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .password(passwordEncoder.encode(request.getPassword()))
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .forcePasswordChange(false)
                .passwordExpired(false)
                .roles(Set.of(role))
                .build();

        Customer customer = Customer.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .nationalId(request.getNationalId())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .address(request.getAddress())
                .dateOfBirth(request.getDateOfBirth())
                .status(CustomerStatus.ACTIVE)
                .build();
        user.setCustomer(customerRepository.save(customer));

        userRepository.save(user);
        auditService.log(AuditActionType.CREATE, "User", user.getId(), null, user.getEmail());

        return AuthResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .fullNames(user.getFullNames())
                .roles(Set.of(RoleName.CUSTOMER.authority()))
                .emailVerified(true)
                .message("Registration successful. Log in via POST /api/auth/login")
                .build();
    }

    private AuthResponse registerStaff(RegisterRequest request, RoleName roleName) {
        duplicateCheckService.assertUniqueUserEmail(request.getEmail());

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new BusinessRuleException("Invalid role"));

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .password(passwordEncoder.encode(request.getPassword()))
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .forcePasswordChange(false)
                .passwordExpired(false)
                .roles(Set.of(role))
                .build();

        user = userRepository.save(user);
        auditService.log(AuditActionType.CREATE, "User", user.getId(), null, user.getEmail());

        return AuthResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .fullNames(user.getFullNames())
                .roles(Set.of(roleName.authority()))
                .emailVerified(true)
                .message("Staff account created with role " + roleName.authority())
                .build();
    }

    private void assertAdminCanRegisterStaff() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof UserPrincipal)) {
            throw new BusinessRuleException("Public users cannot register staff roles");
        }
        boolean isAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
        if (!isAdmin) {
            throw new AccessDeniedException("Only admin can register staff users");
        }
    }

    private Authentication authenticate(String email, String password) {
        return authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password));
    }

    private AuthResponse buildAuthResponse(User user, Authentication authentication) {
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String message = Boolean.TRUE.equals(user.getForcePasswordChange())
                ? "Change your temporary password via POST /api/auth/change-password"
                : null;

        return AuthResponse.builder()
                .token(jwtTokenProvider.generateToken(authentication))
                .type("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .fullNames(user.getFullNames())
                .roles(user.getRoles().stream().map(r -> r.getName().authority()).collect(Collectors.toSet()))
                .forcePasswordChange(Boolean.TRUE.equals(user.getForcePasswordChange()))
                .passwordExpired(Boolean.TRUE.equals(user.getPasswordExpired()))
                .emailVerified(Boolean.TRUE.equals(user.getEmailVerified()))
                .message(message)
                .build();
    }
}
