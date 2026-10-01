from datetime import datetime
from decimal import Decimal

from pydantic import BaseModel


class ShowOut(BaseModel):
    id: int
    movie_id: int
    movie_title: str
    language: str
    venue: str
    city: str
    screen: str
    starts_at: datetime
    price: Decimal


class ShowPage(BaseModel):
    items: list[ShowOut]
    next_cursor: str | None