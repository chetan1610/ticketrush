CREATE TABLE payments (
    id               TEXT PRIMARY KEY,
    user_id          BIGINT NOT NULL,
    booking_id       BIGINT NOT NULL,
    idempotency_key  TEXT NOT NULL,
    amount           NUMERIC(10,2) NOT NULL CHECK (amount >= 0),
    status           TEXT NOT NULL
                     CHECK (status IN ('PROCESSING', 'FAILED', 'CHARGED', 'SUCCEEDED', 'REFUNDED')),
    failure_reason   TEXT,
    notified         BOOLEAN NOT NULL DEFAULT false,
    created_at       TIMESTAMPTZ NOT NULL,
    updated_at       TIMESTAMPTZ NOT NULL,
    -- the same client retry (same key) must never charge twice
    UNIQUE (user_id, idempotency_key)
);

-- at most one live/successful payment per booking
CREATE UNIQUE INDEX payments_one_success_per_booking
    ON payments (booking_id) WHERE status IN ('PROCESSING', 'CHARGED', 'SUCCEEDED');

CREATE INDEX payments_needs_followup_idx ON payments (id)
    WHERE status = 'CHARGED' OR (status = 'SUCCEEDED' AND NOT notified);
