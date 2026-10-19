BEGIN;

CREATE TABLE bookings (
    id          SERIAL PRIMARY KEY,
    user_id     INT NOT NULL,
    user_email  TEXT NOT NULL,
    show_id     INT NOT NULL,
    seat_labels TEXT[] NOT NULL,
    amount      NUMERIC(10,2) NOT NULL CHECK (amount >= 0),
    status      TEXT NOT NULL DEFAULT 'PENDING'
                CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED', 'EXPIRED')),
    payment_id  TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at  TIMESTAMPTZ NOT NULL
);

CREATE INDEX bookings_user_id_idx ON bookings (user_id, id DESC);
CREATE INDEX bookings_pending_expires_at_idx ON bookings (expires_at) WHERE status = 'PENDING';

-- A seat is taken only while its booking is PENDING or CONFIRMED.
-- Releasing a hold deletes its rows here, so the primary key alone
-- stops two bookings from ever holding the same seat for a show.
CREATE TABLE booking_seats (
    show_id    INT NOT NULL,
    seat_id    INT NOT NULL,
    booking_id INT NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
    PRIMARY KEY (show_id, seat_id)
);

CREATE INDEX booking_seats_booking_id_idx ON booking_seats (booking_id);

COMMIT;
