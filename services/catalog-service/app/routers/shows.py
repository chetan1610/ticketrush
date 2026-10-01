from datetime import date

from fastapi import APIRouter, HTTPException, Query

from app.schemas.shows import ShowPage
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