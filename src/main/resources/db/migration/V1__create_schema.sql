CREATE TABLE agendas (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE voting_sessions (
    id BIGSERIAL PRIMARY KEY,
    agenda_id BIGINT NOT NULL REFERENCES agendas(id),
    opened_at TIMESTAMP NOT NULL,
    closes_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_voting_sessions_agenda_id ON voting_sessions(agenda_id);

CREATE TABLE votes (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL REFERENCES voting_sessions(id),
    member_id VARCHAR(64) NOT NULL,
    vote VARCHAR(3) NOT NULL CHECK (vote IN ('YES', 'NO')),
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_votes_session_member UNIQUE (session_id, member_id)
);

CREATE INDEX idx_votes_session_id ON votes(session_id);
