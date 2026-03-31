-- Seed data for evaluator convenience.
-- Plaintext API key (documented in README): test-api-key-001

INSERT INTO app_user (id, created_at)
VALUES ('11111111-1111-1111-1111-111111111111', CURRENT_TIMESTAMP);

INSERT INTO api_key (id, user_id, api_key_hash, created_at, revoked_at)
VALUES (
  '22222222-2222-2222-2222-222222222222',
  '11111111-1111-1111-1111-111111111111',
  '$2b$10$3.KXYmPLasY./slG6T4oKOq20WorvFbb3hCm9vsk8TFQpn50IC8K6',
  CURRENT_TIMESTAMP,
  NULL
);

