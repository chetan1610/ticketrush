from contextlib import asynccontextmanager

from fastapi import FastAPI, HTTPException

from app.db import pool
from app.routers import bookings


@asynccontextmanager
async def lifespan(app: FastAPI):
    pool.open()
    yield
    pool.close()


app = FastAPI(title="TicketRush Booking Service", lifespan=lifespan)
app.include_router(bookings.router)
app.include_router(bookings.internal)


@app.get("/health")
def health():
    try:
        with pool.connection(timeout=2) as conn:
            conn.execute("SELECT 1")
    except Exception:
        raise HTTPException(status_code=503, detail="database unavailable")
    return {"status": "ok", "service": "booking", "database": "ok"}
