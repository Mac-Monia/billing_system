package com.utility.billing.controller;

import com.utility.billing.dto.request.ChangePasswordRequest;
import com.utility.billing.dto.request.LoginRequest;
import com.utility.billing.dto.request.RegisterRequest;
import com.utility.billing.dto.request.VerifyOtpRequest;
import com.utility.billing.dto.response.AuthResponse;
import com.utility.billing.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Customer registration with OTP, staff login, password change")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Customer self-registration (sends OTP to email)")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    @Operation(summary = "Login and receive JWT token")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/change-password")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Change password (required after first login with temporary staff password)")
    public AuthResponse changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        return authService.changePassword(request);
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "Verify customer email with OTP and receive JWT token")
    public AuthResponse verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        return authService.verifyOtp(request);
    }

    @PostMapping("/resend-otp")
    @Operation(summary = "Resend customer verification OTP")
    public AuthResponse resendOtp(@RequestParam String email) {
        return authService.resendOtp(email);
    }
}
