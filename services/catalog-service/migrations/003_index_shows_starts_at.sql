BEGIN;

CREATE INDEX shows_starts_at_id_idx ON shows (starts_at, id);

COMMIT;