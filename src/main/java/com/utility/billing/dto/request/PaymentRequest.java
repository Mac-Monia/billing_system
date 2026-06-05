package com.utility.billing.dto.request;

import com.utility.billing.enums.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Schema(description = "Payment recording payload")
public class PaymentRequest {

    @NotBlank
    @Schema(example = "BILL-A1B2C3D4")
    private String billReference;

    @NotNull
    @Positive(message = "Payment amount must be greater than 0")
    @Schema(example = "15000.00")
    private BigDecimal amount;

    @NotNull
    @Schema(example = "MOMO")
    private PaymentMethod paymentMethod;

    @NotNull
    @PastOrPresent(message = "Payment date cannot be in the future")
    @Schema(example = "2026-06-05")
    private LocalDate paymentDate;
}
