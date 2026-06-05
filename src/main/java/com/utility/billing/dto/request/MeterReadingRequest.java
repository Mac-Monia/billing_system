package com.utility.billing.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Schema(description = "Meter reading capture payload")
public class MeterReadingRequest {

    @NotNull
    private Long meterId;

    @NotNull
    @Schema(example = "500.00")
    private BigDecimal previousReading;

    @NotNull
    @Schema(example = "650.00")
    private BigDecimal currentReading;

    @NotNull
    @PastOrPresent(message = "Reading date cannot be in the future")
    @Schema(example = "2026-05-01")
    private LocalDate readingDate;
}
