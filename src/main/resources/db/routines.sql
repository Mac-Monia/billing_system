-- PostgreSQL database routines for Utility Billing System

-- Trigger: notification when bill is approved
CREATE OR REPLACE FUNCTION notify_on_bill_approval()
RETURNS TRIGGER AS $$
DECLARE
    customer_name VARCHAR(255);
BEGIN
    IF NEW.status = 'APPROVED' AND (OLD.status IS DISTINCT FROM NEW.status) THEN
        SELECT first_name || ' ' || last_name INTO customer_name FROM customers WHERE id = NEW.customer_id;
        INSERT INTO notifications (customer_id, message, created_at, is_read)
        VALUES (
            NEW.customer_id,
            'Dear ' || customer_name || ', Your utility bill of FRW ' || NEW.total_amount || ' has been successfully processed.',
            NOW(),
            false
        );
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_bill_notification ON bills;
DROP TRIGGER IF EXISTS trg_bill_approval_notification ON bills;
CREATE TRIGGER trg_bill_approval_notification
    AFTER UPDATE ON bills
    FOR EACH ROW
    EXECUTE PROCEDURE notify_on_bill_approval();

-- Stored procedure: process payment and notify on full settlement
CREATE OR REPLACE PROCEDURE process_payment_and_notify(
    p_bill_id BIGINT,
    p_amount NUMERIC,
    p_payment_method VARCHAR(50),
    p_payment_reference VARCHAR(100),
    p_payment_date DATE
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_outstanding NUMERIC;
    v_customer_id BIGINT;
    v_total NUMERIC;
    v_customer_name VARCHAR(255);
    v_new_outstanding NUMERIC;
BEGIN
    SELECT outstanding_balance, customer_id, total_amount
    INTO v_outstanding, v_customer_id, v_total
    FROM bills WHERE id = p_bill_id FOR UPDATE;

    IF v_outstanding IS NULL THEN
        RAISE EXCEPTION 'Bill not found: %', p_bill_id;
    END IF;

    IF p_amount > v_outstanding THEN
        RAISE EXCEPTION 'Payment amount exceeds outstanding balance';
    END IF;

    INSERT INTO payments (payment_reference, bill_id, amount, payment_method, payment_date, recorded_at)
    VALUES (p_payment_reference, p_bill_id, p_amount, p_payment_method, p_payment_date, NOW());

    v_new_outstanding := v_outstanding - p_amount;

    UPDATE bills
    SET amount_paid = amount_paid + p_amount,
        outstanding_balance = GREATEST(v_new_outstanding, 0),
        status = CASE
            WHEN v_new_outstanding <= 0 THEN 'PAID'
            WHEN v_new_outstanding > 0 AND v_new_outstanding < total_amount THEN 'PARTIALLY_PAID'
            ELSE status
        END
    WHERE id = p_bill_id;

    IF v_new_outstanding <= 0 THEN
        SELECT first_name || ' ' || last_name INTO v_customer_name FROM customers WHERE id = v_customer_id;
        INSERT INTO notifications (customer_id, message, created_at, is_read)
        VALUES (
            v_customer_id,
            'Dear ' || v_customer_name || ', Your utility bill of FRW ' || v_total || ' has been successfully processed.',
            NOW(),
            false
        );
    END IF;
END;
$$;

-- Trigger: notify on full payment
CREATE OR REPLACE FUNCTION notify_on_full_payment()
RETURNS TRIGGER AS $$
DECLARE
    v_status VARCHAR(50);
    v_customer_id BIGINT;
    v_total NUMERIC;
    v_customer_name VARCHAR(255);
BEGIN
    SELECT status, customer_id, total_amount
    INTO v_status, v_customer_id, v_total
    FROM bills WHERE id = NEW.bill_id;

    IF v_status = 'PAID' THEN
        SELECT first_name || ' ' || last_name INTO v_customer_name FROM customers WHERE id = v_customer_id;
        IF NOT EXISTS (
            SELECT 1 FROM notifications n
            WHERE n.customer_id = v_customer_id
              AND n.message LIKE '%FRW ' || v_total || '%'
              AND n.created_at > NOW() - INTERVAL '1 minute'
        ) THEN
            INSERT INTO notifications (customer_id, message, created_at, is_read)
            VALUES (
                v_customer_id,
                'Dear ' || v_customer_name || ', Your utility bill of FRW ' || v_total || ' has been successfully processed.',
                NOW(),
                false
            );
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_full_payment_notification ON payments;
CREATE TRIGGER trg_full_payment_notification
    AFTER INSERT ON payments
    FOR EACH ROW
    EXECUTE PROCEDURE notify_on_full_payment();
