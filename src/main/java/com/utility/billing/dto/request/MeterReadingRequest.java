package com.utility.billing.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Schema(description = "Meter reading capture payload")
public class MeterReadingRequest {

    @NotNull(message = "Meter ID is required")
    @Schema(description = "ID of the meter being read", example = "1")
    private Long meterId;

    @NotNull(message = "Previous reading is required")
    @DecimalMin(value = "0", inclusive = true, message = "Previous reading cannot be negative")
    @Schema(example = "500.00")
    private BigDecimal previousReading;

    @NotNull(message = "Current reading is required")
    @DecimalMin(value = "0", inclusive = true, message = "Current reading cannot be negative")
    @Schema(example = "650.00")
    private BigDecimal currentReading;

    @NotNull(message = "Reading date is required")
    @PastOrPresent(message = "Reading date cannot be in the future")
    @Schema(example = "2026-05-01")
    private LocalDate readingDate;
}
