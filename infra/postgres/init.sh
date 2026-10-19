#!/bin/sh
# Runs once, on the first start of an empty Postgres volume.
# One database per service: no service reads another service's tables.
set -e

for db in catalog users bookings payments notifications; do
    psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -c "CREATE DATABASE $db"
done

# The FastAPI services keep plain SQL migrations; Django and Flyway migrate themselves.
for f in /migrations/catalog/*.sql /seeds/catalog/dev_seed.sql; do
    psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d catalog -f "$f"
done
for f in /migrations/booking/*.sql; do
    psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d bookings -f "$f"
done
