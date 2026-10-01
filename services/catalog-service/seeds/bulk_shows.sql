INSERT INTO shows (movie_id, screen_id, starts_at, price)
SELECT
    1 + (sc.id + d + t.slot) % 15,
    sc.id,
    (((now() AT TIME ZONE 'Asia/Kolkata')::date + d) + t.show_time) AT TIME ZONE 'Asia/Kolkata',
    t.price
FROM screens sc
CROSS JOIN generate_series(7, 2006) AS d
CROSS JOIN (VALUES
    (1, TIME '10:00', 150.00),
    (2, TIME '13:30', 200.00),
    (3, TIME '17:00', 250.00),
    (4, TIME '20:30', 300.00)
) AS t(slot, show_time, price);

ANALYZE shows;
