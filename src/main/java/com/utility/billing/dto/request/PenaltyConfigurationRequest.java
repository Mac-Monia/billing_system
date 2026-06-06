package com.utility.billing.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Schema(description = "Late payment penalty configuration")
public class PenaltyConfigurationRequest {

    @NotBlank(message = "Penalty name is required")
    @Schema(example = "Standard Late Fee")
    private String name;

    @NotNull(message = "Penalty rate is required")
    @DecimalMin(value = "0.0", message = "Penalty rate must be >= 0")
    @Schema(example = "5.0")
    private BigDecimal ratePercent;

    @NotNull(message = "Grace days is required")
    @Min(value = 0, message = "Grace days must be >= 0")
    @Schema(example = "7")
    private Integer graceDays;

    @NotNull(message = "Effective date is required")
    @FutureOrPresent(message = "Effective date cannot be in the past")
    @Schema(example = "2026-07-01")
    private LocalDate effectiveFrom;
}
