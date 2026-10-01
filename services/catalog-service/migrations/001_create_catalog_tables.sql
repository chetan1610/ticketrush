BEGIN;
CREATE TABLE movies(
    id  SERIAL PRIMARY KEY,
    title TEXT NOT NULL,
    language TEXT NOT NULL,
    duration_minutes INT NOT NULL CHECK (duration_minutes>0),
    certificate TEXT NOT NULL CHECK (certificate IN ('U','UA','A'))
);



CREATE TABLE venues(
    id SERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    city TEXT NOT NULL
);


CREATE TABLE screens(
    id SERIAL PRIMARY KEY,
    venue_id INT NOT NULL REFERENCES venues(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    UNIQUE (venue_id,name)
);


CREATE TABLE seats(
    id SERIAL PRIMARY KEY,
    screen_id INT NOT NULL REFERENCES screens(id) ON DELETE CASCADE,
    row_label TEXT NOT NULL,
    seat_number INT NOT NULL CHECK (seat_number > 0),
    UNIQUE (screen_id,row_label,seat_number)
);

CREATE TABLE shows (
    id         SERIAL PRIMARY KEY,
    movie_id   INT NOT NULL REFERENCES movies(id),
    screen_id  INT NOT NULL REFERENCES screens(id),
    starts_at  TIMESTAMPTZ NOT NULL,
    price      NUMERIC(8,2) NOT NULL CHECK (price >=0)
);
COMMIT;