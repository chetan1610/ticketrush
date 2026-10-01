from app.db import pool


def list_movies():
    with pool.connection() as conn:
        return conn.execute(
            "SELECT id,title,language,duration_minutes,certificate"
            "FROM movies ORDER BY title"
        ).fetchall()
        
def get_movie(movie_id):
    with pool.connection() as conn:
        return conn.execute(
            "SELECT id, title, language, duration_minutes, certificate "
            "FROM movies WHERE id = %s",
            (movie_id,),
        ).fetchone()