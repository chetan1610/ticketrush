from fastapi import FastAPI


app=FastAPI(title="TicketRush Catalog Service")


@app.get("/health")
def health():
    return {"status":"ok","services":"catalog"}