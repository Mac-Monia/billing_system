package com.utility.billing.dto.request;

import com.utility.billing.enums.RoleName;
import com.utility.billing.validation.RwandaPhone;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Admin creates staff user with temporary password")
public class UserCreateRequest {

    @NotBlank(message = "First name is required")
    @Schema(example = "Alice")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Schema(example = "Mukamana")
    private String lastName;

    @NotBlank
    @Email
    @Schema(example = "alice.mukamana@wasac.com")
    private String email;

    @NotBlank
    @RwandaPhone
    @Schema(example = "+250722123456")
    private String phoneNumber;

    @NotNull(message = "Role is required")
    @Schema(example = "OPERATOR")
    private RoleName role;
}
