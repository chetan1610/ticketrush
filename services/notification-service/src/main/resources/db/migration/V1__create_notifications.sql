CREATE TABLE notifications (
    id          BIGSERIAL PRIMARY KEY,
    booking_id  BIGINT NOT NULL UNIQUE,
    email       TEXT NOT NULL,
    subject     TEXT NOT NULL,
    body        TEXT NOT NULL,
    status      TEXT NOT NULL CHECK (status IN ('PENDING', 'SENT', 'FAILED')),
    attempts    INT NOT NULL DEFAULT 0,
    last_error  TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    sent_at     TIMESTAMPTZ
);

CREATE INDEX notifications_unsent_idx ON notifications (id) WHERE status <> 'SENT';
