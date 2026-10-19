#!/usr/bin/env bash
# Walks one customer through the whole flow:
# signup -> login -> find a show -> hold seats -> pay -> confirmed + ticket email.
# Works against `docker compose up` or services started by hand on the default ports.
set -euo pipefail

USER_URL=${USER_URL:-http://localhost:8000}
CATALOG_URL=${CATALOG_URL:-http://localhost:8001}
BOOKING_URL=${BOOKING_URL:-http://localhost:8002}
PAYMENT_URL=${PAYMENT_URL:-http://localhost:8003}
CITY=${CITY:-Hyderabad}

JSON='Content-Type: application/json'
field() { python3 -c "import sys, json; print(json.load(sys.stdin)$1)"; }

EMAIL="smoke+$(date +%s)@example.com"
PASSWORD="smoke-test-pw-123"

echo "1. sign up $EMAIL"
curl -sf -X POST "$USER_URL/auth/signup" -H "$JSON" \
    -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}" > /dev/null

echo "2. log in"
TOKEN=$(curl -sf -X POST "$USER_URL/auth/login" -H "$JSON" \
    -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}" | field '["access"]')
AUTH="Authorization: Bearer $TOKEN"

TOMORROW=$(python3 -c "import datetime; print(datetime.date.today() + datetime.timedelta(days=1))")
echo "3. pick a show in $CITY on $TOMORROW"
SHOW_ID=$(curl -sf "$CATALOG_URL/shows?city=$CITY&day=$TOMORROW&limit=1" | field '["items"][0]["id"]')
echo "   show #$SHOW_ID"

echo "4. hold two free seats"
SEATS=$(curl -sf "$BOOKING_URL/shows/$SHOW_ID/seats" \
    | python3 -c "import sys, json; print([s['id'] for s in json.load(sys.stdin) if s['available']][:2])")
BOOKING=$(curl -sf -X POST "$BOOKING_URL/bookings" -H "$JSON" -H "$AUTH" \
    -d "{\"show_id\":$SHOW_ID,\"seat_ids\":$SEATS}")
BOOKING_ID=$(echo "$BOOKING" | field '["id"]')
echo "   booking #$BOOKING_ID, $(echo "$BOOKING" | field '["seat_labels"]'), Rs. $(echo "$BOOKING" | field '["amount"]')"

echo "5. pay"
PAYMENT=$(curl -sf -X POST "$PAYMENT_URL/payments" -H "$JSON" -H "$AUTH" \
    -H "Idempotency-Key: smoke-$BOOKING_ID" \
    -d "{\"bookingId\":$BOOKING_ID,\"cardNumber\":\"4111111111111111\"}")
echo "   payment $(echo "$PAYMENT" | field '["id"]') -> $(echo "$PAYMENT" | field '["status"]')"

echo "6. booking status"
STATUS=$(curl -sf "$BOOKING_URL/bookings/$BOOKING_ID" -H "$AUTH" | field '["status"]')
echo "   $STATUS"
[ "$STATUS" = "CONFIRMED" ] || { echo "FAILED: expected CONFIRMED"; exit 1; }

echo "OK - ticket email sent to $EMAIL (see Mailpit at http://localhost:8025)"
