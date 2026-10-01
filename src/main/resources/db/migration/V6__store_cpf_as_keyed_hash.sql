-- Votes keep only an HMAC-SHA256 of the CPF (computed by the application with a secret key),
-- which is enough for the one-vote-per-CPF-per-session constraint. The plain CPF column is
-- dropped; votes cast before this migration had their CPF stored in plain text and, since the
-- key is not available to the database, they are left without a hash (NULLs never conflict).
ALTER TABLE votes ADD COLUMN cpf_hash VARCHAR(64);

ALTER TABLE votes DROP CONSTRAINT uk_votes_session_cpf;
ALTER TABLE votes DROP COLUMN cpf;

ALTER TABLE votes ADD CONSTRAINT uk_votes_session_cpf_hash UNIQUE (session_id, cpf_hash);
