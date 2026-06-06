package com.utility.billing.dto.request;

import com.utility.billing.enums.RoleName;
import com.utility.billing.enums.UserStatus;
import com.utility.billing.util.PhoneNumbers;
import com.utility.billing.validation.CountryCode;
import com.utility.billing.validation.RwandaLocalPhone;
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

    @CountryCode
    @Schema(description = "International dialing code", example = "+250", defaultValue = "+250")
    private String countryCode = PhoneNumbers.DEFAULT_COUNTRY_CODE;

    @RwandaLocalPhone
    @Schema(description = "Local phone number without country code", example = "722123456")
    private String phone;

    private UserStatus status;

    @Schema(description = "Update role; sends role change notification email")
    private RoleName role;

    public String getPhoneNumber() {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        return PhoneNumbers.toE164(countryCode, phone);
    }
}
