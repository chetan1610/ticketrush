import base64
from datetime import datetime, time, timedelta, timezone

from app.repositories import shows as show_repo

IST = timezone(timedelta(hours=5, minutes=30))


class InvalidCursor(Exception):
    pass


def encode_cursor(starts_at, show_id):
    raw = f"{starts_at.isoformat()}|{show_id}"
    return base64.urlsafe_b64encode(raw.encode()).decode()


def decode_cursor(cursor):
    try:
        raw = base64.urlsafe_b64decode(cursor.encode()).decode()
        starts_at, show_id = raw.split("|")
        return datetime.fromisoformat(starts_at), int(show_id)
    except ValueError:
        raise InvalidCursor()


def list_shows(city, day, limit, cursor):
    if day is None:
        day = datetime.now(IST).date()
    day_start = datetime.combine(day, time.min, tzinfo=IST)
    day_end = day_start + timedelta(days=1)

    after = decode_cursor(cursor) if cursor else None

    rows = show_repo.list_shows(city, day_start, day_end, limit + 1, after)

    has_more = len(rows) > limit
    items = rows[:limit]
    next_cursor = None
    if has_more:
        last = items[-1]
        next_cursor = encode_cursor(last["starts_at"], last["id"])

    return {"items": items, "next_cursor": next_cursor}