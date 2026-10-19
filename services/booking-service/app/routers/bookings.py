from fastapi import APIRouter, Depends, HTTPException

from app.auth import CurrentUser, current_user, internal_only
from app.clients.catalog import CatalogUnavailable
from app.schemas.bookings import (
    BookingCreate,
    BookingOut,
    ConfirmRequest,
    InternalBookingOut,
    SeatStatus,
)
from app.services import bookings as booking_service

router = APIRouter(tags=["bookings"])


def catalog_down():
    return HTTPException(status_code=503, detail="catalog service unavailable")


@router.get("/shows/{show_id}/seats", response_model=list[SeatStatus])
def seat_map(show_id: int):
    try:
        return booking_service.seat_map(show_id)
    except booking_service.ShowNotFound:
        raise HTTPException(status_code=404, detail="show not found")
    except CatalogUnavailable:
        raise catalog_down()


@router.post("/bookings", response_model=BookingOut, status_code=201)
def create_booking(data: BookingCreate, user: CurrentUser = Depends(current_user)):
    try:
        return booking_service.create_booking(user, data)
    except booking_service.ShowNotFound:
        raise HTTPException(status_code=404, detail="show not found")
    except booking_service.ShowStarted:
        raise HTTPException(status_code=400, detail="show has already started")
    except booking_service.UnknownSeats as e:
        raise HTTPException(status_code=422, detail=f"seats not in this show: {e.args[0]}")
    except booking_service.SeatsTaken:
        raise HTTPException(status_code=409, detail="one or more seats are already taken")
    except CatalogUnavailable:
        raise catalog_down()


@router.get("/bookings", response_model=list[BookingOut])
def list_bookings(user: CurrentUser = Depends(current_user)):
    return booking_service.list_bookings(user)


@router.get("/bookings/{booking_id}", response_model=BookingOut)
def get_booking(booking_id: int, user: CurrentUser = Depends(current_user)):
    try:
        return booking_service.get_booking(booking_id, user.id)
    except booking_service.BookingNotFound:
        raise HTTPException(status_code=404, detail="booking not found")


@router.post("/bookings/{booking_id}/cancel", response_model=BookingOut)
def cancel_booking(booking_id: int, user: CurrentUser = Depends(current_user)):
    try:
        return booking_service.cancel_booking(booking_id, user)
    except booking_service.BookingNotFound:
        raise HTTPException(status_code=404, detail="booking not found")
    except booking_service.NotPending as e:
        raise HTTPException(status_code=409, detail=f"booking is {e.args[0]}, cannot cancel")


internal = APIRouter(prefix="/internal", tags=["internal"], dependencies=[Depends(internal_only)])


@internal.get("/bookings/{booking_id}", response_model=InternalBookingOut)
def internal_get_booking(booking_id: int):
    try:
        return booking_service.get_booking(booking_id)
    except booking_service.BookingNotFound:
        raise HTTPException(status_code=404, detail="booking not found")


@internal.post("/bookings/{booking_id}/confirm", response_model=InternalBookingOut)
def internal_confirm_booking(booking_id: int, data: ConfirmRequest):
    try:
        return booking_service.confirm_booking(booking_id, data.payment_id)
    except booking_service.BookingNotFound:
        raise HTTPException(status_code=404, detail="booking not found")
    except booking_service.NotPending as e:
        raise HTTPException(status_code=409, detail=f"booking is {e.args[0]}, cannot confirm")
