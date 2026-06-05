package com.utility.billing.dto.request;

import com.utility.billing.enums.MeterType;
import com.utility.billing.enums.TariffType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Schema(description = "Tariff configuration payload")
public class TariffRequest {

    @NotNull
    private MeterType meterType;

    @NotNull
    private TariffType tariffType;

    @DecimalMin(value = "0.0001", message = "Price per unit must be greater than 0")
    @Schema(example = "250.00")
    private BigDecimal ratePerUnit;

    @NotNull
    @DecimalMin(value = "0.0", message = "Service charge must be >= 0")
    @Schema(example = "1500.00")
    private BigDecimal fixedCharge;

    @NotNull
    @DecimalMin(value = "0.0", message = "VAT must be between 0 and 100")
    @DecimalMax(value = "100.0", message = "VAT must be between 0 and 100")
    @Schema(example = "18.0")
    private BigDecimal vatPercent;

    @NotNull
    @DecimalMin(value = "0.0", message = "Late penalty must be >= 0")
    @Schema(example = "5.0")
    private BigDecimal latePenaltyPercent;

    @NotNull
    @FutureOrPresent(message = "Effective date cannot be in the past")
    @Schema(example = "2026-06-01")
    private LocalDate effectiveFrom;

    private List<TariffTierRequest> tiers;
}
