from app.db import pool

BASE_QUERY = """
    SELECT sh.id, sh.starts_at, sh.price,
           m.id AS movie_id, m.title AS movie_title, m.language,
           v.name AS venue, v.city, sc.name AS screen
    FROM shows sh
    JOIN movies  m  ON m.id  = sh.movie_id
    JOIN screens sc ON sc.id = sh.screen_id
    JOIN venues  v  ON v.id  = sc.venue_id
"""


def list_shows(city, day_start, day_end, limit, after=None):
    sql = BASE_QUERY + """
        WHERE v.city = %(city)s
          AND sh.starts_at >= %(day_start)s
          AND sh.starts_at <  %(day_end)s
          AND sh.starts_at > now()
    """
    params = {"city": city, "day_start": day_start, "day_end": day_end, "limit": limit}

    if after is not None:
        sql += " AND (sh.starts_at, sh.id) > (%(after_starts_at)s, %(after_id)s)"
        params["after_starts_at"], params["after_id"] = after

    sql += " ORDER BY sh.starts_at, sh.id LIMIT %(limit)s"

    with pool.connection() as conn:
        return conn.execute(sql, params).fetchall()


def get_show(show_id):
    with pool.connection() as conn:
        return conn.execute(
            BASE_QUERY + " WHERE sh.id = %(id)s", {"id": show_id}
        ).fetchone()


def create_show(movie_id, screen_id, starts_at, price):
    with pool.connection() as conn:
        row = conn.execute(
            """
            INSERT INTO shows (movie_id, screen_id, starts_at, price)
            VALUES (%(movie_id)s, %(screen_id)s, %(starts_at)s, %(price)s)
            RETURNING id
            """,
            {"movie_id": movie_id, "screen_id": screen_id,
             "starts_at": starts_at, "price": price},
        ).fetchone()
    return row["id"]