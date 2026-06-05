-- Database uniqueness constraints (applied on PostgreSQL startup)
-- Hibernate ddl-auto=update creates columns; these reinforce constraints where needed.

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_customers_national_id') THEN
        ALTER TABLE customers ADD CONSTRAINT uk_customers_national_id UNIQUE (national_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_customers_email') THEN
        ALTER TABLE customers ADD CONSTRAINT uk_customers_email UNIQUE (email);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_customers_phone_number') THEN
        ALTER TABLE customers ADD CONSTRAINT uk_customers_phone_number UNIQUE (phone_number);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_users_email') THEN
        ALTER TABLE users ADD CONSTRAINT uk_users_email UNIQUE (email);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_meters_meter_number') THEN
        ALTER TABLE meters ADD CONSTRAINT uk_meters_meter_number UNIQUE (meter_number);
    END IF;
END $$;
