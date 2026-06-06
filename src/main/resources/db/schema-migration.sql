-- Upgrade legacy schema to current entity model (idempotent)

-- Users: add new columns with safe defaults for existing rows
ALTER TABLE users ADD COLUMN IF NOT EXISTS first_name VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS last_name VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS account_locked BOOLEAN DEFAULT false;
ALTER TABLE users ADD COLUMN IF NOT EXISTS password_expired BOOLEAN DEFAULT false;
ALTER TABLE users ADD COLUMN IF NOT EXISTS force_password_change BOOLEAN DEFAULT false;
ALTER TABLE users ADD COLUMN IF NOT EXISTS seeded_admin BOOLEAN DEFAULT false;
ALTER TABLE users ADD COLUMN IF NOT EXISTS email_verified BOOLEAN DEFAULT false;
ALTER TABLE users ADD COLUMN IF NOT EXISTS email_verification_token VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS email_verification_expiry TIMESTAMP;

UPDATE users SET first_name = 'User' WHERE first_name IS NULL;
UPDATE users SET last_name = 'Account' WHERE last_name IS NULL;
UPDATE users SET account_locked = false WHERE account_locked IS NULL;
UPDATE users SET password_expired = false WHERE password_expired IS NULL;
UPDATE users SET force_password_change = false WHERE force_password_change IS NULL;
UPDATE users SET seeded_admin = false WHERE seeded_admin IS NULL;
UPDATE users SET email_verified = true WHERE email_verified IS NULL;

ALTER TABLE users ALTER COLUMN first_name SET NOT NULL;
ALTER TABLE users ALTER COLUMN last_name SET NOT NULL;
ALTER TABLE users ALTER COLUMN account_locked SET NOT NULL;
ALTER TABLE users ALTER COLUMN password_expired SET NOT NULL;
ALTER TABLE users ALTER COLUMN force_password_change SET NOT NULL;
ALTER TABLE users ALTER COLUMN seeded_admin SET NOT NULL;
ALTER TABLE users ALTER COLUMN email_verified SET NOT NULL;

-- Customers: migrate full_names to first_name / last_name when present
ALTER TABLE customers ADD COLUMN IF NOT EXISTS first_name VARCHAR(255);
ALTER TABLE customers ADD COLUMN IF NOT EXISTS last_name VARCHAR(255);
ALTER TABLE customers ADD COLUMN IF NOT EXISTS date_of_birth DATE;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'customers' AND column_name = 'full_names'
    ) THEN
        UPDATE customers
        SET first_name = COALESCE(first_name, split_part(full_names, ' ', 1)),
            last_name = COALESCE(last_name, NULLIF(trim(substring(full_names from position(' ' in full_names))), ''), full_names)
        WHERE first_name IS NULL OR last_name IS NULL;
    END IF;
END $$;

UPDATE customers SET first_name = 'Customer' WHERE first_name IS NULL;
UPDATE customers SET last_name = 'Account' WHERE last_name IS NULL;

ALTER TABLE customers ALTER COLUMN first_name SET NOT NULL;
ALTER TABLE customers ALTER COLUMN last_name SET NOT NULL;

-- Bill status: legacy UNPAID renamed to PENDING; OVERDUE added
UPDATE bills SET status = 'PENDING' WHERE status = 'UNPAID';
