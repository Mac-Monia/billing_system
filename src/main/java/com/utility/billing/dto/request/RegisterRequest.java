package com.utility.billing.dto.request;

import com.utility.billing.enums.RoleName;
import com.utility.billing.util.PhoneNumbers;
import com.utility.billing.validation.AdultAge;
import com.utility.billing.validation.CountryCode;
import com.utility.billing.validation.NationalId;
import com.utility.billing.validation.RwandaLocalPhone;
import com.utility.billing.validation.StrongPassword;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(description = "Registration: customers self-register (no role → CUSTOMER). Admin registers staff with role ADMIN, OPERATOR, or FINANCE.")
public class RegisterRequest {

    @NotBlank(message = "First name is required")
    @Schema(example = "Jean")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Schema(example = "Uwase")
    private String lastName;

    @NotBlank
    @Email
    @Schema(example = "jean.uwase@example.com")
    private String email;

    @CountryCode
    @Schema(description = "International dialing code", example = "+250", defaultValue = "+250")
    private String countryCode = PhoneNumbers.DEFAULT_COUNTRY_CODE;

    @NotBlank(message = "Phone number is required")
    @RwandaLocalPhone
    @Schema(description = "Local phone number without country code", example = "788310922")
    private String phone;

    @NotBlank
    @StrongPassword
    private String password;

    @Schema(description = "Defaults to CUSTOMER for public registration")
    private RoleName role;

    @NationalId
    @Schema(example = "1234567890123456")
    private String nationalId;

    @Schema(example = "Kigali, Gasabo")
    private String address;

    @AdultAge
    @Schema(example = "1998-05-15")
    private LocalDate dateOfBirth;

    public String getPhoneNumber() {
        return PhoneNumbers.toE164(countryCode, phone);
    }
}
