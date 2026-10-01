from pydantic import BaseModel


class MovieOut(BaseModel):
    id: int
    title: str
    language: str
    duration_minutes: int
    certificate: str