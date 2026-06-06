-- PostgreSQL database routines for Utility Billing System
-- Message format:
-- Dear <CustomerName>,
-- Your <Month/Year> utility bill of <Amount> FRW has been successfully processed.

-- Shared helper: billing period label (e.g. "June 2026")
CREATE OR REPLACE FUNCTION format_billing_period(p_month INTEGER, p_year INTEGER)
RETURNS TEXT
LANGUAGE plpgsql
AS $$
BEGIN
    RETURN TRIM(TO_CHAR(MAKE_DATE(p_year, p_month, 1), 'TMMonth YYYY'));
END;
$$;

-- Shared helper: notification message body
CREATE OR REPLACE FUNCTION build_utility_bill_message(
    p_customer_id BIGINT,
    p_billing_month INTEGER,
    p_billing_year INTEGER,
    p_amount NUMERIC
)
RETURNS TEXT
LANGUAGE plpgsql
AS $$
DECLARE
    v_customer_name TEXT;
    v_period TEXT;
BEGIN
    SELECT TRIM(first_name || ' ' || last_name)
    INTO v_customer_name
    FROM customers
    WHERE id = p_customer_id;

    v_period := format_billing_period(p_billing_month, p_billing_year);

    RETURN 'Dear ' || v_customer_name || E',\nYour ' || v_period
        || ' utility bill of ' || p_amount || ' FRW has been successfully processed.';
END;
$$;

-- Trigger: notify customer when a bill is generated (INSERT)
CREATE OR REPLACE FUNCTION notify_on_bill_generation()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO notifications (customer_id, message, created_at, is_read)
    VALUES (
        NEW.customer_id,
        build_utility_bill_message(NEW.customer_id, NEW.billing_month, NEW.billing_year, NEW.total_amount),
        NOW(),
        false
    );

    UPDATE bills SET notification_sent = true WHERE id = NEW.id;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_bill_notification ON bills;
DROP TRIGGER IF EXISTS trg_bill_approval_notification ON bills;
DROP TRIGGER IF EXISTS trg_bill_generation_notification ON bills;
CREATE TRIGGER trg_bill_generation_notification
    AFTER INSERT ON bills
    FOR EACH ROW
    EXECUTE PROCEDURE notify_on_bill_generation();

-- Trigger: notify customer when bill status becomes PAID (full payment via application layer)
CREATE OR REPLACE FUNCTION notify_on_bill_paid()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.status = 'PAID'
       AND (OLD.status IS DISTINCT FROM NEW.status)
       AND OLD.status IN ('APPROVED', 'PARTIALLY_PAID', 'OVERDUE') THEN
        INSERT INTO notifications (customer_id, message, created_at, is_read)
        VALUES (
            NEW.customer_id,
            build_utility_bill_message(NEW.customer_id, NEW.billing_month, NEW.billing_year, NEW.total_amount),
            NOW(),
            false
        );
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_bill_paid_notification ON bills;
CREATE TRIGGER trg_bill_paid_notification
    AFTER UPDATE ON bills
    FOR EACH ROW
    EXECUTE PROCEDURE notify_on_bill_paid();

-- Stored procedure: record payment, update bill status, notify on full settlement (uses a cursor)
-- Full-payment notification is created by trg_bill_paid_notification when status becomes PAID.
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
    bill_cursor CURSOR FOR
        SELECT id, customer_id, billing_month, billing_year, total_amount, outstanding_balance, status
        FROM bills
        WHERE id = p_bill_id
        FOR UPDATE;

    v_bill_id BIGINT;
    v_customer_id BIGINT;
    v_billing_month INTEGER;
    v_billing_year INTEGER;
    v_total NUMERIC;
    v_outstanding NUMERIC;
    v_status VARCHAR(50);
    v_new_outstanding NUMERIC;
BEGIN
    OPEN bill_cursor;
    FETCH bill_cursor INTO v_bill_id, v_customer_id, v_billing_month, v_billing_year,
        v_total, v_outstanding, v_status;
    IF NOT FOUND THEN
        CLOSE bill_cursor;
        RAISE EXCEPTION 'Bill not found: %', p_bill_id;
    END IF;

    IF v_status NOT IN ('APPROVED', 'PARTIALLY_PAID', 'OVERDUE') THEN
        CLOSE bill_cursor;
        RAISE EXCEPTION 'Payments can only be recorded for approved bills';
    END IF;

    IF p_amount > v_outstanding THEN
        CLOSE bill_cursor;
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
            WHEN v_new_outstanding > 0 THEN 'PARTIALLY_PAID'
            ELSE status
        END
    WHERE id = p_bill_id;

    CLOSE bill_cursor;
    -- Full-payment notification is created by trg_bill_paid_notification when status becomes PAID
END;
$$;

-- Remove legacy payment-insert trigger (bill status is updated before notification via bill UPDATE trigger)
DROP TRIGGER IF EXISTS trg_full_payment_notification ON payments;
DROP FUNCTION IF EXISTS notify_on_full_payment();
DROP FUNCTION IF EXISTS notify_on_bill_approval();
