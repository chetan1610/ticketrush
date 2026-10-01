from app.repositories import movies as movie_repo


class MovieNotFound(Exception):
    pass


def list_movies():
    return movie_repo.list_movies()


def get_movie(movie_id):
    movie = movie_repo.get_movie(movie_id)
    if movie is None:
        raise MovieNotFound()
    return movie