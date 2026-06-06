package com.utility.billing.dto.request;

import com.utility.billing.enums.MeterStatus;
import com.utility.billing.enums.MeterType;
import com.utility.billing.validation.ValidationPatterns;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(description = "Meter create/update payload")
public class MeterRequest {

    @NotNull(message = "Customer ID is required")
    private Long customerId;

    @NotBlank
    @Pattern(regexp = ValidationPatterns.METER_NUMBER, message = ValidationPatterns.METER_NUMBER_MESSAGE)
    @Schema(example = "WM-10001")
    private String meterNumber;

    @NotNull
    @Schema(example = "WATER")
    private MeterType meterType;

    @NotNull
    @PastOrPresent(message = "Installation date cannot be in the future")
    @Schema(example = "2024-01-15")
    private LocalDate installationDate;

    @Schema(description = "ACTIVE or INACTIVE", example = "ACTIVE")
    private MeterStatus status;
}
