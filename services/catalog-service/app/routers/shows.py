from datetime import date

from fastapi import APIRouter, HTTPException, Query

from app.schemas.shows import ShowCreate, ShowOut, ShowPage
from app.services import shows as show_service

router = APIRouter(prefix="/shows", tags=["shows"])


@router.get("", response_model=ShowPage)
def list_shows(
    city: str,
    day: date | None = None,
    limit: int = Query(20, ge=1, le=100),
    cursor: str | None = None,
):
    try:
        return show_service.list_shows(city, day, limit, cursor)
    except show_service.InvalidCursor:
        raise HTTPException(status_code=400, detail="invalid cursor")


@router.get("/{show_id}", response_model=ShowOut)
def get_show(show_id: int):
    try:
        return show_service.get_show(show_id)
    except show_service.ShowNotFound:
        raise HTTPException(status_code=404, detail="show not found")


@router.post("", response_model=ShowOut, status_code=201)
def create_show(data: ShowCreate):
    try:
        return show_service.create_show(data)
    except show_service.ShowInPast:
        raise HTTPException(status_code=400, detail="show must start in the future")
    except show_service.UnknownMovieOrScreen:
        raise HTTPException(status_code=422, detail="movie or screen does not exist")
    except show_service.ShowSlotTaken:
        raise HTTPException(status_code=409, detail="this screen already has a show at that time")