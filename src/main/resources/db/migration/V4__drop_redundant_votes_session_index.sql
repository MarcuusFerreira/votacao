-- uk_votes_session_member (session_id, member_id) already serves lookups and the tally by
-- session_id (leading column), so idx_votes_session_id only added write cost to every vote.
DROP INDEX idx_votes_session_id;
