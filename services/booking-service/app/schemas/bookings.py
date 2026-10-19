from datetime import datetime
from decimal import Decimal
from typing import Literal

from pydantic import BaseModel, Field, field_validator


class SeatStatus(BaseModel):
    id: int
    row_label: str
    seat_number: int
    available: bool


class BookingCreate(BaseModel):
    show_id: int
    seat_ids: list[int] = Field(min_length=1, max_length=10)

    @field_validator("seat_ids")
    @classmethod
    def no_duplicates(cls, value):
        if len(set(value)) != len(value):
            raise ValueError("seat_ids must be unique")
        return value


class BookingOut(BaseModel):
    id: int
    show_id: int
    seat_labels: list[str]
    amount: Decimal
    status: Literal["PENDING", "CONFIRMED", "CANCELLED", "EXPIRED"]
    payment_id: str | None
    created_at: datetime
    expires_at: datetime


class InternalBookingOut(BookingOut):
    user_id: int
    user_email: str


class ConfirmRequest(BaseModel):
    payment_id: str = Field(min_length=1)
