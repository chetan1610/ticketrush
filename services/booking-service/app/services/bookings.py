from datetime import datetime, timezone
from decimal import Decimal

from psycopg import errors

from app.clients import catalog
from app.config import settings
from app.repositories import bookings as booking_repo


class ShowNotFound(Exception):
    pass


class ShowStarted(Exception):
    pass


class UnknownSeats(Exception):
    pass


class SeatsTaken(Exception):
    pass


class BookingNotFound(Exception):
    pass


class NotPending(Exception):
    pass


def seat_map(show_id):
    seats = catalog.get_seats(show_id)
    if seats is None:
        raise ShowNotFound()
    taken = booking_repo.taken_seat_ids(show_id)
    return [{**seat, "available": seat["id"] not in taken} for seat in seats]


def create_booking(user, data):
    show = catalog.get_show(data.show_id)
    seats = catalog.get_seats(data.show_id)
    if show is None or seats is None:
        raise ShowNotFound()
    if datetime.fromisoformat(show["starts_at"]) <= datetime.now(timezone.utc):
        raise ShowStarted()

    seats_by_id = {seat["id"]: seat for seat in seats}
    unknown = [seat_id for seat_id in data.seat_ids if seat_id not in seats_by_id]
    if unknown:
        raise UnknownSeats(unknown)

    seat_ids = sorted(data.seat_ids)
    labels = [f"{seats_by_id[i]['row_label']}{seats_by_id[i]['seat_number']}" for i in seat_ids]
    amount = Decimal(show["price"]) * len(seat_ids)

    try:
        return booking_repo.create_booking(
            user.id, user.email, data.show_id, seat_ids, labels, amount, settings.hold_minutes
        )
    except errors.UniqueViolation:
        raise SeatsTaken()


def list_bookings(user):
    return booking_repo.list_for_user(user.id)


def get_booking(booking_id, user_id=None):
    booking = booking_repo.get_booking(booking_id)
    if booking is None or (user_id is not None and booking["user_id"] != user_id):
        raise BookingNotFound()
    return booking


def cancel_booking(booking_id, user):
    booking = get_booking(booking_id, user.id)
    cancelled = booking_repo.cancel_pending(booking["id"], user.id)
    if cancelled is None:
        raise NotPending(booking["status"])
    return cancelled


def confirm_booking(booking_id, payment_id):
    booking = get_booking(booking_id)
    if booking["status"] == "CONFIRMED" and booking["payment_id"] == payment_id:
        return booking  # payment-service retried; same answer again
    confirmed = booking_repo.confirm_pending(booking_id, payment_id)
    if confirmed is None:
        raise NotPending(get_booking(booking_id)["status"])
    return confirmed
