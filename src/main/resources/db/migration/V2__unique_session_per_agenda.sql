-- Each member may vote only once per AGENDA. Since vote uniqueness is
-- (session_id, member_id), allowing at most one session per agenda makes the rule
-- structural: it prevents reopening an already-closed session, removes the race between
-- two concurrent POST /sessoes and guarantees that tallying (which reads the agenda's
-- session) never discards votes from a previous session.
ALTER TABLE voting_sessions
    ADD CONSTRAINT uk_voting_sessions_agenda UNIQUE (agenda_id);
