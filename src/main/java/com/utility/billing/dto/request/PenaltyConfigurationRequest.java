package com.utility.billing.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PenaltyConfigurationRequest {

    @NotBlank
    private String name;

    @NotNull
    private BigDecimal ratePercent;

    @NotNull
    private Integer graceDays;

    @NotNull
    private LocalDate effectiveFrom;
}
