-- The entities map these columns to java.time.Instant. As TIMESTAMP (without time zone) the
-- stored value depended on how each writer rendered the instant (Hibernate in UTC, plain JDBC in
-- the JVM's zone), so comparing them could be off by the zone offset. TIMESTAMPTZ stores the
-- absolute instant. Existing values were written by the app running in UTC.
ALTER TABLE agendas
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC';

ALTER TABLE voting_sessions
    ALTER COLUMN opened_at TYPE TIMESTAMPTZ USING opened_at AT TIME ZONE 'UTC',
    ALTER COLUMN closes_at TYPE TIMESTAMPTZ USING closes_at AT TIME ZONE 'UTC';

ALTER TABLE votes
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC';
