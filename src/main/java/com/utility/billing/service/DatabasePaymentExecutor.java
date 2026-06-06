package com.utility.billing.service;

import com.utility.billing.enums.PaymentMethod;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;

/**
 * Invokes the PostgreSQL {@code process_payment_and_notify} stored procedure
 * (cursor-based). Bill status updates and full-payment notifications are
 * handled by database triggers defined in {@code db/routines.sql}.
 */
@Component
@Profile("!test")
@RequiredArgsConstructor
public class DatabasePaymentExecutor {

    private final JdbcTemplate jdbcTemplate;

    public void processPayment(Long billId,
                               BigDecimal amount,
                               PaymentMethod paymentMethod,
                               String paymentReference,
                               LocalDate paymentDate) {
        jdbcTemplate.update(
                "CALL process_payment_and_notify(?, ?, ?, ?, ?)",
                billId,
                amount,
                paymentMethod.name(),
                paymentReference,
                Date.valueOf(paymentDate));
    }
}
