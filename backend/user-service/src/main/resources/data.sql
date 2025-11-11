-- Insert initial users with BCrypt hashed passwords
-- Password for admin: Admin123!
-- Password for demo: Demo123!
-- Password for testuser: Test123!

-- Admin user with ROLE_ADMIN
INSERT INTO users (username, email, password, first_name, last_name, enabled, account_non_expired, account_non_locked, credentials_non_expired, created_at, updated_at)
VALUES ('admin', 'admin@ecommerce.com', '$2a$10$qKZYCzqXh5qFwq9qhPj/ReLRZQ5nGYzQz0xH4HzqzQzY6qw9qhPj/', 'Admin', 'User', true, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (username) DO NOTHING;

-- Associate admin role
INSERT INTO user_roles (user_id, role)
SELECT id, 'ROLE_ADMIN' FROM users WHERE username = 'admin'
ON CONFLICT DO NOTHING;

-- Demo customer user with ROLE_CUSTOMER
INSERT INTO users (username, email, password, first_name, last_name, enabled, account_non_expired, account_non_locked, credentials_non_expired, created_at, updated_at)
VALUES ('demo', 'demo@ecommerce.com', '$2a$10$kCZYCzqXh5qFwq9qhPj/ReQRZQ5nGYzQz0xH4HzqzQzY6qw9qhPj/', 'Demo', 'Customer', true, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (username) DO NOTHING;

-- Associate customer role for demo
INSERT INTO user_roles (user_id, role)
SELECT id, 'ROLE_CUSTOMER' FROM users WHERE username = 'demo'
ON CONFLICT DO NOTHING;

-- Test customer user with ROLE_CUSTOMER
INSERT INTO users (username, email, password, first_name, last_name, enabled, account_non_expired, account_non_locked, credentials_non_expired, created_at, updated_at)
VALUES ('testuser', 'test@ecommerce.com', '$2a$10$jCZYCzqXh5qFwq9qhPj/RePRZQ5nGYzQz0xH4HzqzQzY6qw9qhPj/', 'Test', 'User', true, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (username) DO NOTHING;

-- Associate customer role for testuser
INSERT INTO user_roles (user_id, role)
SELECT id, 'ROLE_CUSTOMER' FROM users WHERE username = 'testuser'
ON CONFLICT DO NOTHING;
