-- Mark existing user as super admin and ensure account is enabled (run after creating user e.g. via register with username 'admin')
-- Also sets enabled = 1 so the account can log in; run this again if you see "Account is disabled".
-- Usage: mysql -u redlining -p redlining < scripts/create-super-admin.sql

UPDATE users SET is_super_admin = 1, enabled = 1 WHERE username = 'admin';
