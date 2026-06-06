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

    @NotBlank(message = "Bill reference is required")
    @Schema(description = "Bill number (e.g. BILL-A1B2C3D4)", example = "BILL-A1B2C3D4")
    private String billReference;

    @NotNull(message = "Payment amount is required")
    @Positive(message = "Payment amount must be greater than 0")
    @Schema(description = "Amount paid toward the bill", example = "15000.00")
    private BigDecimal amount;

    @NotNull(message = "Payment method is required")
    @Schema(description = "MOMO, BANK, CARD, or CASH", example = "MOMO")
    private PaymentMethod paymentMethod;

    @NotNull(message = "Payment date is required")
    @PastOrPresent(message = "Payment date cannot be in the future")
    @Schema(example = "2026-06-05")
    private LocalDate paymentDate;
}
