-- Migrate legacy ROLE_* enum values to ADMIN, OPERATOR, FINANCE, CUSTOMER
ALTER TABLE roles DROP CONSTRAINT IF EXISTS roles_name_check;

UPDATE roles SET name = 'ADMIN' WHERE name = 'ROLE_ADMIN';
UPDATE roles SET name = 'OPERATOR' WHERE name = 'ROLE_OPERATOR';
UPDATE roles SET name = 'FINANCE' WHERE name = 'ROLE_FINANCE';
UPDATE roles SET name = 'CUSTOMER' WHERE name = 'ROLE_CUSTOMER';
