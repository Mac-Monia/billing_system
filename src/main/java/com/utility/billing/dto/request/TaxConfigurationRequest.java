package com.utility.billing.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Schema(description = "Additional tax configuration (e.g. environmental levy)")
public class TaxConfigurationRequest {

    @NotBlank(message = "Tax name is required")
    @Schema(example = "Environmental Levy")
    private String name;

    @NotNull(message = "Tax rate is required")
    @DecimalMin(value = "0.0", message = "Tax rate must be between 0 and 100")
    @DecimalMax(value = "100.0", message = "Tax rate must be between 0 and 100")
    @Schema(example = "2.5")
    private BigDecimal rate;

    @NotNull(message = "Effective date is required")
    @FutureOrPresent(message = "Effective date cannot be in the past")
    @Schema(example = "2026-07-01")
    private LocalDate effectiveFrom;
}
