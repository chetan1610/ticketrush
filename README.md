# TicketRush 🎟️

A ticket booking platform (like BookMyShow), built as microservices to demonstrate the core concepts of backend engineering.

## Services

| Service | Tech | Port | Job |
|---|---|---|---|
| user-service | Django + DRF | 8000 | signup, login, JWT |
| catalog-service | FastAPI | 8001 | movies, theatres, shows, seat layout |
| booking-service | FastAPI | 8002 | seat holds and bookings |
| payment-service | Spring Boot | 8003 | payments (simulated card gateway) |
| notification-service | Spring Boot | 8004 | ticket emails |

Each service owns its own Postgres database (`users`, `catalog`, `bookings`, `payments`, `notifications`). No service reads another service's tables; they talk over HTTP.

## How a booking flows

```
client ──login──▶ user-service ──▶ JWT (HS256, shared JWT_SECRET)

client ──GET /shows?city=..──▶ catalog-service

client ──POST /bookings──▶ booking-service ──GET /shows/{id}, /shows/{id}/seats──▶ catalog-service
                              │  seats held as PENDING for 10 minutes
                              ▼
client ──POST /payments──▶ payment-service ──GET  /internal/bookings/{id}─────────▶ booking-service
       (Idempotency-Key)      │             ──POST /internal/bookings/{id}/confirm─▶ booking-service
                              │             ──POST /internal/notifications/...────▶ notification-service ──SMTP──▶ inbox
```

`/internal/**` endpoints require the `X-Internal-Token` header (shared `INTERNAL_TOKEN`), so only other services can call them.

## Concepts demonstrated

- **No double booking**: `booking_seats` has primary key `(show_id, seat_id)`. Two people racing for the same seat both try to `INSERT`; Postgres lets exactly one win and the other gets `409`.
- **Seat holds that expire**: a booking starts as `PENDING` with an `expires_at`. Expired holds are released (lazily, inside the same transaction as the next booking) so seats return to the pool.
- **Idempotent payments**: `POST /payments` requires an `Idempotency-Key` header. Retrying with the same key returns the original payment instead of charging twice (`UNIQUE (user_id, idempotency_key)`). A partial unique index allows at most one live or successful payment per booking.
- **Failure handling between services**: payment-service never holds a DB transaction across HTTP calls. If booking-service or notification-service is down after the card is charged, the payment stays `CHARGED` (or `notified = false`) and a scheduled reconciler retries. The downstream calls are idempotent, so retries are safe. If the hold expired before payment finished, the charge is refunded (`REFUNDED`).
- **Retrying email**: notification-service stores every email; failed sends are retried on a schedule, up to 5 attempts.
- **Keyset pagination** on `GET /shows` (cursor on `(starts_at, id)` backed by an index).
- **Layered structure** in the Python services: routers → services → repositories.
- **Stateless auth**: services verify the JWT themselves, with no call to user-service per request.

## Run everything

```bash
docker compose up --build
./scripts/smoke_test.sh        # signup → login → hold seats → pay → confirmed
```

Ticket emails land in Mailpit: http://localhost:8025. Postgres is on `localhost:5433` (user/password `ticketrush`).

The first start creates the five databases and applies the catalog/booking SQL migrations and dev seed data (`infra/postgres/init.sh`). user-service (Django) and the Spring services (Flyway) migrate themselves on startup. To start from scratch: `docker compose down -v`.

## Run a service by hand

Each service has a `.env.example`. Copy it to `.env` (Python services) or export the variables (Spring services).

```bash
# catalog-service / booking-service
pip install -r requirements.txt
psql ... -f migrations/*.sql            # first time only
uvicorn app.main:app --port 8001        # booking: --port 8002

# user-service
pip install -r requirements.txt
python manage.py migrate
python manage.py runserver 8000

# payment-service / notification-service
mvn spring-boot:run
```

## API cheat sheet

| Method | Path | Auth | Notes |
|---|---|---|---|
| POST | `user:/auth/signup` | – | `{email, password, name?}` |
| POST | `user:/auth/login` | – | `{email, password}` → `{access, refresh}` |
| POST | `user:/auth/refresh` | – | `{refresh}` → `{access}` |
| GET | `user:/auth/me` | JWT | |
| GET | `catalog:/movies`, `/movies/{id}` | – | |
| GET | `catalog:/shows?city=&day=&limit=&cursor=` | – | keyset paginated |
| GET | `catalog:/shows/{id}`, `/shows/{id}/seats` | – | |
| POST | `catalog:/shows` | – | create a show |
| GET | `booking:/shows/{id}/seats` | – | seat map with `available` |
| POST | `booking:/bookings` | JWT | `{show_id, seat_ids}` → `PENDING` hold |
| GET | `booking:/bookings`, `/bookings/{id}` | JWT | your bookings |
| POST | `booking:/bookings/{id}/cancel` | JWT | only `PENDING` |
| POST | `payment:/payments` | JWT + `Idempotency-Key` | `{bookingId, cardNumber}`; cards ending `0002` are declined |
| GET | `payment:/payments`, `/payments/{id}` | JWT | |

Payment responses: `201` succeeded, `200` replay of an earlier request, `202` charged but confirmation still retrying, `402` declined, `409` booking no longer payable.

## Folders

- `services/`: one folder per microservice
- `infra/`: Postgres bootstrap for docker compose
- `scripts/`: end-to-end smoke test
- `learning/`: experiments, like the raw HTTP server
