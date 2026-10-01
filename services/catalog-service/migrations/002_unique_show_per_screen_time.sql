BEGIN;

ALTER TABLE shows
    ADD CONSTRAINT shows_screen_id_starts_at_key UNIQUE (screen_id, starts_at);

COMMIT;