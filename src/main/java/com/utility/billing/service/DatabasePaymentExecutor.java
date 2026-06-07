package com.utility.billing.service;

import com.utility.billing.enums.PaymentMethod;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.CallableStatement;
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
@Slf4j
public class DatabasePaymentExecutor {

    private static final String PROCEDURE_CALL =
            "{call process_payment_and_notify(?, ?, ?, ?, ?)}";

    private final JdbcTemplate jdbcTemplate;

    public void processPayment(Long billId,
                               BigDecimal amount,
                               PaymentMethod paymentMethod,
                               String paymentReference,
                               LocalDate paymentDate) {
        jdbcTemplate.execute((java.sql.Connection connection) -> {
            try (CallableStatement statement = connection.prepareCall(PROCEDURE_CALL)) {
                statement.setLong(1, billId);
                statement.setBigDecimal(2, amount);
                statement.setString(3, paymentMethod.name());
                statement.setString(4, paymentReference);
                statement.setDate(5, Date.valueOf(paymentDate));
                statement.execute();
            }
            return null;
        });
        log.debug("Stored procedure process_payment_and_notify completed for bill {}", billId);
    }

    public boolean isProcedureAvailable() {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    """
                    SELECT COUNT(*) FROM pg_proc p
                    JOIN pg_namespace n ON p.pronamespace = n.oid
                    WHERE p.proname = 'process_payment_and_notify'
                      AND n.nspname = 'public'
                    """,
                    Integer.class);
            return count != null && count > 0;
        } catch (Exception ex) {
            log.warn("Could not verify stored procedure availability: {}", ex.getMessage());
            return false;
        }
    }
}
