-- The member is identified by member_id (as the challenge states) and the CPF is what the
-- eligibility check (bonus 1) validates. Storing the CPF with the vote and making it unique per
-- session binds the two: an eligible CPF cannot be reused to vote again under another member_id.
-- Nullable only because votes cast before this migration have no CPF; the API always sends it.
ALTER TABLE votes ADD COLUMN cpf VARCHAR(11);

ALTER TABLE votes ADD CONSTRAINT uk_votes_session_cpf UNIQUE (session_id, cpf);
