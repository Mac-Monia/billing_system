package com.utility.billing.dto.request;

import com.utility.billing.enums.RoleName;
import com.utility.billing.enums.UserStatus;
import com.utility.billing.validation.RwandaPhone;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserUpdateRequest {

    @NotBlank
    @Schema(example = "Alice")
    private String firstName;

    @NotBlank
    @Schema(example = "Mukamana")
    private String lastName;

    @RwandaPhone
    @Schema(example = "+250722123456")
    private String phoneNumber;

    private UserStatus status;

    @Schema(description = "Update role; sends role change notification email")
    private RoleName role;
}
