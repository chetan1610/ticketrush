from fastapi import FastAPI,HTTPException
from app.db import pool
from contextlib import asynccontextmanager


@asynccontextmanager
async def lifespan(app:FastAPI):
    pool.open()
    yield
    pool.close()


app=FastAPI(title="TicketRush Catalog Service", lifespan=lifespan)


@app.get("/health")
def health():
    try:
        with pool.connection(timeout=2) as conn:
            conn.execute("SELECT 1")
    except Exception:
        raise HTTPException(status_code=503, detail="database unavailable")
    return {"status": "ok", "service": "catalog", "database": "ok"}    
            