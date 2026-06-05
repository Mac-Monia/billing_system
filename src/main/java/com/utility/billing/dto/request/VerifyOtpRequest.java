package com.utility.billing.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Schema(description = "Verify customer email using OTP sent during registration")
public class VerifyOtpRequest {

    @NotBlank
    @Email
    @Schema(example = "jean.uwase@example.com")
    private String email;

    @NotBlank
    @Pattern(regexp = "^\\d{6}$", message = "OTP must be a 6-digit code")
    @Schema(example = "482913")
    private String otp;
}
