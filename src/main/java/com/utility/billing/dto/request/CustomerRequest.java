package com.utility.billing.dto.request;

import com.utility.billing.enums.CustomerStatus;
import com.utility.billing.validation.AdultAge;
import com.utility.billing.validation.NationalId;
import com.utility.billing.validation.RwandaPhone;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(description = "Customer create/update payload")
public class CustomerRequest {

    @NotBlank(message = "First name is required")
    @Schema(example = "Jean")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Schema(example = "Uwase")
    private String lastName;

    @NotBlank
    @NationalId
    @Schema(example = "1234567890123456")
    private String nationalId;

    @NotBlank
    @Email
    @Schema(example = "jean.uwase@example.com")
    private String email;

    @NotBlank
    @RwandaPhone
    @Schema(example = "+250788310922")
    private String phoneNumber;

    @NotBlank
    @Schema(example = "Kigali, Gasabo")
    private String address;

    @AdultAge
    @Schema(example = "1998-05-15")
    private LocalDate dateOfBirth;

    private CustomerStatus status;
}
