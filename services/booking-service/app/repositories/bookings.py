from app.db import pool

COLUMNS = """
    id, user_id, user_email, show_id, seat_labels, amount,
    status, payment_id, created_at, expires_at
"""

# Expired holds give their seats back. Run inside the caller's transaction
# so a new booking never races an old hold that just timed out.
EXPIRE_HOLDS = """
    WITH expired AS (
        UPDATE bookings SET status = 'EXPIRED'
        WHERE status = 'PENDING' AND expires_at <= now()
        RETURNING id
    )
    DELETE FROM booking_seats WHERE booking_id IN (SELECT id FROM expired)
"""


def taken_seat_ids(show_id):
    with pool.connection() as conn:
        conn.execute(EXPIRE_HOLDS)
        rows = conn.execute(
            "SELECT seat_id FROM booking_seats WHERE show_id = %s", (show_id,)
        ).fetchall()
    return {r["seat_id"] for r in rows}


def create_booking(user_id, user_email, show_id, seat_ids, seat_labels, amount, hold_minutes):
    """Raises psycopg.errors.UniqueViolation if any seat is already taken."""
    with pool.connection() as conn:
        with conn.transaction():
            conn.execute(EXPIRE_HOLDS)
            booking = conn.execute(
                f"""
                INSERT INTO bookings (user_id, user_email, show_id, seat_labels, amount, expires_at)
                VALUES (%s, %s, %s, %s, %s, now() + make_interval(mins => %s))
                RETURNING {COLUMNS}
                """,
                (user_id, user_email, show_id, seat_labels, amount, hold_minutes),
            ).fetchone()
            with conn.cursor() as cur:
                cur.executemany(
                    "INSERT INTO booking_seats (show_id, seat_id, booking_id) VALUES (%s, %s, %s)",
                    [(show_id, seat_id, booking["id"]) for seat_id in seat_ids],
                )
    return booking


def list_for_user(user_id):
    with pool.connection() as conn:
        return conn.execute(
            f"SELECT {COLUMNS} FROM bookings WHERE user_id = %s ORDER BY id DESC",
            (user_id,),
        ).fetchall()


def get_booking(booking_id):
    with pool.connection() as conn:
        conn.execute(EXPIRE_HOLDS)
        return conn.execute(
            f"SELECT {COLUMNS} FROM bookings WHERE id = %s", (booking_id,)
        ).fetchone()


def cancel_pending(booking_id, user_id):
    with pool.connection() as conn:
        with conn.transaction():
            row = conn.execute(
                f"""
                UPDATE bookings SET status = 'CANCELLED'
                WHERE id = %s AND user_id = %s AND status = 'PENDING'
                RETURNING {COLUMNS}
                """,
                (booking_id, user_id),
            ).fetchone()
            if row is not None:
                conn.execute("DELETE FROM booking_seats WHERE booking_id = %s", (booking_id,))
    return row


def confirm_pending(booking_id, payment_id):
    with pool.connection() as conn:
        return conn.execute(
            f"""
            UPDATE bookings SET status = 'CONFIRMED', payment_id = %s
            WHERE id = %s AND status = 'PENDING' AND expires_at > now()
            RETURNING {COLUMNS}
            """,
            (payment_id, booking_id),
        ).fetchone()
