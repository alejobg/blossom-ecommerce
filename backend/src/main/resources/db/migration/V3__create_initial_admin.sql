-- Initial back-office account, so the catalog can be managed right after the first start.
-- Credentials: admin@blossom.com / Admin123!  (BCrypt, cost 10). Change them outside a local/demo setup.
INSERT INTO users (role, name, email, password_hash, created_at, updated_at)
VALUES ('ADMIN', 'Blossom Admin', 'admin@blossom.com',
        '$2a$10$BbiGOJdZSbpP06DQ3bxGB.7YGCrsEq9sCgs.q4ewiXGRHs0aAc/YO', NOW(), NOW());
