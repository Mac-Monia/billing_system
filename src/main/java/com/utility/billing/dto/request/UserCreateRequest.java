package com.utility.billing.dto.request;

import com.utility.billing.enums.RoleName;
import com.utility.billing.util.PhoneNumbers;
import com.utility.billing.validation.CountryCode;
import com.utility.billing.validation.RwandaLocalPhone;
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

    @CountryCode
    @Schema(description = "International dialing code", example = "+250", defaultValue = "+250")
    private String countryCode = PhoneNumbers.DEFAULT_COUNTRY_CODE;

    @NotBlank(message = "Phone number is required")
    @RwandaLocalPhone
    @Schema(description = "Local phone number without country code", example = "722123456")
    private String phone;

    @NotNull(message = "Role is required")
    @Schema(example = "OPERATOR")
    private RoleName role;

    public String getPhoneNumber() {
        return PhoneNumbers.toE164(countryCode, phone);
    }
}
