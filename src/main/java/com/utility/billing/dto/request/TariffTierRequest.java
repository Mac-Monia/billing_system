package com.utility.billing.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TariffTierRequest {

    @NotNull
    private BigDecimal minConsumption;

    private BigDecimal maxConsumption;

    @NotNull
    private BigDecimal ratePerUnit;
}
