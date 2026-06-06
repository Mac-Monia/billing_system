package com.utility.billing.dto.request;

import com.utility.billing.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChangePasswordRequest {

    @NotBlank
    private String currentPassword;

    @NotBlank
    @StrongPassword
    private String newPassword;
}
