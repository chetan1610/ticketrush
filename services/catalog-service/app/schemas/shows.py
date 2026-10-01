from datetime import datetime
from decimal import Decimal

from pydantic import BaseModel
from pydantic import AwareDatetime, BaseModel, Field

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
    
class ShowCreate(BaseModel):
    movie_id: int
    screen_id: int
    starts_at: AwareDatetime
    price: Decimal = Field(ge=0, max_digits=8, decimal_places=2)