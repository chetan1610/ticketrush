BEGIN;

TRUNCATE movies, venues, screens, seats, shows RESTART IDENTITY CASCADE;

INSERT INTO movies (title, language, duration_minutes, certificate) VALUES
    ('Inception',                   'English',   148, 'UA'),
    ('Interstellar',                'English',   169, 'UA'),
    ('The Dark Knight',             'English',   152, 'UA'),
    ('Oppenheimer',                 'English',   180, 'UA'),
    ('RRR',                         'Telugu',    187, 'UA'),
    ('Baahubali 2: The Conclusion', 'Telugu',    167, 'UA'),
    ('Kalki 2898 AD',               'Telugu',    181, 'UA'),
    ('Pushpa 2: The Rule',          'Telugu',    200, 'UA'),
    ('3 Idiots',                    'Hindi',     170, 'U'),
    ('Dangal',                      'Hindi',     161, 'U'),
    ('Jawan',                       'Hindi',     169, 'UA'),
    ('Animal',                      'Hindi',     201, 'A'),
    ('Vikram',                      'Tamil',     174, 'UA'),
    ('KGF: Chapter 2',              'Kannada',   168, 'UA'),
    ('Manjummel Boys',              'Malayalam', 135, 'UA');

INSERT INTO venues (name, city) VALUES
    ('PVR Nexus',          'Hyderabad'),
    ('INOX GVK One',       'Hyderabad'),
    ('AMB Cinemas',        'Hyderabad'),
    ('PVR Phoenix',        'Bengaluru'),
    ('INOX Garuda',        'Bengaluru'),
    ('PVR Grand Galada',   'Chennai'),
    ('PVR Sathyam',        'Chennai'),
    ('INOX Nariman Point', 'Mumbai');



INSERT INTO screens(venue_id,name)
SELECT v.id,'screen' || n
FROM venues v
CROSS JOIN generate_series(1,3) AS n
ORDER BY v.id,n;


INSERT INTO seats (screen_id,row_label,seat_number)
SELECT s.id,chr(64+r),n
FROM screens s
CROSS JOIN generate_series(1,8) AS r
CROSS JOIN generate_series(1,12) AS n
ORDER BY s.id,r,n;


INSERT INTO shows (movie_id, screen_id, starts_at, price)
SELECT
    1 + (sc.id + d + t.slot) % 15,
    sc.id,
    (((now() AT TIME ZONE 'Asia/Kolkata')::date + d) + t.show_time) AT TIME ZONE 'Asia/Kolkata',
    t.price
FROM screens sc
CROSS JOIN generate_series(0, 6) AS d
CROSS JOIN (VALUES
    (1, TIME '10:00', 150.00),
    (2, TIME '13:30', 200.00),
    (3, TIME '17:00', 250.00),
    (4, TIME '20:30', 300.00)
) AS t(slot, show_time, price)
ORDER BY d, t.slot, sc.id;


COMMIT;