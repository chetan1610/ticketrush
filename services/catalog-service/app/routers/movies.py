from fastapi import APIRouter, HTTPException

from app.schemas.movies import MovieOut
from app.services import movies as movie_service

router = APIRouter(prefix="/movies", tags=["movies"])


@router.get("", response_model=list[MovieOut])
def list_movies():
    return movie_service.list_movies()


@router.get("/{movie_id}", response_model=MovieOut)
def get_movie(movie_id: int):
    try:
        return movie_service.get_movie(movie_id)
    except movie_service.MovieNotFound:
        raise HTTPException(status_code=404, detail="movie not found")